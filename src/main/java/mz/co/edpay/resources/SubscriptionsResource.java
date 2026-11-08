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

    public String list(String queryParams) {
        String path = "/v1/invoice-series" + (queryParams != null && !queryParams.isBlank() ? "?" + queryParams : "");
        return client.sendRequest("GET", path, null, null);
    }

    public String retrieve(String seriesId) {
        return client.sendRequest("GET", "/v1/invoice-series/" + seriesId, null, null);
    }

    public String pause(String seriesId) {
        return client.sendRequest("POST", "/v1/invoice-series/" + seriesId + "/pause", "{}", null);
    }

    public String resume(String seriesId) {
        return client.sendRequest("POST", "/v1/invoice-series/" + seriesId + "/resume", "{}", null);
    }

    public String cancel(String seriesId) {
        return client.sendRequest("POST", "/v1/invoice-series/" + seriesId + "/cancel", "{}", null);
    }
}
