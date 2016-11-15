package ch.coredump.watertemp.rest.models;

public class ApiError {
    private int statusCode;
    private String message;

    public ApiError(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ApiError: HTTP " + this.statusCode + ": " + this.message;
    }
}
