package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class SubscriptionsResource {
    private final EDPayClient client;

    public SubscriptionsResource(EDPayClient client) {
        this.client = client;
    }

    public String create(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/invoice-series", jsonPayload, idempotencyKey);
    }

    public String list() { return client.sendRequest("GET", "/v1/invoice-series", null, null); }
    public String list(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return client.sendRequest("GET", "/v1/invoice-series?limit=" + limit, null, null);
    }

    public String pause(String seriesId) {
        return client.sendRequest("POST", "/v1/invoice-series/" + EDPayClient.pathSegment(seriesId) + "/pause", "{}", null);
    }

    public String resume(String seriesId) {
        return client.sendRequest("POST", "/v1/invoice-series/" + EDPayClient.pathSegment(seriesId) + "/resume", "{}", null);
    }
}
