import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitKt.doInitKoin(appModule: nil)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
