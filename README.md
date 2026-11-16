# EDPay Java SDK

Java client for sending server-to-server requests to the EDPay API. Resource methods return JSON strings from the API.

## Requirements

- Java 17 or later
- Maven 3.8 or later

## Install

Install the SDK in your local Maven repository:

```bash
git clone https://github.com/edutecka/edpay-java-sdk.git
cd edpay-java-sdk
mvn install
```

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
  <groupId>mz.co.edpay</groupId>
  <artifactId>edpay-java-sdk</artifactId>
  <version>1.5.1</version>
</dependency>
```

For Gradle, add `mavenLocal()` to the repositories block and use:

```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.5.1'
```

## Create a payment

Every partner API request requires both the public key (`pk_test_` or `pk_live_`) and private key (`sk_test_` or `sk_live_`) from the same environment. The SDK rejects missing, blank, or mismatched key pairs when building the client. Public catalog requests and buyer-facing checkout requests authenticated by a checkout token do not use the partner key pair; keep the private key on your server.

Set `EDPAY_PUBLIC_KEY` and `EDPAY_PRIVATE_KEY` in the process environment. Keep the private key on your server. Use test keys for sandbox requests and set the API host explicitly:

```java
import mz.co.edpay.EDPayClient;
import mz.co.edpay.contacts.PhoneNumberUtils;
import java.util.Objects;

EDPayClient client = new EDPayClient.Builder()
    .publicKey(System.getenv("EDPAY_PUBLIC_KEY"))
    .privateKey(System.getenv("EDPAY_PRIVATE_KEY"))
    .environment(EDPayClient.Environment.SANDBOX)
    .baseUrl("https://api-pay.edutecka.com")
    .build();

String method = Objects.requireNonNull(System.getenv("EDPAY_METHOD"), "Set EDPAY_METHOD from the public catalog");
String phoneMember = "";
if (PhoneNumberUtils.requiresPhoneForPaymentMethod(method)) {
    String phoneInput = Objects.requireNonNull(System.getenv("CUSTOMER_PHONE"), "Set CUSTOMER_PHONE for this method");
    if (!PhoneNumberUtils.isValidForPaymentMethod(method, phoneInput)) {
        throw new IllegalArgumentException("Phone is invalid for the selected method");
    }
    String phone = PhoneNumberUtils.normalizeForPaymentMethod(method, phoneInput);
    phoneMember = ",\"phone\":\"" + phone + "\"";
}
String payload = """
    {"amount_minor":50000,"currency":"MZN","method":"%s"%s,
     "reference":"ORDER-10492","description":"Example order"}
    """.formatted(method, phoneMember);

String responseJson = client.payments().create(payload, "ORDER-10492-CREATE-1");
System.out.println(responseJson);
```

`payments().create(payload, idempotencyKey)` sends `POST /v1/payments` and returns the API response as JSON text. Select a live `method` ID from `GET /v1/public/payment-methods`. `amount_minor` is a positive integer in minor currency units (`50000` means MZN 500.00); `currency` must be `MZN`, `USD`, or `EUR` and match the account base currency. The method-aware helper validates and normalizes eMola (`86`/`87` → 9 national digits) and M-Pesa (`84`/`85` → `258` + 9 national digits). For card and digital-wallet methods, leave phone validation off and pass the instrument in `channel` (`payment_token`, `wallet_token`, `holder_name`, `customer_email`, `customer_phone`, `return_url`, `type`, or `provider` as applicable). Optional payment fields also include `customer_id`, `reference`, and `description`. Partner API routes send key headers; the public catalog and token-based checkout omit them. Writes include an `Idempotency-Key` where the endpoint requires one.

Retrieve a payment and its lifecycle with `client.payments().retrieve(paymentId)`, `.events(paymentId)`, and `.attempts(paymentId)`.

For hosted checkout, first create a payment link and persist a `payment_link_id` → local order mapping. Then call `client.checkout().createSession` with JSON containing `payment_link_id` and an `Idempotency-Key`. The API returns a short-lived token; pass it with the public key to the browser checkout. On webhook receipt, use `data.payment_link_id` to recover the order mapping; this field is nullable for payments not created from a payment link. Confirm the final payment using a server-side lookup or verified webhook.

## Verify webhook signatures

Pass the exact raw request bytes, the `Payment-Signature` header, and the full webhook secret returned at endpoint creation:

```java
import mz.co.edpay.webhooks.WebhookVerifier;

