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

    public String list(String queryParams) {
        String path = "/v1/invoices" + (queryParams != null && !queryParams.isBlank() ? "?" + queryParams : "");
        return client.sendRequest("GET", path, null, null);
    }

    public String retrieve(String invoiceId) {
        return client.sendRequest("GET", "/v1/invoices/" + invoiceId, null, null);
    }

    public String finalize(String invoiceId) {
        return client.sendRequest("POST", "/v1/invoices/" + invoiceId + "/finalize", "{}", null);
    }

    public String voidInvoice(String invoiceId) {
        return client.sendRequest("POST", "/v1/invoices/" + invoiceId + "/void", "{}", null);
    }
}
