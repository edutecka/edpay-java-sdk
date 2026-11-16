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
  <version>1.4.0</version>
</dependency>
```

For Gradle, add `mavenLocal()` to the repositories block and use:

```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.4.0'
```

## Create a payment

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

## Available operations

- Payments: create (`/v1/payments` and `/v1/payment-checkout`), list (limit 1–100), retrieve, events and attempts.
- Checkout: create session; retrieve session/payment link; frame policy; create/retrieve session and payment-link payments.
- Customers: create, list (up to 100), retrieve and update. Payment links: create, list (up to 100), retrieve and deactivate.
- Invoices: create, list (default or explicit limit 1–100), retrieve, finalize and void. Invoice series: create, list (default or explicit limit 1–100), pause and resume; the API has no series-detail operation.
- FX quote; wallets (list, retrieve, balance, cursor-paginated entries, limit 1–100); public payment methods; webhooks (endpoint list/create/update/disable/delete and delivery list/retrieve/retry, limit 1–100).
- Public support chat: post optional `session_id` and `messages[]` (`role`, `content`) JSON and receive the server-sent event stream as text (default request timeout: 60 seconds; the API stream can run for up to 45 seconds).
- Contacts: generic `normalize`, `format`, `addCountryPrefix`, `removeCountryPrefix`, `isValid`; method-aware `rulesForPaymentMethod`, `normalizeForPaymentMethod`, `formatForPaymentMethod`, `isValidForPaymentMethod`, and `requiresPhoneForPaymentMethod`.
- Webhook signature verification: `WebhookVerifier.verify`

On non-success responses, `EDPayException` exposes the HTTP status and response body, plus `getRequestId()` (`X-Request-Id`) and `getRetryAfter()` (`Retry-After`, when present). Use the request ID when contacting support and honor the retry delay on HTTP 429.

The SDK accepts request bodies as JSON strings and returns JSON strings. Request schemas use `amount_minor`, `currency`, `method`, conditional `phone`, optional `customer_id`, `reference`, `description`, and `channel` for payments; invoice `items[]` have `description`, integer `quantity`, and `unit_amount_minor`; invoice series add `interval_unit`, `interval_count`, `starts_at`, `due_days`, and optional `ends_at`; payment links use `amount_minor`, `currency`, optional `expires_at`; customers use `name` and optional `email`; webhook endpoints use `url` and `events[]`; checkout sessions use `payment_link_id`. Partner API errors use `{error:{type,code,message,details},meta:{request_id}}`; `EDPayException` exposes status, response body, request ID, and retry-after metadata.

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
