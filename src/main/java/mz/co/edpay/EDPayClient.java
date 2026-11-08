package mz.co.edpay;

import mz.co.edpay.exceptions.EDPayException;
import mz.co.edpay.resources.PaymentsResource;
import mz.co.edpay.resources.CheckoutResource;
import mz.co.edpay.resources.InvoicesResource;
import mz.co.edpay.resources.SubscriptionsResource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EDPayClient {

    public enum Environment {
        PRODUCTION("https://api.edpay.co.mz"),
        SANDBOX("https://sandbox-api.edpay.co.mz");

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

    private final PaymentsResource payments;
    private final CheckoutResource checkout;
    private final InvoicesResource invoices;
    private final SubscriptionsResource subscriptions;

    private EDPayClient(Builder builder) {
        this.publicKey = builder.publicKey;
        this.privateKey = builder.privateKey;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : builder.environment.getBaseUrl();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(builder.timeoutSeconds))
            .build();

        this.payments = new PaymentsResource(this);
        this.checkout = new CheckoutResource(this);
        this.invoices = new InvoicesResource(this);
        this.subscriptions = new SubscriptionsResource(this);
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

    public String sendRequest(String method, String endpoint, String jsonBody, String idempotencyKey) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("X-EDPay-Public-Key", publicKey)
                .header("X-EDPay-Private-Key", privateKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "EDPay-Java-SDK/1.0.0");

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                reqBuilder.header("Idempotency-Key", idempotencyKey);
            }

            if ("POST".equalsIgnoreCase(method)) {
                reqBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
            } else if ("PUT".equalsIgnoreCase(method)) {
                reqBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EDPayException("EDPay API error: " + response.statusCode(), response.statusCode(), response.body());
            }
            return response.body();
        } catch (EDPayException e) {
            throw e;
        } catch (Exception e) {
            throw new EDPayException("Failed to send request to EDPay: " + e.getMessage(), 0, null);
        }
    }

    public static class Builder {
        private String publicKey;
        private String privateKey;
        private Environment environment = Environment.PRODUCTION;
        private String baseUrl;
        private int timeoutSeconds = 30;

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
            if (publicKey == null || privateKey == null) {
                throw new IllegalArgumentException("Both publicKey and privateKey must be provided.");
            }
            return new EDPayClient(this);
        }
    }
}
