package com.drivehub.pagos.common.response;

public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {
}
