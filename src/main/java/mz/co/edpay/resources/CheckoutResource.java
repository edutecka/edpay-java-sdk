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

    public String retrieveSession(String token) {
        return client.sendPublicRequest("GET", "/v1/checkout/sessions/" + EDPayClient.pathSegment(token), null);
    }

    public String getPaymentLink(String linkId) {
        return client.sendPublicRequest("GET", "/v1/checkout/payment-links/" + EDPayClient.pathSegment(linkId), null);
    }

    public String framePolicy(String token) {
        return client.sendPublicRequest("GET", "/v1/checkout/sessions/" + EDPayClient.pathSegment(token) + "/frame-policy", null);
    }

    public String createSessionPayment(String token, String jsonPayload, String idempotencyKey) {
        return client.sendPublicRequest("POST", "/v1/checkout/sessions/" + EDPayClient.pathSegment(token) + "/payments", jsonPayload, idempotencyKey);
    }

    public String retrieveSessionPayment(String token, String paymentId) {
        return client.sendPublicRequest("GET", "/v1/checkout/sessions/" + EDPayClient.pathSegment(token) + "/payments/" + EDPayClient.pathSegment(paymentId), null);
    }

    public String createPaymentLinkPayment(String linkId, String jsonPayload, String idempotencyKey) {
        return client.sendPublicRequest("POST", "/v1/checkout/payment-links/" + EDPayClient.pathSegment(linkId) + "/payments", jsonPayload, idempotencyKey);
    }

    public String retrievePaymentLinkPayment(String linkId, String paymentId) {
        return client.sendPublicRequest("GET", "/v1/checkout/payment-links/" + EDPayClient.pathSegment(linkId) + "/payments/" + EDPayClient.pathSegment(paymentId), null);
    }
}
