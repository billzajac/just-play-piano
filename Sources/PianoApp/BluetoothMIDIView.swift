#if os(iOS)
import CoreAudioKit
import SwiftUI

struct BluetoothMIDIView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CABTMIDICentralViewController {
        CABTMIDICentralViewController()
    }

    func updateUIViewController(_ uiViewController: CABTMIDICentralViewController, context: Context) {}
}
#endif
