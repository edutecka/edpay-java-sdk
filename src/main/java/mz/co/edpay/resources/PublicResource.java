package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class PublicResource {
    private final EDPayClient client;
    public PublicResource(EDPayClient client) { this.client = client; }
    public String listPaymentMethods() { return client.sendPublicRequest("GET", "/v1/public/payment-methods", null); }

    /** Posts the public support-chat request and returns its server-sent event stream as text. */
    public String supportChat(String jsonPayload) {
        return client.sendPublicRequest("POST", "/v1/public/support/chat", jsonPayload);
    }
}
