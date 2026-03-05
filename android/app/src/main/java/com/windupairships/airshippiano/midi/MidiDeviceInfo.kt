package com.windupairships.airshippiano.midi

data class MidiEndpointInfo(
    val id: Int,
    val endpointName: String,
    val deviceName: String,
    val manufacturer: String
) {
    val displayName: String
        get() {
            var name = endpointName
            // Strip device name prefix (e.g. "Nektar SE61 MIDI 1" -> "MIDI 1")
            if (deviceName.isNotEmpty() && name.startsWith(deviceName)) {
                val suffix = name.removePrefix(deviceName).trim()
                if (suffix.isNotEmpty()) {
                    name = suffix
                }
            }
            // Add space between "MIDI" and number if missing (e.g. "MIDI1" -> "MIDI 1")
            name = name.replace(Regex("MIDI(\\d)"), "MIDI $1")
            return name
        }
}

data class MidiDeviceGroup(
    val id: String,
    val deviceName: String,
    val manufacturer: String,
    val endpoints: List<MidiEndpointInfo>,
    val activeNotes: Set<Int> = emptySet(),
    val transpose: Int = 0
)
