package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;

public class InvoicesResource {
    private final EDPayClient client;

    public InvoicesResource(EDPayClient client) {
        this.client = client;
    }

    public String create(String jsonPayload, String idempotencyKey) {
        return client.sendRequest("POST", "/v1/invoices", jsonPayload, idempotencyKey);
    }

    public String list() { return client.sendRequest("GET", "/v1/invoices", null, null); }
    public String list(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return client.sendRequest("GET", "/v1/invoices?limit=" + limit, null, null);
    }

    public String retrieve(String invoiceId) {
        return client.sendRequest("GET", "/v1/invoices/" + EDPayClient.pathSegment(invoiceId), null, null);
    }

    public String finalize(String invoiceId) {
        return client.sendRequest("POST", "/v1/invoices/" + EDPayClient.pathSegment(invoiceId) + "/finalize", "{}", null);
    }

    public String voidInvoice(String invoiceId) {
        return client.sendRequest("POST", "/v1/invoices/" + EDPayClient.pathSegment(invoiceId) + "/void", "{}", null);
    }
}
