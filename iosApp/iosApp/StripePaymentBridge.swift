import Foundation
import UIKit
import ComposeApp

#if canImport(StripePaymentSheet)
import StripePaymentSheet
#endif
#if canImport(StripePayments)
import StripePayments
#endif
#if canImport(StripeCore)
import StripeCore
#endif

enum StripePaymentBridgeNotifications {
    static let request = Notification.Name("StripePaymentBridgeRequest")
    static let response = Notification.Name("StripePaymentBridgeResponse")
}

final class StripePaymentBridgeService: NSObject {
    static let shared = StripePaymentBridgeService()

    private var started = false

    #if canImport(StripePaymentSheet)
    private var activePaymentSheet: PaymentSheet?
    #endif

    private override init() {
        super.init()
    }

    func start() {
        guard !started else { return }
        started = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleRequest(_:)),
            name: StripePaymentBridgeNotifications.request,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc
    private func handleRequest(_ notification: Notification) {
        guard let userInfo = notification.userInfo else { return }
        guard let requestId = parseRequestId(userInfo["requestId"]) else {
            postFailure(requestId: 0, message: "Missing request id")
            return
        }
        let action = userInfo["action"] as? String ?? ""

        #if canImport(StripePaymentSheet)
        switch action {
        case "presentPaymentSheet":
            handlePresentPaymentSheet(requestId: requestId, userInfo: userInfo)
        default:
            postFailure(requestId: requestId, message: "Unknown Stripe payment action: \(action)")
        }
        #else
        postFailure(
            requestId: requestId,
            message: "StripePaymentSheet SDK is not linked. Add Stripe iOS package product StripePaymentSheet."
        )
        #endif
    }

    #if canImport(StripePaymentSheet)
    private func handlePresentPaymentSheet(requestId: Int64, userInfo: [AnyHashable: Any]) {
        guard let clientSecret = userInfo["clientSecret"] as? String, !clientSecret.isEmpty else {
            postFailure(requestId: requestId, message: "Missing client secret")
            return
        }

        let publishableKey = LocalSecretsKt.localStripePublishableKey().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !publishableKey.isEmpty else {
            postFailure(requestId: requestId, message: "Missing VINCE_STRIPE_PUBLISHABLE_KEY")
            return
        }

        #if canImport(StripePayments)
        STPAPIClient.shared.publishableKey = publishableKey
        #endif

        #if canImport(StripeCore)
        StripeAPI.defaultPublishableKey = publishableKey
        #endif

        var configuration = PaymentSheet.Configuration()
        let merchantName = LocalSecretsKt.localStripeMerchantDisplayName().trimmingCharacters(in: .whitespacesAndNewlines)
        configuration.merchantDisplayName = merchantName.isEmpty ? "Vince Pro Shop" : merchantName
        configuration.allowsDelayedPaymentMethods = true

        var billing = PaymentSheet.BillingDetails()
        if let name = (userInfo["customerName"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            billing.name = name
        }
        if let email = (userInfo["customerEmail"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !email.isEmpty {
            billing.email = email
        }
        if let phone = (userInfo["customerPhone"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !phone.isEmpty {
            billing.phone = phone
        }
        configuration.defaultBillingDetails = billing

        guard let presenter = topViewController() else {
            postFailure(requestId: requestId, message: "Could not find presenting view controller")
            return
        }

        let paymentSheet = PaymentSheet(paymentIntentClientSecret: clientSecret, configuration: configuration)
        activePaymentSheet = paymentSheet

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            paymentSheet.present(from: presenter) { result in
                self.activePaymentSheet = nil
                switch result {
                case .completed:
                    self.postSuccess(
                        requestId: requestId,
                        payload: [
                            "status": "completed",
                            "paymentIntentId": self.extractPaymentIntentId(from: clientSecret)
                        ]
                    )
                case .canceled:
                    self.postSuccess(
                        requestId: requestId,
                        payload: ["status": "canceled"]
                    )
                case .failed(let error):
                    self.postSuccess(
                        requestId: requestId,
                        payload: [
                            "status": "failed",
                            "errorMessage": error.localizedDescription
                        ]
                    )
                }
            }
        }
    }

    private func topViewController(
        from controller: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
    ) -> UIViewController? {
        if let navigationController = controller as? UINavigationController {
            return topViewController(from: navigationController.visibleViewController)
        }
        if let tabBarController = controller as? UITabBarController {
            return topViewController(from: tabBarController.selectedViewController)
        }
        if let presented = controller?.presentedViewController {
            return topViewController(from: presented)
        }
        return controller
    }

    private func extractPaymentIntentId(from clientSecret: String) -> String {
        if let index = clientSecret.range(of: "_secret_") {
            return String(clientSecret[..<index.lowerBound])
        }
        return ""
    }
    #endif

    private func postSuccess(requestId: Int64, payload: [String: Any]) {
        var userInfo: [String: Any] = [
            "requestId": requestId,
            "success": true
        ]
        payload.forEach { userInfo[$0.key] = $0.value }
        NotificationCenter.default.post(
            name: StripePaymentBridgeNotifications.response,
            object: nil,
            userInfo: userInfo
        )
    }

    private func postFailure(requestId: Int64, message: String) {
        NotificationCenter.default.post(
            name: StripePaymentBridgeNotifications.response,
            object: nil,
            userInfo: [
                "requestId": requestId,
                "success": false,
                "errorMessage": message
            ]
        )
    }

    private func parseRequestId(_ raw: Any?) -> Int64? {
        switch raw {
        case let value as NSNumber:
            return value.int64Value
        case let value as Int64:
            return value
        case let value as Int:
            return Int64(value)
        case let value as String:
            return Int64(value)
        default:
            return nil
        }
    }
}
