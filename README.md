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
  <version>1.0.0</version>
</dependency>
```

For Gradle, add `mavenLocal()` to the repositories block and use:

```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.0.0'
```

## Create a payment

Set `EDPAY_PUBLIC_KEY` and `EDPAY_PRIVATE_KEY` in the process environment. Keep the private key on your server. Use test keys for sandbox requests and set the API host explicitly:

```java
import mz.co.edpay.EDPayClient;

EDPayClient client = new EDPayClient.Builder()
    .publicKey(System.getenv("EDPAY_PUBLIC_KEY"))
    .privateKey(System.getenv("EDPAY_PRIVATE_KEY"))
    .environment(EDPayClient.Environment.SANDBOX)
    .baseUrl("https://api-pay.edutecka.com")
    .build();

String payload = """
    {"amount_minor":50000,"currency":"MZN",
     "method":"method_from_payment_methods",
     "phone":"+25884xxxxxxx",
     "reference":"ORDER-10492","description":"Example order"}
    """;

String responseJson = client.payments().create(payload, "ORDER-10492");
System.out.println(responseJson);
```

`payments().create(payload, idempotencyKey)` sends `POST /v1/payments` and returns the API response as JSON text. Replace `method_from_payment_methods` with an ID returned by `GET /v1/public/payment-methods`. `amount_minor` uses minor currency units: `50000` means MZN 500.00. Include `phone` when required by the selected method. The SDK sends `X-EDPay-Public-Key`, `X-EDPay-Private-Key`, and the optional `Idempotency-Key`.

Retrieve or cancel a payment with `client.payments().retrieve(paymentId)` and `client.payments().cancel(paymentId)`.

For hosted checkout, use `client.checkout().createSession(jsonPayload, idempotencyKey)`, then redirect the customer to the checkout URL returned by the API. Confirm the final payment using a server-side lookup or a verified webhook.

## Verify webhook signatures

Pass the exact raw request body, the `X-EDPay-Signature` header value, and the webhook signing secret to the verifier before processing the event:

```java
import mz.co.edpay.webhooks.WebhookVerifier;

boolean valid = WebhookVerifier.verify(rawRequestBody, signatureHeader, webhookSecret);
if (!valid) {
    throw new SecurityException("Invalid EDPay webhook signature");
}
```

The verifier checks a hexadecimal HMAC-SHA256 signature. Do not parse and re-serialize the body before verification.

## Available operations

- Payments: `create`, `retrieve`, `cancel`
- Checkout sessions: `createSession`, `retrieveSession`, `expireSession`
- Invoices: `create`, `list`, `retrieve`, `finalize`, `voidInvoice`
- Subscriptions: `create`, `list`, `retrieve`, `pause`, `resume`, `cancel`
- Webhooks: `WebhookVerifier.verify`

The SDK accepts request bodies as JSON strings and returns JSON strings. Parse the response to read the payment ID, status, and any checkout URL.

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
