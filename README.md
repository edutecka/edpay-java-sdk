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
  <version>1.6.1</version>
</dependency>
```

For Gradle, add `mavenLocal()` to the repositories block and use:

```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.6.1'
```

## Create a payment

Every partner API request requires both the public key (`pk_test_` or `pk_live_`) and private key (`sk_test_` or `sk_live_`) from the same environment. The SDK rejects missing, blank, or mismatched key pairs when building the client. Public catalog requests and buyer-facing checkout requests authenticated by a checkout token do not use the partner key pair; keep the private key on your server.

Set `EDPAY_PUBLIC_KEY` and `EDPAY_PRIVATE_KEY` in the process environment. Keep the private key on your server. Use matching test keys for test requests or live keys for production. The key prefixes select the environment; both use the same API host:

```java
import mz.co.edpay.EDPayClient;
import mz.co.edpay.contacts.PhoneNumberUtils;
import java.util.Objects;

EDPayClient client = new EDPayClient.Builder()
    .publicKey(System.getenv("EDPAY_PUBLIC_KEY"))
    .privateKey(System.getenv("EDPAY_PRIVATE_KEY"))
    .build();

String payload = """
    {"amount_minor":50000,"currency":"MZN",
     "reference":"ORDER-10492","description":"Example order"}
    """;

String responseJson = client.payments().create(payload, "ORDER-10492-CREATE-1");
System.out.println(responseJson);
```

`payments().create(payload, idempotencyKey)` sends `POST /v1/payments` and returns the API response as JSON text. `amount_minor` is a positive integer in minor currency units (`50000` means MZN 500.00); `currency` must be `MZN`, `USD`, or `EUR` and match the account base currency. Optional payment fields also include `customer_id`, `reference`, and `description`. Partner API routes send key headers; the public catalog and token-based checkout omit them. Writes include an `Idempotency-Key` where the endpoint requires one.

Retrieve remote status and lifecycle with `client.payments().retrieve(paymentId)` and `.events(paymentId)`. Use `.listCheckouts(paymentId)` to read private executions, public/link executions, and sessions where no method was submitted; each execution keeps its originating checkout mode.

For hosted checkout, create or reuse the parent payment and call `publicCheckout(paymentId, payload, idempotencyKey)`. Give the returned opaque token to the browser checkout. Confirm the final payment with a server-side lookup or verified webhook; do not treat a browser redirect as confirmation.

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

The delivered event uses `type` for the event name. Match the local order by `reference`:

```json
{"id":"evt_0123456789abcdef","object":"event","type":"payment.succeeded","created_at":"2026-11-07T12:05:00Z","data":{"object":"payment","id":"pay_0123456789abcdef","reference":"ORDER-10492","trans_id":null,"method":"mpesa","status":"succeeded","amount_minor":50000,"currency":"MZN","platform_fee_minor":3000,"receiver_net_minor":47000,"created_at":"2026-11-07T12:04:00Z"}}
```

The Java model is `mz.co.edpay.webhooks.WebhookEvent`. Match the event to the order using its merchant reference, then process status changes idempotently.

## Choose a checkout mode for one payment

`payments().create()` creates the parent record only. Save its ID and reference with your order, then choose one of the child checkout operations. The same payment can use private API processing, hosted checkout, or a shareable link checkout; its ID, amount, and reference stay the same.

```java
String paymentId = /* data.id returned by payments().create(...) */;
String resultJson = client.payments().publicCheckout(
    paymentId, "{}", "ORDER-10492-PUBLIC-CHECKOUT");
