# Airship Piano — Mobile Plan

## Strategy: Two Native Apps

After evaluating KMP, Flutter, and native approaches, the unanimous recommendation is
**two separate native apps** (Swift for iOS, Kotlin for Android). Rationale:

- **Latency**: Musical instruments need <10ms response. Cross-platform frameworks add
  abstraction overhead on the latency-critical MIDI-to-audio path. Native gives direct
  API access with zero abstraction tax.
- **Proportionality**: The app is ~400 lines across 4 files. The UI is a volume slider,
  a device list, and a progress bar. Cross-platform frameworks solve shared complex UI —
  which this app doesn't have.
- **Risk**: KMP's MIDI library (ktmidi) has untested iOS support. Flutter's MIDI/audio
  plugins are single-maintainer with known bugs. Native uses Apple/Google-maintained APIs.
- **iOS code reuse**: ~90% of the macOS Swift code works on iOS unchanged (CoreMIDI,
  AVAudioEngine, SwiftUI).

---

## Phase 1: iOS (1-2 days)

### What works unchanged
- `AudioEngine.swift` — AVAudioEngine + AVAudioUnitSampler are available on iOS
- `MIDIManager.swift` — CoreMIDI is fully available on iOS
- `SoundFontManager.swift` — URLSession, FileManager, CommonCrypto all work on iOS
- `ContentView` in `PianoApp.swift` — pure SwiftUI, works on iOS

### What needs to change
- **App shell** (~25 lines): Remove `NSApplicationDelegateAdaptor`, `.defaultSize()`,
  `.commands{}`, and AppKit about panel
- **GM fallback paths**: macOS system DLS files don't exist on iOS — remove or skip
- **Add Bluetooth MIDI pairing**: Wrap `CABTMIDICentralViewController` (CoreAudioKit)
  in a SwiftUI `UIViewControllerRepresentable` — ~15 lines
- **Build system**: Create Xcode project with iOS target (SPM alone can't build iOS apps
  with signing/provisioning)

### iOS-specific UX
- Portrait orientation (no piano keyboard UI, landscape adds nothing)
- Single screen: connection status, scan button, volume slider
- System BLE MIDI picker sheet for Bluetooth pairing
- Auto-reconnect to last-used device

---

## Phase 2: Android (1-2 weeks)

### Architecture
- **MIDI input**: `android.media.midi` — mature, supports USB + Bluetooth MIDI (API 23+)
- **SF2 playback**: FluidSynth via NDK with Oboe audio driver for low-latency output
- **UI**: Jetpack Compose — trivial for this app (~80-100 lines)
- **Download manager**: Rewrite SoundFontManager in Kotlin (~100-150 lines)

### Key challenges
- FluidSynth NDK integration (glib cross-compilation, JNI bridge). Mitigated by:
  - [VolcanoMobile/fluidsynth-android](https://github.com/VolcanoMobile/fluidsynth-android) AAR
  - [android-midi-synth](https://github.com/robsonsmartins/android-midi-synth) example project
- Android audio latency varies by device (10ms on Pixel, 40-100ms+ on cheap devices)
- BLE MIDI requires custom scanning UI + runtime permissions (BLUETOOTH_SCAN/CONNECT)

### Estimated code
- MIDI input layer: ~150-200 lines Kotlin
- FluidSynth JNI bridge: ~200-300 lines (C++ + Kotlin)
- UI (Jetpack Compose): ~80-100 lines
- SF2 download manager: ~100-150 lines
- **Total**: ~600-800 lines

---

## References
- [SoundFonts iOS app](https://github.com/bradhowes/SoundFonts) — AVAudioUnitSampler + SF2 on iOS
- [android-midi-synth](https://github.com/robsonsmartins/android-midi-synth) — Kotlin + FluidSynth example
- [VolcanoMobile/fluidsynth-android](https://github.com/VolcanoMobile/fluidsynth-android) — FluidSynth AAR
- [FluidSynth](https://www.fluidsynth.org/) — SF2 synthesis engine
- [Oboe](https://developer.android.com/games/sdk/oboe) — Low-latency Android audio
