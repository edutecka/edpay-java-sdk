package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class CheckoutResource {
    private final EDPayClient client;

    public CheckoutResource(EDPayClient client) {
        this.client = client;
    }

    public String createSession(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/checkout/sessions", jsonPayload, idempotencyKey);
    }

    public String retrieveSession(String sessionId) {
        return client.sendRequest("GET", "/v1/checkout/sessions/" + sessionId, null, null);
    }

    public String expireSession(String sessionId) {
        return client.sendRequest("POST", "/v1/checkout/sessions/" + sessionId + "/expire", "{}", null);
    }
}
