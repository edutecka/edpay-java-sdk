package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class WebhooksResource {
    private final EDPayClient client;
    public WebhooksResource(EDPayClient client) { this.client = client; }

    public String listEndpoints() { return client.sendRequest("GET", "/v1/webhook-endpoints", null, null); }
    public String createEndpoint(String jsonPayload, String idempotencyKey) { return client.sendRequest("POST", "/v1/webhook-endpoints", jsonPayload, idempotencyKey); }
    public String updateEndpoint(String endpointId, String jsonPayload) { return client.sendRequest("PATCH", "/v1/webhook-endpoints/" + EDPayClient.pathSegment(endpointId), jsonPayload, null); }
    public String disableEndpoint(String endpointId) { return client.sendRequest("POST", "/v1/webhook-endpoints/" + EDPayClient.pathSegment(endpointId) + "/disable", "{}", null); }
    public String deleteEndpoint(String endpointId) { return client.sendRequest("DELETE", "/v1/webhook-endpoints/" + EDPayClient.pathSegment(endpointId), null, null); }
    public String listDeliveries() { return listDeliveries(50); }
    public String listDeliveries(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return client.sendRequest("GET", "/v1/webhook-deliveries?limit=" + limit, null, null);
    }
    public String retrieveDelivery(String deliveryId) { return client.sendRequest("GET", "/v1/webhook-deliveries/" + EDPayClient.pathSegment(deliveryId), null, null); }
    public String retryDelivery(String deliveryId) { return client.sendRequest("POST", "/v1/webhook-deliveries/" + EDPayClient.pathSegment(deliveryId) + "/retry", "{}", null); }
}
