package io.zicteam.zeconomy.api.model;

public record ApiResult<T>(boolean success, ApiErrorCode errorCode, String message, T value) {
}
