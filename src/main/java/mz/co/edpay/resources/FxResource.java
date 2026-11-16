package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FxResource {
    private final EDPayClient client;
    public FxResource(EDPayClient client) { this.client = client; }

    public String quote(String fromCurrency, String toCurrency, long amountMinor) {
        if (amountMinor < 1) throw new IllegalArgumentException("amountMinor must be positive");
        String query = "from_currency=" + enc(fromCurrency) + "&to_currency=" + enc(toCurrency) + "&amount_minor=" + amountMinor;
        return client.sendRequest("GET", "/v1/fx/quote?" + query, null, null);
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
