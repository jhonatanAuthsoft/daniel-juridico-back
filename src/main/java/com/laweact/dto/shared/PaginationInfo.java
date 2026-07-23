package com.laweact.dto.shared;

public record PaginationInfo(
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PaginationInfo of(int limit, int offset, long totalElements) {
        int size = limit > 0 ? limit : (int) totalElements;
        int page = limit > 0 ? (offset / limit) + 1 : 1;
        int totalPages = limit > 0 ? (int) Math.ceil((double) totalElements / limit) : 1;
        return new PaginationInfo(page, size, totalElements, totalPages);
    }
}
