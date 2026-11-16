package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class PaymentLinksResource {
    private final EDPayClient client;
    public PaymentLinksResource(EDPayClient client) { this.client = client; }

    public String create(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/payment-links", jsonPayload, idempotencyKey);
    }
    public String list() { return client.sendRequest("GET", "/v1/payment-links", null, null); }
    public String retrieve(String linkId) {
        return client.sendRequest("GET", "/v1/payment-links/" + EDPayClient.pathSegment(linkId), null, null);
    }
    public String deactivate(String linkId, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/payment-links/" + EDPayClient.pathSegment(linkId) + "/deactivate", "{}", idempotencyKey);
    }
    public String createCheckoutSession(String linkId, String embedOrigin, String idempotencyKey) {
        String payload = "{\"payment_link_id\":\"" + jsonEscape(linkId) + "\"" +
            (embedOrigin == null ? "" : ",\"embed_origin\":\"" + jsonEscape(embedOrigin) + "\"") + "}";
        return client.sendRequest("POST", "/v1/checkout/sessions", payload, idempotencyKey);
    }
    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
