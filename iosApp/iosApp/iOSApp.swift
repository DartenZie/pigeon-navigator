import SwiftUI
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var appSettings = AppSettingsViewModelWrapper()

    init() {
        KoinIOSKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(appSettings: appSettings)
        }
        .onChange(of: scenePhase) { newPhase in
            // Tear down the shared SettingsHandle subscription when the scene is
            // no longer active. The handle is recreated lazily on next launch via
            // the `@StateObject` above (which itself recreates the wrapper if the
            // App is fully reinstantiated).
            if newPhase == .background {
                appSettings.dispose()
            }
        }
    }
}
