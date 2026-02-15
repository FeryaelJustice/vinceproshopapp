import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        StripePaymentBridgeService.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
