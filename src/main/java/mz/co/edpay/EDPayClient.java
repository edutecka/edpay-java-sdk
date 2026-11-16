package mz.co.edpay;

import mz.co.edpay.exceptions.EDPayException;
import mz.co.edpay.resources.PaymentsResource;
import mz.co.edpay.resources.CheckoutResource;
import mz.co.edpay.resources.InvoicesResource;
import mz.co.edpay.resources.SubscriptionsResource;
import mz.co.edpay.resources.CustomersResource;
import mz.co.edpay.resources.PaymentLinksResource;
import mz.co.edpay.resources.WalletsResource;
import mz.co.edpay.resources.FxResource;
import mz.co.edpay.resources.WebhooksResource;
import mz.co.edpay.resources.PublicResource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class EDPayClient {

    public enum Environment {
        PRODUCTION("https://api-pay.edutecka.com"),
        SANDBOX("https://api-pay.edutecka.com");

        private final String baseUrl;
        Environment(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        public String getBaseUrl() {
            return baseUrl;
        }
    }

    private final String publicKey;
    private final String privateKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final int timeoutSeconds;

    private final PaymentsResource payments;
    private final CheckoutResource checkout;
    private final InvoicesResource invoices;
    private final SubscriptionsResource subscriptions;
    private final CustomersResource customers;
    private final PaymentLinksResource paymentLinks;
    private final WalletsResource wallets;
    private final FxResource fx;
    private final WebhooksResource webhooks;
    private final PublicResource publicCatalog;

    private EDPayClient(Builder builder) {
        this.publicKey = builder.publicKey;
        this.privateKey = builder.privateKey;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : builder.environment.getBaseUrl();
        this.timeoutSeconds = builder.timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(builder.timeoutSeconds))
            .build();

        this.payments = new PaymentsResource(this);
        this.checkout = new CheckoutResource(this);
        this.invoices = new InvoicesResource(this);
        this.subscriptions = new SubscriptionsResource(this);
        this.customers = new CustomersResource(this);
        this.paymentLinks = new PaymentLinksResource(this);
        this.wallets = new WalletsResource(this);
        this.fx = new FxResource(this);
        this.webhooks = new WebhooksResource(this);
        this.publicCatalog = new PublicResource(this);
    }

    public PaymentsResource payments() {
        return payments;
    }

    public CheckoutResource checkout() {
        return checkout;
    }

    public InvoicesResource invoices() {
        return invoices;
    }

    public SubscriptionsResource subscriptions() {
        return subscriptions;
    }

    public CustomersResource customers() { return customers; }
    public PaymentLinksResource paymentLinks() { return paymentLinks; }
    public WalletsResource wallets() { return wallets; }
    public FxResource fx() { return fx; }
    public WebhooksResource webhooks() { return webhooks; }
    public PublicResource publicCatalog() { return publicCatalog; }

    public String sendRequest(String method, String endpoint, String jsonBody, String idempotencyKey) {
        return sendRequest(method, endpoint, jsonBody, idempotencyKey, Map.of());
    }

    public String sendRequest(String method, String endpoint, String jsonBody, String idempotencyKey, Map<String, String> extraHeaders) {
        return sendRequest(method, endpoint, jsonBody, idempotencyKey, extraHeaders, true);
    }

    public String sendPublicRequest(String method, String endpoint, String jsonBody) {
        return sendPublicRequest(method, endpoint, jsonBody, null);
    }

    public String sendPublicRequest(String method, String endpoint, String jsonBody, String idempotencyKey) {
        return sendRequest(method, endpoint, jsonBody, idempotencyKey, Map.of(), false);
    }

    private String sendRequest(String method, String endpoint, String jsonBody, String idempotencyKey, Map<String, String> extraHeaders, boolean authenticated) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "EDPay-Java-SDK/1.5.1");

            if (authenticated) {
                reqBuilder.header("X-EDPay-Public-Key", publicKey)
                    .header("X-EDPay-Private-Key", privateKey);
            }

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                reqBuilder.header("Idempotency-Key", idempotencyKey);
            }
            extraHeaders.forEach(reqBuilder::header);
            String verb = method.toUpperCase(java.util.Locale.ROOT);
            HttpRequest.BodyPublisher body = jsonBody == null || jsonBody.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
            reqBuilder.method(verb, body);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EDPayException(
                    "EDPay API error: " + response.statusCode(),
                    response.statusCode(),
                    response.body(),
                    response.headers().firstValue("X-Request-Id").orElse(null),
                    response.headers().firstValue("Retry-After").orElse(null)
                );
            }
            return response.body();
        } catch (EDPayException e) {
            throw e;
        } catch (Exception e) {
            throw new EDPayException("Failed to send request to EDPay: " + e.getMessage(), 0, null);
        }
    }

    public static String pathSegment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static class Builder {
        private String publicKey;
        private String privateKey;
        private Environment environment = Environment.PRODUCTION;
        private String baseUrl;
        private int timeoutSeconds = 60;

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder environment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public EDPayClient build() {
            String normalizedPublicKey = publicKey == null ? "" : publicKey.trim();
            String normalizedPrivateKey = privateKey == null ? "" : privateKey.trim();
            String publicMode = normalizedPublicKey.matches("^pk_(test|live)_[0-9a-fA-F]{24}$")
                ? (normalizedPublicKey.startsWith("pk_test_") ? "test" : "live") : null;
            String privateMode = normalizedPrivateKey.matches("^sk_(test|live)_[0-9a-fA-F]{64}$")
                ? (normalizedPrivateKey.startsWith("sk_test_") ? "test" : "live") : null;
            if (publicMode == null || privateMode == null) {
                throw new IllegalArgumentException("A valid pk_test_/pk_live_ publicKey (24 hex characters) and sk_test_/sk_live_ privateKey (64 hex characters) must be provided.");
            }
            if (!publicMode.equals(privateMode)) {
                throw new IllegalArgumentException("EDPay publicKey and privateKey must use the same environment (test or live).");
            }
            this.publicKey = normalizedPublicKey;
            this.privateKey = normalizedPrivateKey;
            return new EDPayClient(this);
        }
    }
}