byte[] rawRequestBody = readRequestBodyBytes();
String signatureHeader = requestHeader("Payment-Signature");
boolean valid = WebhookVerifier.verify(rawRequestBody, signatureHeader, webhookSecret);
if (!valid) {
    throw new SecurityException("Invalid EDPay webhook signature");
}
```

The verifier checks `Payment-Signature: t=<unix-seconds>,v1=<hex>` using HMAC-SHA256 over the timestamp, a period, and the unmodified body bytes. It compares in constant time and rejects timestamps outside the default five-minute tolerance. Configure a different accepted window with the `Duration` overload when needed. The `String` overload UTF-8 encodes its input; use `byte[]` to preserve the exact HTTP body.

The delivered event uses `type` for the event name and may have `data.payment_link_id` set to a link ID or `null`:

```json
{"id":"evt_0123456789abcdef","object":"event","type":"payment.succeeded","created_at":"2026-11-07T12:05:00Z","data":{"object":"payment","id":"pay_0123456789abcdef","reference":"ORDER-10492","payment_link_id":"plink_0123456789abcdef","trans_id":null,"method":"mpesa","status":"succeeded","amount_minor":50000,"currency":"MZN","platform_fee_minor":3000,"receiver_net_minor":47000,"created_at":"2026-11-07T12:04:00Z"}}
```

The Java model is `mz.co.edpay.webhooks.WebhookEvent`; `WebhookEvent.PaymentData.payment_link_id` is nullable. Persist the link-ID-to-order mapping before redirecting the customer and use this field to recover the order. Handle `null` for other payment paths.

## Continue the same payment with another method

The payment ID and reference identify one payment. Present active gateways as choices; each selected method is a child attempt, with only one attempt in progress at a time. Do not create another payment when the buyer changes method. Create the next attempt only after the current one is definitively failed. If it is still processing or its result is unknown, retrieve it later and wait for reconciliation to avoid charging twice. Use a new idempotency key for each child attempt.

```java
// paymentId is the ID returned by payments().create; save it with your order.
// First check GET /v1/payments/{id}; call only when its status is "failed".
String attemptJson = "{\\"method\\":\\"<METHOD_CODE>\\",\\"phone\\":\\"+25884xxxxxxx\\"}";
String nextAttempt = client.payments().createAttempt(
    paymentId, attemptJson, "ORDER-10492-ATTEMPT-2"
);
```

For a payment started through a hosted session or payment link, use `checkout().createSessionPaymentAttempt()` or `checkout().createPaymentLinkPaymentAttempt()` with that same payment ID and its session token/link ID. The current public checkout form accepts phone-based methods; use the partner payment resource for tokenized instruments.

## Function reference

Each row tells you what the call does and when to use it. Partner calls use the API key pair; public catalog and buyer checkout calls do not. Supply a stable `Idempotency-Key` for writes where required.

| Resource | Function | Endpoint | What it does / when to use it |
|---|---|---|---|
| Payments | `payments().create` | `POST /v1/payments` | Creates the parent payment and its first method attempt. Start a server-to-server payment; keep its ID and reference for later method attempts and status checks. |
| Payments | `payments().createCheckout` | `POST /v1/payment-checkout` | Legacy alias for creating a payment; it does not create a hosted checkout session. Use only when migrating an integration that already calls this route. New code should use create. |
| Payments | `payments().list` | `GET /v1/payments` | Lists payments visible to the current API credentials and environment. Reconcile or show a payment history. Use the limit argument where available. |
| Payments | `payments().retrieve` | `GET /v1/payments/{id}` | Returns one payment and its current status. Check the result on your server after a redirect or webhook. |
| Payments | `payments().events` | `GET /v1/payments/{id}/events` | Returns the recorded status changes for a payment. Inspect the payment lifecycle or troubleshoot a status change. |
| Payments | `payments().attempts` | `GET /v1/payments/{id}/attempts` | Lists the payment's provider attempts; this method is read-only. See which method was tried and the outcome of each attempt. |
| Payments | `payments().createAttempt` | `POST /v1/payments/{id}/attempts` | Adds another dynamic method as a child attempt. The payment ID, amount and reference stay the same; this attempt uses its own Idempotency-Key. Use after the latest attempt definitively failed. If it is processing or unknown, retrieve the payment and wait for reconciliation to avoid a duplicate charge. A payment supports up to 20 attempts. |
| Hosted checkout | `checkout().createSession` | `POST /v1/checkout/sessions` | Creates a short-lived checkout session from a payment link. Returns a token for the browser. Send a buyer to EDPay-hosted checkout or embed it on the exact HTTPS origin. |
| Hosted checkout | `checkout().retrieveSession` | `GET /v1/checkout/sessions/{token}` | Reads public session and checkout details. Pass the token returned by createSession. Load the hosted checkout page using its browser token. |
| Hosted checkout | `checkout().getPaymentLink` | `GET /v1/checkout/payment-links/{id}` | Reads the public details for an active payment link. Render the buyer-facing link checkout before a payment is submitted. |
| Hosted checkout | `checkout().framePolicy` | `GET /v1/checkout/sessions/{token}/frame-policy` | Reads whether the session can be embedded and its frame policy. Check embed eligibility before placing checkout in an iframe. |
| Hosted checkout | `checkout().createSessionPayment` | `POST /v1/checkout/sessions/{token}/payments` | Submits a selected phone-based method and buyer data to a public session; tokenized methods are not accepted by the current hosted form. Requires an idempotency key. Start payment from a hosted checkout form. The response means accepted for processing, not paid. |
| Hosted checkout | `checkout().createSessionPaymentAttempt` | `POST /v1/checkout/sessions/{token}/payments/{payment_id}/attempts` | Adds another phone-based method to the payment already created from this session. It keeps the same payment ID and reference; hosted checkout currently does not accept tokenized instruments. Use the same session token and payment ID after the previous attempt definitively failed. Wait when the result is processing or unknown. |
| Hosted checkout | `checkout().retrieveSessionPayment` | `GET /v1/checkout/sessions/{token}/payments/{payment_id}` | Reads the status of a payment created from that session. Show a buyer the latest processing result; verify settlement server-side too. |
| Hosted checkout | `checkout().createPaymentLinkPayment` | `POST /v1/checkout/payment-links/{id}/payments` | Submits a selected phone-based method and buyer data from a public link; tokenized methods are not accepted by the current hosted form. Requires an idempotency key. Start a buyer payment directly from a payment-link page. |
| Hosted checkout | `checkout().createPaymentLinkPaymentAttempt` | `POST /v1/checkout/payment-links/{id}/payments/{payment_id}/attempts` | Adds another phone-based method to the payment already created from this link. It keeps the same payment ID and reference; hosted checkout currently does not accept tokenized instruments. Reuse the same link ID and payment ID after the previous attempt definitively failed. Wait when the result is processing or unknown. |
| Hosted checkout | `checkout().retrievePaymentLinkPayment` | `GET /v1/checkout/payment-links/{id}/payments/{payment_id}` | Reads the status of a payment submitted through a public link. Refresh the buyer-facing result for a link payment. |
| Payment links | `paymentLinks().create` | `POST /v1/payment-links` | Creates a fixed-amount public link. Creating the link does not itself create a payment. Collect payment for a fixed amount through a shareable buyer URL. |
| Payment links | `paymentLinks().list` | `GET /v1/payment-links` | Lists links owned by the authenticated account and environment. Show or reconcile the account's payment links. |
| Payment links | `paymentLinks().retrieve` | `GET /v1/payment-links/{id}` | Reads one link and its status. Check expiry, amount, or whether the link is still active. |
| Payment links | `paymentLinks().deactivate` | `POST /v1/payment-links/{id}/deactivate` | Stops an active link from starting future checkout payments. Close a link that should no longer accept buyer payments. |
| Payment links | `paymentLinks().createCheckoutSession` | `POST /v1/checkout/sessions` | Convenience wrapper that creates a hosted session for the supplied link. Use it instead of calling checkout.createSession when your SDK provides this wrapper. |
| Customers | `customers().create` | `POST /v1/customers` | Creates a customer record for the authenticated account. Save customer details before associating them with payments or invoices. |
| Customers | `customers().list` | `GET /v1/customers` | Lists customers in the current account and environment. Build a customer picker or reconcile customer records. |
| Customers | `customers().retrieve` | `GET /v1/customers/{id}` | Reads one customer record. Load a customer's saved details by ID. |
| Customers | `customers().update` | `PATCH /v1/customers/{id}` | Updates the supplied customer fields. Correct or refresh a customer's details; use a stable idempotency key where required. |
| Invoices | `invoices().create` | `POST /v1/invoices` | Creates a draft invoice with its customer and line items. Prepare an invoice before issuing it to the customer. |
| Invoices | `invoices().list` | `GET /v1/invoices` | Lists invoices, optionally limited to a page size. Show or reconcile invoices for the account. |
| Invoices | `invoices().retrieve` | `GET /v1/invoices/{id}` | Reads one invoice and its current state. Load invoice details or check its lifecycle. |
| Invoices | `invoices().finalize` | `POST /v1/invoices/{id}/finalize` | Issues the draft invoice and makes it available for payment. The invoice is complete and ready to send to the customer. |
| Invoices | `invoices().voidInvoice` | `POST /v1/invoices/{id}/void` | Voids an invoice that should no longer be payable. Cancel an issued invoice according to its allowed state transitions. |
| Invoice series | `subscriptions().create` | `POST /v1/invoice-series` | Creates a schedule that issues invoices on the configured interval. Bill a customer repeatedly using a defined start date, interval, and due period. |
| Invoice series | `subscriptions().list` | `GET /v1/invoice-series` | Lists invoice series; there is no series-detail endpoint in this API version. Review recurring schedules for the account. |
| Invoice series | `subscriptions().pause` | `POST /v1/invoice-series/{id}/pause` | Pauses future invoice generation for a series. Temporarily stop recurring billing without deleting the schedule. |
| Invoice series | `subscriptions().resume` | `POST /v1/invoice-series/{id}/resume` | Resumes invoice generation for a paused series. Restart recurring billing after the pause condition has ended. |
| FX quotes | `fx().quote` | `GET /v1/fx/quote` | Returns an indicative conversion quote for an amount and currency pair. Display a conversion estimate before creating a payment. A quote is not a settlement guarantee. |
| Wallets | `wallets().list` | `GET /v1/wallets` | Lists wallets available to the authenticated account and environment. Choose a wallet before reading its balance or entries. |
| Wallets | `wallets().retrieve` | `GET /v1/wallets/{id}` | Reads one wallet's details. Load the wallet metadata for an account view. |
| Wallets | `wallets().balance` | `GET /v1/wallets/{id}/balance` | Reads the wallet's current balance. Show the available or pending balance for a wallet. |
| Wallets | `wallets().entries` | `GET /v1/wallets/{id}/entries` | Lists ledger entries with cursor pagination. Show a transaction statement; pass the returned cursor for the next page. |
| Webhooks | `webhooks().listEndpoints` | `GET /v1/webhook-endpoints` | Lists configured webhook destinations. Review which event destinations are active. |
| Webhooks | `webhooks().createEndpoint` | `POST /v1/webhook-endpoints` | Registers an HTTPS receiver and selected event types. The signing secret is shown only at creation. Connect your server to receive payment lifecycle events; store the returned secret securely. |
| Webhooks | `webhooks().updateEndpoint` | `PATCH /v1/webhook-endpoints/{id}` | Updates a destination's URL or subscribed event types. Change a receiver without creating a second endpoint. |
| Webhooks | `webhooks().disableEndpoint` | `POST /v1/webhook-endpoints/{id}/disable` | Stops future deliveries to an endpoint while retaining its record. Temporarily stop a destination while investigating or migrating it. |
| Webhooks | `webhooks().deleteEndpoint` | `DELETE /v1/webhook-endpoints/{id}` | Removes a webhook endpoint. Permanently remove a destination that is no longer used. |
| Webhooks | `webhooks().listDeliveries` | `GET /v1/webhook-deliveries` | Lists delivery attempts and their summary states. Monitor failed, pending, or completed event deliveries. |
| Webhooks | `webhooks().retrieveDelivery` | `GET /v1/webhook-deliveries/{id}` | Reads one delivery's status and attempt metadata. Inspect one delivery before deciding whether to retry it. |
| Webhooks | `webhooks().retryDelivery` | `POST /v1/webhook-deliveries/{id}/retry` | Queues a retry for an eligible failed delivery. Retry after fixing the receiver; the endpoint must be active. |
| Public catalog and optional support chat | `publicCatalog().listPaymentMethods` | `GET /v1/public/payment-methods` | Returns active payment methods and their public requirements without partner keys. Build a method selector and validate the selected method's required fields. |
| Public catalog and optional support chat | `publicCatalog().supportChat` | `POST /v1/public/support/chat` | Submits a support-chat request and returns the server-sent event stream as text. Use only when integrating the public support-chat feature; this is not a payment operation. |
| Contact helpers | `PhoneNumberUtils.normalize` | `Local helper` | Removes display punctuation and returns the phone value in the configured national or international form. Prepare a phone value before validating or sending it to a method that requires one. |
| Contact helpers | `PhoneNumberUtils.format` | `Local helper` | Formats a valid phone number for display; it does not change the API payload automatically. Show a readable phone number in a form or confirmation screen. |
| Contact helpers | `PhoneNumberUtils.addCountryPrefix` | `Local helper` | Adds the configured country calling code to a national number. An integration needs the international form, such as 258 plus national digits. |
| Contact helpers | `PhoneNumberUtils.removeCountryPrefix` | `Local helper` | Removes the configured country calling code and returns national digits. A selected method expects the national number without a country code. |
| Contact helpers | `PhoneNumberUtils.isValid` | `Local helper` | Checks length, allowed characters, country code, and configured prefixes; returns true or false. Reject invalid input before calling the API. It does not replace server-side validation. |
| Contact helpers | `PhoneNumberUtils.rulesForPaymentMethod` | `Local helper` | Returns known contact rules for the selected method as `Optional<Rules>`; `Optional.empty()` means no preset exists. Load rules before validating a phone; configure rules from the live catalog for other phone methods. |
| Contact helpers | `PhoneNumberUtils.normalizeForPaymentMethod` | `Local helper` | Normalizes the contact using the selected method's preset rules; passes the input through unchanged when there is no preset. |
| Contact helpers | `PhoneNumberUtils.formatForPaymentMethod` | `Local helper` | Formats the contact using the selected method's preset rules. Display the buyer's number according to the selected method's country rules. |
| Contact helpers | `PhoneNumberUtils.isValidForPaymentMethod` | `Local helper` | Validates the contact directly against a selected method's preset rules. Check a number in one call when the selected method has a known preset. |
| Contact helpers | `PhoneNumberUtils.requiresPhoneForPaymentMethod` | `Local helper` | Reports whether the SDK has a phone requirement preset for the method; unknown methods return false. Decide whether to show phone input only when the method's requirements are known. Check the live catalog too. |
| Webhook signature verification | `WebhookVerifier.verify` | `Local helper` | Checks the Payment-Signature HMAC and timestamp against the unmodified request body. Verify the signature before parsing or acting on an incoming webhook. Use the raw bytes and keep the secret server-side. |
| Client utilities | `client.sendRequest / sendPublicRequest` | `Raw API request` | Sends a request to an API path without a dedicated resource wrapper. sendRequest sends partner-key headers; sendPublicRequest omits them. Choose the method that matches the endpoint's authentication. Use only for a supported endpoint that has no SDK method; keep private keys on the server and apply the endpoint's scope and idempotency rules. |
| Client utilities | `EDPayClient.pathSegment(value)` | `Local helper` | Percent-encodes a value for use as one URL path segment. Build a custom request path safely when no resource method exists. |

Resource methods return JSON text. `EDPayException` exposes HTTP status and response body; use its request ID for support and honor `Retry-After` on HTTP 429.
## Official documentation

- [SDK installation and integration guide](https://docs-pay.edutecka.com/sdks/)
- [Payments API](https://docs-pay.edutecka.com/api/payments/)
- [Authentication and API keys](https://docs-pay.edutecka.com/guides/authentication/)
- [Webhooks](https://docs-pay.edutecka.com/api/webhooks/)

## License

MIT (as declared in `pom.xml`).

## Other official SDKs and plugins

### SDKs

- [JavaScript / TypeScript](https://github.com/edutecka/edpay-javascript-sdk)
- [Python](https://github.com/edutecka/edpay-python-sdk)
- [PHP](https://github.com/edutecka/edpay-php-sdk)
- [.NET](https://github.com/edutecka/edpay-dot-net-sdk)

### E-commerce integrations

These are links to official integration repositories; some contain previews. Review each repository's status and implementation limitations before installing.

- [WooCommerce (WordPress)](https://github.com/edutecka/edpay-woocommerce-plugin)
- [Shopify](https://github.com/edutecka/edpay-shopify-app)
- [Magento / Adobe Commerce](https://github.com/edutecka/edpay-magento2-module)
- [PrestaShop](https://github.com/edutecka/edpay-prestashop-module)
- [OpenCart](https://github.com/edutecka/edpay-opencart-extension)
- [WHMCS](https://github.com/edutecka/edpay-whmcs-gateway)
