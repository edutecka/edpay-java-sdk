package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class PaymentsResource {
    private final EDPayClient client;

    public PaymentsResource(EDPayClient client) {
        this.client = client;
    }

    public String create(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/payments", jsonPayload, idempotencyKey);
    }

    public String createCheckout(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/payment-checkout", jsonPayload, idempotencyKey);
    }

    public String list(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return client.sendRequest("GET", "/v1/payments?limit=" + limit, null, null);
    }

    public String list() { return list(50); }

    public String retrieve(String paymentId) {
        return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId), null, null);
    }

    public String events(String paymentId) {
        return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/events", null, null);
    }

    public String attempts(String paymentId) {
        return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/attempts", null, null);
    }
}
