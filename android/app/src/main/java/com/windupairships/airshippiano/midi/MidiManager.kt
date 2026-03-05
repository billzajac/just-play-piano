package com.windupairships.airshippiano.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager as AndroidMidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import com.windupairships.airshippiano.audio.AudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MidiManager(context: Context) {

    private val _deviceGroups = MutableStateFlow<List<MidiDeviceGroup>>(emptyList())
    val deviceGroups: StateFlow<List<MidiDeviceGroup>> = _deviceGroups

    private val midiManager: AndroidMidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? AndroidMidiManager

    private val handler = Handler(Looper.getMainLooper())
    private val openDevices = mutableMapOf<Int, OpenDeviceHandle>()
    private val transposeByDevice = mutableMapOf<String, Int>()
    private var audioEngine: AudioEngine? = null
    private var started = false

    private data class OpenDeviceHandle(
        val device: MidiDevice,
        val ports: List<MidiOutputPort>,
        val deviceName: String
    )

    val allActiveNotes: Set<Int>
        get() {
            val notes = mutableSetOf<Int>()
            for (group in _deviceGroups.value) {
                for (note in group.activeNotes) {
                    val transposed = note + group.transpose
                    if (transposed in 0..127) notes.add(transposed)
                }
            }
            return notes
        }

    fun start(audioEngine: AudioEngine) {
        if (started) return
        started = true
        this.audioEngine = audioEngine

        midiManager?.registerDeviceCallback(deviceCallback, handler)
        syncDevices()
    }

    fun stop() {
        started = false
        midiManager?.unregisterDeviceCallback(deviceCallback)
        for ((_, handle) in openDevices) {
            handle.ports.forEach { it.close() }
            handle.device.close()
        }
        openDevices.clear()
    }

    fun setTranspose(deviceName: String, offset: Int) {
        transposeByDevice[deviceName] = offset
        _deviceGroups.update { groups ->
            groups.map { group ->
                if (group.deviceName == deviceName) group.copy(transpose = offset) else group
            }
        }
    }

    private val deviceCallback = object : AndroidMidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            syncDevices()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            val id = device.id
            openDevices.remove(id)?.let { handle ->
                handle.ports.forEach { it.close() }
                handle.device.close()
                // Clear active notes for this device
                _deviceGroups.update { groups ->
                    groups.map { group ->
                        if (group.deviceName == handle.deviceName) {
                            group.copy(activeNotes = emptySet())
                        } else group
                    }
                }
            }
            syncDevices()
        }
    }

    private fun syncDevices() {
        val devices = midiManager?.devices ?: return

        val allEndpoints = mutableListOf<MidiEndpointInfo>()

        for (deviceInfo in devices) {
            // Only care about devices with output ports (that send MIDI to us)
            if (deviceInfo.outputPortCount == 0) continue

            val id = deviceInfo.id
            val props = deviceInfo.properties
            val name = props.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown Device"
            val manufacturer = props.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: ""

            for (portInfo in deviceInfo.ports) {
                if (portInfo.type == android.media.midi.MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                    allEndpoints.add(
                        MidiEndpointInfo(
                            id = id * 100 + portInfo.portNumber,
                            endpointName = portInfo.name?.ifEmpty { name } ?: name,
                            deviceName = name,
                            manufacturer = manufacturer
                        )
                    )
                }
            }

            // Open device if not already open
            if (!openDevices.containsKey(id)) {
                midiManager?.openDevice(deviceInfo, { device ->
                    if (device != null) {
                        val ports = mutableListOf<MidiOutputPort>()
                        for (portInfo in deviceInfo.ports) {
                            if (portInfo.type == android.media.midi.MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                                val port = device.openOutputPort(portInfo.portNumber)
                                if (port != null) {
                                    port.connect(MidiNoteReceiver(name))
                                    ports.add(port)
                                }
                            }
                        }
                        openDevices[id] = OpenDeviceHandle(device, ports, name)
                        // Pre-create synth channel for this device
                        audioEngine?.ensureChannel(name)
                    }
                }, handler)
            }
        }

        // Build device groups
        val grouped = allEndpoints.groupBy { it.deviceName }
        val newGroups = grouped.entries.sortedBy { it.key }.map { (deviceName, endpoints) ->
            val existing = _deviceGroups.value.find { it.deviceName == deviceName }
            MidiDeviceGroup(
                id = deviceName.ifEmpty { "unknown-${endpoints.first().id}" },
                deviceName = deviceName,
                manufacturer = endpoints.first().manufacturer,
                endpoints = endpoints,
                activeNotes = existing?.activeNotes ?: emptySet(),
                transpose = transposeByDevice[deviceName] ?: existing?.transpose ?: 0
            )
        }
        _deviceGroups.value = newGroups
    }

    private inner class MidiNoteReceiver(private val deviceName: String) : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                val messageType = status and 0xF0

                when (messageType) {
                    0x90, 0x80 -> {
                        if (i + 2 < offset + count) {
                            val note = data[i + 1].toInt() and 0x7F
                            val velocity = data[i + 2].toInt() and 0x7F
                            val transpose = transposeByDevice[deviceName] ?: 0

                            if (messageType == 0x90 && velocity > 0) {
                                // Note On
                                val transposed = note + transpose
                                if (transposed in 0..127) {
                                    audioEngine?.noteOn(transposed, velocity, deviceName)
                                }
                                updateActiveNotes(deviceName) { it + note }
                            } else {
                                // Note Off (0x80 or 0x90 with velocity 0)
                                val transposed = note + transpose
                                if (transposed in 0..127) {
                                    audioEngine?.noteOff(transposed, deviceName)
                                }
                                updateActiveNotes(deviceName) { it - note }
                            }
                            i += 3
                        } else break
                    }
                    0xB0 -> {
                        // Control Change
                        if (i + 2 < offset + count) {
                            val controller = data[i + 1].toInt() and 0x7F
                            val value = data[i + 2].toInt() and 0x7F
                            when (controller) {
                                64 -> audioEngine?.sustainPedal(value, deviceName)
                                66, 67 -> audioEngine?.controlChange(controller, value, deviceName)
                            }
                            i += 3
                        } else break
                    }
                    0xC0, 0xD0 -> {
                        // Program Change, Channel Pressure (2 bytes)
                        i += 2
                    }
                    0xE0 -> {
                        // Pitch Bend (3 bytes)
                        i += 3
                    }
                    0xF0 -> {
                        // System messages - skip sysex
                        if (status == 0xF0) {
                            while (i < offset + count && (data[i].toInt() and 0xFF) != 0xF7) i++
                            i++ // skip F7
                        } else {
                            i++ // other system messages
                        }
                    }
                    else -> i++
                }
            }
        }
    }

    private fun updateActiveNotes(deviceName: String, transform: (Set<Int>) -> Set<Int>) {
        _deviceGroups.update { groups ->
            groups.map { group ->
                if (group.deviceName == deviceName) {
                    group.copy(activeNotes = transform(group.activeNotes))
                } else group
            }
        }
    }
}
