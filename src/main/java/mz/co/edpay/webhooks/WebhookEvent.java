package mz.co.edpay.webhooks;

/** Standard partner payment webhook envelope. A payment-link ID is absent/null for non-link payments. */
public record WebhookEvent(
    String id,
    String object,
    String type,
    String created_at,
    PaymentData data
) {
    public record PaymentData(
        String object,
        String id,
        String reference,
        String payment_link_id,
        String trans_id,
        String method,
        String status,
        Long amount_minor,
        String currency,
        Long platform_fee_minor,
        Long receiver_net_minor,
        String created_at,
        String reconciliation_status,
        String reconciliation_code
    ) {}
}
