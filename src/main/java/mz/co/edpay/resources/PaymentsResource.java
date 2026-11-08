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

    public String retrieve(String paymentId) {
        return client.sendRequest("GET", "/v1/payments/" + paymentId, null, null);
    }

    public String cancel(String paymentId) {
        return client.sendRequest("POST", "/v1/payments/" + paymentId + "/cancel", "{}", null);
    }
}
