import SwiftUI

#if !SWIFT_PACKAGE
private extension Bundle {
    static let module = Bundle.main
}
#endif

#if os(macOS)
class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }
}
#endif

@main
struct PianoApp: App {
    #if os(macOS)
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    #endif
    @StateObject private var soundFontManager = SoundFontManager()
    @StateObject private var midiManager = MIDIManager()
    @State private var audioEngine = AudioEngine()

    var body: some Scene {
        WindowGroup {
            MainView(soundFontManager: soundFontManager, midiManager: midiManager, audioEngine: audioEngine)
        }
        #if os(macOS)
        .defaultSize(width: 360, height: 260)
        .commands {
            CommandGroup(replacing: .appInfo) {
                Button("About Airship Piano") {
                    NSApplication.shared.orderFrontStandardAboutPanel(options: [
                        .applicationName: "Airship Piano",
                        .applicationVersion: "1.0.0",
                        .credits: NSAttributedString(
                            string: "A lightweight, open-source MIDI program that lets you just play your MIDI keyboard.\n\nhttps://github.com/billzajac/airship-piano",
                            attributes: [
                                .font: NSFont.systemFont(ofSize: 11),
                                .foregroundColor: NSColor.textColor
                            ]
                        )
                    ])
                }
            }
        }
        #endif
    }
}

struct MainView: View {
    @ObservedObject var soundFontManager: SoundFontManager
    @ObservedObject var midiManager: MIDIManager
    let audioEngine: AudioEngine

    var body: some View {
        Group {
            switch soundFontManager.state {
            case .checking:
                ProgressView("Checking sound font...")

            case .downloading(let progress):
                VStack(spacing: 12) {
                    Text("Downloading Yamaha C5 Grand Piano")
                        .font(.headline)
                    Text("Salamander sound font (~24 MB, one-time)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    ProgressView(value: progress)
                        .frame(maxWidth: 240)
                    Text("\(Int(progress * 100))%")
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
                .padding(40)

            case .ready(let url):
                ContentView(midiManager: midiManager, audioEngine: audioEngine)
                    .onAppear { audioEngine.loadSoundFont(url: url) }

            case .error(let message):
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundStyle(.orange)
                        .accessibilityHidden(true)
                    Text("Failed to load sound font")
                        .font(.headline)
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 280)
                    HStack(spacing: 12) {
                        Button("Retry") { soundFontManager.retry() }
                            .buttonStyle(.borderedProminent)
                        #if os(macOS)
                        Button("Use Default (Lower Quality)") {
                            audioEngine.loadGMFallback()
                            soundFontManager.useDefaultSound()
                        }
                        #endif
                    }
                }
                .padding(40)
            }
        }
    }
}

struct ContentView: View {
    @ObservedObject var midiManager: MIDIManager
    let audioEngine: AudioEngine
    @State private var volume: Float = 0.8
    #if os(iOS)
    @State private var showingBluetoothPicker = false
    #endif

    var body: some View {
        VStack(spacing: 16) {
            Image("AppLogo", bundle: Bundle.module)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 80, height: 80)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            Text("A simple MIDI piano app")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            if midiManager.connectedDevices.isEmpty {
                Label("No MIDI device connected", systemImage: "pianokeys")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(midiManager.connectedDevices, id: \.self) { device in
                    Label(device, systemImage: "pianokeys")
                        .foregroundStyle(.green)
                }
            }

            #if os(iOS)
            Button("Connect Bluetooth MIDI") {
                showingBluetoothPicker = true
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $showingBluetoothPicker) {
                BluetoothMIDIView()
            }
            #endif

            Divider()

            HStack {
                Image(systemName: "speaker.fill")
                Slider(value: $volume, in: 0...1) { _ in
                    audioEngine.volume = volume
                }
                .accessibilityLabel("Volume")
                Image(systemName: "speaker.wave.3.fill")
            }
            .padding(.horizontal)
        }
        .padding(24)
        .onAppear {
            audioEngine.volume = volume
            midiManager.start(audioEngine: audioEngine)
        }
    }
}