// Or call privateCheckout(paymentId, payload, idempotencyKey)
// or linkCheckout(paymentId, payload, idempotencyKey).
```

Use `listCheckouts(paymentId)` to read private executions, public/link executions, and sessions where no method was submitted. Use `retrieve(paymentId)` to read current payment status. For buyer routes, use the opaque token returned by `publicCheckout` or `linkCheckout`; keep partner keys on your server.

### M-Pesa and e-Mola PIN confirmation

For `mpesa` and `emola` calls to `privateCheckout`, set a 60-second confirmation window and ask the buyer to approve the phone prompt and enter the PIN. The API returns `202 Accepted` when the execution is queued; it does not wait for PIN entry or confirm payment. Keep the payment pending and wait for a verified webhook or poll `retrieve(paymentId)` / `listCheckouts(paymentId)` for the final status. If no confirmation arrives within 60 seconds, continue reconciliation; do not resubmit while the execution is pending or unknown.

## Function reference

Each row tells you what the call does and when to use it. Partner calls use the API key pair; public catalog and buyer checkout calls do not. Supply a stable `Idempotency-Key` for writes where required.

| Resource | Function | Endpoint | What it does / when to use it |
|---|---|---|---|
| Payments | `payments().create` | `POST /v1/payments` | Creates the parent payment only. Save its ID and reference, then choose a checkout mode. |
| Payments | `payments().privateCheckout` | `POST /v1/payments/{id}/private-checkout` | Submits a selected method from your server for the existing payment. Use when your backend controls the interface. |
| Payments | `payments().publicCheckout` | `POST /v1/payments/{id}/public-checkout` | Creates an opaque token for EDPay-hosted checkout on the existing payment. |
| Payments | `payments().linkCheckout` | `POST /v1/payments/{id}/link-checkout` | Creates a shareable buyer token for the existing payment; it does not create another payment. |
| Payments | `payments().listCheckouts` | `GET /v1/payments/{id}/checkouts` | Lists executions with their checkout mode and sessions where no method was submitted. |
| Payments | `payments().retrieve` | `GET /v1/payments/{id}` | Retrieves remote payment status. |
| Payments | `payments().events` | `GET /v1/payments/{id}/events` | Reads recorded payment status changes. |
| Payments | `payments().list` | `GET /v1/payments` | Lists account payments for reconciliation or history. |
| Hosted checkout | `payments().getCheckout` | `GET /v1/payments/checkouts/{token}` | Loads the buyer-facing hosted or link checkout with its opaque token. |
| Hosted checkout | `payments().submitCheckoutPayment` | `POST /v1/payments/checkouts/{token}/payment` | Submits the buyer-selected method and required method fields with an idempotency key. |
| Hosted checkout | `payments().retrieveCheckoutPayment` | `GET /v1/payments/checkouts/{token}/payment` | Reads the parent payment status without exposing its ID to the buyer. |
| Hosted checkout | `payments().framePolicy` | `GET /v1/payments/checkouts/{token}/frame-policy` | Checks whether the hosted checkout can be embedded. |
| Customers | `customers().create` | `POST /v1/customers` | Creates a customer record for the authenticated account. Save customer details before associating them with payments or invoices. |
| Customers | `customers().list` | `GET /v1/customers` | Lists customers in the current account and environment. Build a customer picker or reconcile customer records. |
| Customers | `customers().retrieve` | `GET /v1/customers/{id}` | Reads one customer record. Load a customer's saved details by ID. |
| Customers | `customers().update` | `PATCH /v1/customers/{id}` | Updates the supplied customer fields. Correct or refresh a customer's details; use a stable idempotency key where required. |
| Invoices | `invoices().create` | `POST /v1/invoices` | Creates a draft invoice with its customer and line items. Prepare an invoice before issuing it to the customer. |
| Invoices | `invoices().list` | `GET /v1/invoices` | Lists invoices, optionally limited to a page size. Show or reconcile invoices for the account. |
| Invoices | `invoices().retrieve` | `GET /v1/invoices/{id}` | Reads one invoice and its current state. Load invoice details or check its lifecycle. |
| Invoices | `invoices().finalize` | `POST /v1/invoices/{id}/finalize` | Issues the draft invoice and makes it available for payment. The invoice is complete and ready to send to the customer. |
| Invoices | `invoices().voidInvoice` | `POST /v1/invoices/{id}/void` | Voids an invoice that should no longer be payable. Cancel an issued invoice according to its allowed state transitions. |
| Invoice series | `subscriptions().create` | `POST /v1/invoices/series` | Creates a schedule that issues invoices on the configured interval. Bill a customer repeatedly using a defined start date, interval, and due period. |
| Invoice series | `subscriptions().list` | `GET /v1/invoices/series` | Lists invoice series; there is no series-detail endpoint in this API version. Review recurring schedules for the account. |
| Invoice series | `subscriptions().pause` | `POST /v1/invoices/series/{id}/pause` | Pauses future invoice generation for a series. Temporarily stop recurring billing without deleting the schedule. |
| Invoice series | `subscriptions().resume` | `POST /v1/invoices/series/{id}/resume` | Resumes invoice generation for a paused series. Restart recurring billing after the pause condition has ended. |
| FX quotes | `fx().quote` | `GET /v1/fx/quote` | Returns an indicative conversion quote for an amount and currency pair. Display a conversion estimate before creating a payment. A quote is not a settlement guarantee. |
| Wallets | `wallets().list` | `GET /v1/wallets` | Lists wallets available to the authenticated account and environment. Choose a wallet before reading its balance or entries. |
| Wallets | `wallets().retrieve` | `GET /v1/wallets/{id}` | Reads one wallet's details. Load the wallet metadata for an account view. |
| Wallets | `wallets().balance` | `GET /v1/wallets/{id}/balance` | Reads the wallet's current balance. Show the available or pending balance for a wallet. |
| Wallets | `wallets().entries` | `GET /v1/wallets/{id}/entries` | Lists ledger entries with cursor pagination. Show a transaction statement; pass the returned cursor for the next page. |
| Webhooks | `webhooks().listEndpoints` | `GET /v1/webhooks/endpoints` | Lists configured webhook destinations. Review which event destinations are active. |
| Webhooks | `webhooks().createEndpoint` | `POST /v1/webhooks/endpoints` | Registers an HTTPS receiver and selected event types. The signing secret is shown only at creation. Connect your server to receive payment lifecycle events; store the returned secret securely. |
| Webhooks | `webhooks().updateEndpoint` | `PATCH /v1/webhooks/endpoints/{id}` | Updates a destination's URL or subscribed event types. Change a receiver without creating a second endpoint. |
| Webhooks | `webhooks().disableEndpoint` | `POST /v1/webhooks/endpoints/{id}/disable` | Stops future deliveries to an endpoint while retaining its record. Temporarily stop a destination while investigating or migrating it. |
| Webhooks | `webhooks().deleteEndpoint` | `DELETE /v1/webhooks/endpoints/{id}` | Removes a webhook endpoint. Permanently remove a destination that is no longer used. |
| Webhooks | `webhooks().listDeliveries` | `GET /v1/webhooks/deliveries` | Lists delivery attempts and their summary states. Monitor failed, pending, or completed event deliveries. |
| Webhooks | `webhooks().retrieveDelivery` | `GET /v1/webhooks/deliveries/{id}` | Reads one delivery's status and attempt metadata. Inspect one delivery before deciding whether to retry it. |
| Webhooks | `webhooks().retryDelivery` | `POST /v1/webhooks/deliveries/{id}/retry` | Queues a retry for an eligible failed delivery. Retry after fixing the receiver; the endpoint must be active. |
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
