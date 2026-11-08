package com.edutecka.edpay;

public class EDPayClient {
    private final String publicKey;
    private final String privateKey;
    private final String baseUrl;

    public EDPayClient(String publicKey, String privateKey, String baseUrl) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.baseUrl = (baseUrl != null) ? baseUrl : "https://api-pay.edutecka.com";
    }

    public EDPayClient(String publicKey, String privateKey) {
        this(publicKey, privateKey, "https://api-pay.edutecka.com");
    }

    public String getPublicKey() { return publicKey; }
    public String getBaseUrl() { return baseUrl; }
}
