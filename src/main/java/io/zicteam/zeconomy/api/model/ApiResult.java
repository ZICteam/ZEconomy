package io.zicteam.zeconomy.api.model;

public record ApiResult<T>(boolean success, ApiErrorCode errorCode, String message, T value) {
    public static <T> ApiResult<T> success(T value) {
        return new ApiResult<>(true, ApiErrorCode.NONE, "", value);
    }

    public static <T> ApiResult<T> failure(ApiErrorCode errorCode, String message) {
        return new ApiResult<>(false, errorCode, message == null ? "" : message, null);
    }
}
