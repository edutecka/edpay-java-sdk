package mz.co.edpay.webhooks;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verifies EDPay timestamped Payment-Signature webhook headers. */
public final class WebhookVerifier {
    public static final Duration DEFAULT_TOLERANCE = Duration.ofMinutes(5);
    private static final Pattern SIGNATURE_HEADER = Pattern.compile("^t=([0-9]+),v1=([0-9a-fA-F]{64})$");

    private WebhookVerifier() {}

    public static boolean verify(String payload, String signatureHeader, String secret) {
        return payload != null && verify(payload.getBytes(StandardCharsets.UTF_8), signatureHeader, secret);
    }

    public static boolean verify(byte[] rawBody, String signatureHeader, String secret) {
        return verify(rawBody, signatureHeader, secret, Instant.now(), DEFAULT_TOLERANCE);
    }

    public static boolean verify(byte[] rawBody, String signatureHeader, String secret, Duration tolerance) {
        return verify(rawBody, signatureHeader, secret, Instant.now(), tolerance);
    }

    /** The explicit clock overload supports applications with a synchronized clock and deterministic validation. */
    public static boolean verify(byte[] rawBody, String signatureHeader, String secret, Instant now, Duration tolerance) {
        if (rawBody == null || signatureHeader == null || secret == null || secret.isEmpty() || now == null || tolerance == null || tolerance.isNegative()) {
            return false;
        }

        Matcher matcher = SIGNATURE_HEADER.matcher(signatureHeader.trim());
        if (!matcher.matches()) {
            return false;
        }

        try {
            Instant timestamp = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
            if (Duration.between(timestamp, now).abs().compareTo(tolerance) > 0) {
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(matcher.group(1).getBytes(StandardCharsets.US_ASCII));
            mac.update((byte) '.');
            byte[] expected = mac.doFinal(rawBody);
            byte[] supplied = HexFormat.of().parseHex(matcher.group(2));
            return MessageDigest.isEqual(expected, supplied);
        } catch (Exception exception) {
            return false;
        }
    }
}
