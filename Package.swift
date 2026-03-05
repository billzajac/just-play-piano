// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "PianoApp",
    platforms: [.macOS(.v13), .iOS(.v16)],
    targets: [
        .executableTarget(
            name: "PianoApp",
            path: "Sources/PianoApp",
            exclude: ["iOS.entitlements"],
            resources: [.process("Assets.xcassets")]
        )
    ]
)
