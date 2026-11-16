package mz.co.edpay.exceptions;

public class EDPayException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;
    private final String requestId;
    private final String retryAfter;

    public EDPayException(String message, int statusCode, String responseBody) {
        this(message, statusCode, responseBody, null, null);
    }

    public EDPayException(String message, int statusCode, String responseBody, String requestId, String retryAfter) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.requestId = requestId;
        this.retryAfter = retryAfter;
    }

    public EDPayException(String message) {
        this(message, 400, null);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRequestId() { return requestId; }

    public String getRetryAfter() { return retryAfter; }
}
