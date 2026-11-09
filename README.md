# EDPay Java SDK

Official Java client for server-to-server payment integrations. The SDK uses Java's built-in HTTP client and returns API response bodies as JSON strings.

## Requirements

- Java 17 or later
- Maven 3.8 or later to build and install from source

## Build and install from source

The repository contains the SDK source. No remote Maven artifact repository is configured here; build and install the artifact locally before adding it to another project:

```bash
git clone https://github.com/edutecka/edpay-java-sdk.git
cd edpay-java-sdk
mvn test
mvn install
```

The Maven coordinates and version are defined in this repository's `pom.xml`:

```xml
<dependency>
  <groupId>mz.co.edpay</groupId>
  <artifactId>edpay-java-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

For Gradle, after running `mvn install`, add `mavenLocal()` to the repositories block and use:

```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.0.0'
```

## Create a payment

Set `EDPAY_PUBLIC_KEY` and `EDPAY_PRIVATE_KEY` in the process environment. Use sandbox credentials with the sandbox environment while testing; keep private keys out of source control.

```java
import mz.co.edpay.EDPayClient;

EDPayClient client = new EDPayClient.Builder()
    .publicKey(System.getenv("EDPAY_PUBLIC_KEY"))
    .privateKey(System.getenv("EDPAY_PRIVATE_KEY"))
    .environment(EDPayClient.Environment.SANDBOX)
    .build();

String payload = """
    {"amount":500.00,"currency":"MZN",
     "methods":["method_from_payment_methods"],
     "reference":"ORDER-10492","description":"Example order",
     "return_url":"https://shop.example/orders/ORDER-10492"}
    """;

String responseJson = client.payments().create(payload, "ORDER-10492");
System.out.println(responseJson);
```

`payments().create(payload, idempotencyKey)` sends `POST /v1/payments`. Replace `method_from_payment_methods` with a method identifier returned by `GET /v1/public/payment-methods`, and use the request fields required by the API reference. The SDK forwards the JSON string without request-model validation and returns the response body as a string. It sends `X-EDPay-Public-Key`, `X-EDPay-Private-Key`, `Content-Type: application/json`, and, when supplied, `Idempotency-Key`. The example amount is MZN 500.00. For hosted checkout, extract the `payment_url` from the API response and redirect the customer to it. Do not treat the browser return as payment confirmation.

To retrieve or cancel a payment, use `client.payments().retrieve(paymentId)` or `client.payments().cancel(paymentId)`. These call `GET /v1/payments/{paymentId}` and `POST /v1/payments/{paymentId}/cancel` respectively.

The SDK also exposes `client.checkout().createSession(jsonPayload, idempotencyKey)`, `retrieveSession(sessionId)`, and `expireSession(sessionId)` for `/v1/checkout/sessions` routes. These routes are implemented in this SDK, but are not described in the published API reference; confirm their availability and request schema with EDPay before using them. The documented hosted payment flow is to create a payment through `POST /v1/payments` and use the returned `payment_url`.

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
