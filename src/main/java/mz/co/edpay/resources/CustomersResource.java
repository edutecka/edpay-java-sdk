package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class CustomersResource {
    private final EDPayClient client;
    public CustomersResource(EDPayClient client) { this.client = client; }

    public String create(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/customers", jsonPayload, idempotencyKey);
    }
    public String list() { return client.sendRequest("GET", "/v1/customers", null, null); }
    public String retrieve(String customerId) {
        return client.sendRequest("GET", "/v1/customers/" + EDPayClient.pathSegment(customerId), null, null);
    }
    public String update(String customerId, String jsonPayload, String idempotencyKey) {
        return client.sendRequest("PATCH", "/v1/customers/" + EDPayClient.pathSegment(customerId), jsonPayload, idempotencyKey);
    }
}
