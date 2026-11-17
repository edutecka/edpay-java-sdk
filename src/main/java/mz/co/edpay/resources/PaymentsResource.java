package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class PaymentsResource {
    private final EDPayClient client;
    public PaymentsResource(EDPayClient client) { this.client = client; }

    public String create(String jsonPayload, String idempotencyKey) { return client.sendRequest("POST", "/v1/payments", jsonPayload, idempotencyKey); }
    public String privateCheckout(String paymentId, String jsonPayload, String idempotencyKey) { return client.sendRequest("POST", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/private-checkout", jsonPayload, idempotencyKey); }
    public String publicCheckout(String paymentId, String jsonPayload, String idempotencyKey) { return client.sendRequest("POST", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/public-checkout", jsonPayload, idempotencyKey); }
    public String linkCheckout(String paymentId, String jsonPayload, String idempotencyKey) { return client.sendRequest("POST", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/link-checkout", jsonPayload, idempotencyKey); }
    /** Lists executions with their checkout mode and sessions with no submission. */
    public String listCheckouts(String paymentId) { return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/checkouts", null, null); }
    public String getCheckout(String token) { return client.sendPublicRequest("GET", "/v1/payments/checkouts/" + EDPayClient.pathSegment(token), null); }
    public String submitCheckoutPayment(String token, String jsonPayload, String idempotencyKey) { return client.sendPublicRequest("POST", "/v1/payments/checkouts/" + EDPayClient.pathSegment(token) + "/payment", jsonPayload, idempotencyKey); }
    public String retrieveCheckoutPayment(String token) { return client.sendPublicRequest("GET", "/v1/payments/checkouts/" + EDPayClient.pathSegment(token) + "/payment", null); }
    public String framePolicy(String token) { return client.sendPublicRequest("GET", "/v1/payments/checkouts/" + EDPayClient.pathSegment(token) + "/frame-policy", null); }
    public String list(int limit) { if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100"); return client.sendRequest("GET", "/v1/payments?limit=" + limit, null, null); }
    public String list() { return list(50); }
    public String retrieve(String paymentId) { return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId), null, null); }
    public String events(String paymentId) { return client.sendRequest("GET", "/v1/payments/" + EDPayClient.pathSegment(paymentId) + "/events", null, null); }
}
