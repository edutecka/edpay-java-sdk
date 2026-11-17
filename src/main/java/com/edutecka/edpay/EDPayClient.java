package com.edutecka.edpay;

public class EDPayClient {
    private final String publicKey;
    private final String privateKey;
    private final String baseUrl;
    private final mz.co.edpay.EDPayClient delegate;

    public EDPayClient(String publicKey, String privateKey, String baseUrl) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.baseUrl = (baseUrl != null) ? baseUrl : "https://api-pay.edutecka.com";
        this.delegate = new mz.co.edpay.EDPayClient.Builder()
            .publicKey(publicKey)
            .privateKey(privateKey)
            .environment(mz.co.edpay.EDPayClient.Environment.PRODUCTION)
            .baseUrl(this.baseUrl)
            .build();
    }

    public EDPayClient(String publicKey, String privateKey) {
        this(publicKey, privateKey, "https://api-pay.edutecka.com");
    }

    public String getPublicKey() { return publicKey; }
    public String getBaseUrl() { return baseUrl; }

    public mz.co.edpay.resources.PaymentsResource payments() { return delegate.payments(); }
    public mz.co.edpay.resources.CustomersResource customers() { return delegate.customers(); }
    public mz.co.edpay.resources.InvoicesResource invoices() { return delegate.invoices(); }
    public mz.co.edpay.resources.SubscriptionsResource subscriptions() { return delegate.subscriptions(); }
    public mz.co.edpay.resources.WalletsResource wallets() { return delegate.wallets(); }
    public mz.co.edpay.resources.FxResource fx() { return delegate.fx(); }
    public mz.co.edpay.resources.WebhooksResource webhooks() { return delegate.webhooks(); }
    public mz.co.edpay.resources.PublicResource publicCatalog() { return delegate.publicCatalog(); }
}
