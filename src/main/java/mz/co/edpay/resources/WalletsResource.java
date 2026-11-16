package mz.co.edpay.resources;

import mz.co.edpay.EDPayClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WalletsResource {
    private final EDPayClient client;
    public WalletsResource(EDPayClient client) { this.client = client; }

    public String list() { return client.sendRequest("GET", "/v1/wallets", null, null); }
    public String retrieve(String walletId) { return client.sendRequest("GET", "/v1/wallets/" + EDPayClient.pathSegment(walletId), null, null); }
    public String balance(String walletId) { return client.sendRequest("GET", "/v1/wallets/" + EDPayClient.pathSegment(walletId) + "/balance", null, null); }
    public String entries(String walletId) { return entries(walletId, null, 50); }
    public String entries(String walletId, String cursor) { return entries(walletId, cursor, 50); }
    public String entries(String walletId, String cursor, Integer limit) {
        if (limit != null && (limit < 1 || limit > 100)) throw new IllegalArgumentException("limit must be between 1 and 100");
        StringBuilder query = new StringBuilder();
        if (cursor != null && !cursor.isBlank()) query.append("cursor=").append(URLEncoder.encode(cursor, StandardCharsets.UTF_8));
        if (limit != null) {
            if (!query.isEmpty()) query.append('&');
            query.append("limit=").append(limit);
        }
        String suffix = query.isEmpty() ? "" : "?" + query;
        return client.sendRequest("GET", "/v1/wallets/" + EDPayClient.pathSegment(walletId) + "/entries" + suffix, null, null);
    }
}
