package com.example.backend.common.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        T data,
        String message,
        Instant timestamp,
        String error,
        PageMeta meta
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", data, message, Instant.now(), null, null);
    }

    public static <T> ApiResponse<List<T>> successList(List<T> data, String message, PageMeta meta) {
        return new ApiResponse<>("success", data, message, Instant.now(), null, meta);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>("failed", null, message, Instant.now(), errorCode, null);
    }
}
