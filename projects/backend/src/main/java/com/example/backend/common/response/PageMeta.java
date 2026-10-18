package com.example.backend.common.response;

public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {
}
