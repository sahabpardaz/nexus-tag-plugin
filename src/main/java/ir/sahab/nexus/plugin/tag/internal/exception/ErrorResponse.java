package ir.sahab.nexus.plugin.tag.internal.exception;

/**
 * Error response returned to user as JSON.
 */
public class ErrorResponse {
    private String message;

    public ErrorResponse() {
        // Used by jackson
    }

    public static ErrorResponse of(String message) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(message);
        return response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
