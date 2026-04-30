//
//  ContentView.swift
//  PolstSDKSandbox
//
//  Root menu: two buttons — SwiftUI (modern) and UIKit (legacy) — each
//  pushing onto the navigation stack to the matching PolstSDK rendering
//  surface.
//

import PolstSDK
import SwiftUI
import UIKit

// Shared across both screens so the demo hits the same polst/backend.
private let demoClient = PolstClient(environment: .production)
private let demoShortId = "XPvUofYvtvRM"

struct ContentView: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("PolstSDK Sandbox")
                    .font(.title2)
                    .padding(.top, 24)

                Text("Pick a rendering surface.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                VStack(spacing: 12) {
                    NavigationLink {
                        SwiftUIScreen()
                    } label: {
                        SurfaceButtonLabel(
                            title: "SwiftUI",
                            subtitle: "Modern implementation",
                            systemImage: "swift"
                        )
                    }

                    NavigationLink {
                        UIKitScreen()
                    } label: {
                        SurfaceButtonLabel(
                            title: "UIKit",
                            subtitle: "Legacy implementation",
                            systemImage: "rectangle.grid.1x2"
                        )
                    }
                }
                .padding(.horizontal, 24)

                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle("Polst")
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationViewStyle(.stack)
    }
}

private struct SurfaceButtonLabel: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.title3)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body.weight(.semibold))
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.secondarySystemBackground))
        )
        .foregroundColor(.primary)
    }
}

// MARK: - SwiftUI surface

struct SwiftUIScreen: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                PolstView(shortId: demoShortId, client: demoClient)
            }
            .padding()
        }
        .navigationTitle("SwiftUI")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - UIKit surface

struct UIKitScreen: View {
    var body: some View {
        UIKitPolstHost()
            .navigationTitle("UIKit")
            .navigationBarTitleDisplayMode(.inline)
    }
}

/// Embeds `PolstViewController` (the SDK's UIKit bridge) inside a plain
/// `UIViewController` so the SwiftUI navigation stack can push to a purely
/// UIKit-hosted screen.
private struct UIKitPolstHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let host = UIViewController()
        host.view.backgroundColor = .systemBackground

        let child = PolstViewController(shortId: demoShortId, client: demoClient)
        host.addChild(child)
        child.view.translatesAutoresizingMaskIntoConstraints = false
        host.view.addSubview(child.view)
        NSLayoutConstraint.activate([
            child.view.topAnchor.constraint(equalTo: host.view.safeAreaLayoutGuide.topAnchor, constant: 16),
            child.view.leadingAnchor.constraint(equalTo: host.view.leadingAnchor, constant: 16),
            child.view.trailingAnchor.constraint(equalTo: host.view.trailingAnchor, constant: -16),
            child.view.heightAnchor.constraint(equalToConstant: 360),
        ])
        child.didMove(toParent: host)
        return host
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

#Preview {
    ContentView()
}
