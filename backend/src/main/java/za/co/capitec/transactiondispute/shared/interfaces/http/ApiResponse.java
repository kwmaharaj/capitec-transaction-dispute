package za.co.capitec.transactiondispute.shared.interfaces.http;

/**
 * keep standard response for ui to read, ie success attribute.
 * @param success
 * @param data
 * @param <T>
 */
public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }
}