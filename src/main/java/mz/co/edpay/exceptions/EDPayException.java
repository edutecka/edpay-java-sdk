package mz.co.edpay.exceptions;

public class EDPayException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public EDPayException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
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
}
