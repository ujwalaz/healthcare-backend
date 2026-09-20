package com.healthcare.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Central pagination guard for all public list endpoints. */
public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 30;

    private PaginationUtils() {
    }

    public static Pageable of(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
