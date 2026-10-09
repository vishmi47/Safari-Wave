package com.example.safariwave_backend.service.support;

import com.example.safariwave_backend.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;

/** Shared helpers for the paged /search endpoints of every module (spec B.4: pagination plus search/filter). */
public final class PagingSupport {

    public static final int MAX_PAGE_SIZE = 100;

    private PagingSupport() {
    }

    /** Validates page/size (400 on bad values) and builds the page request. */
    public static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("Page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        return PageRequest.of(page, size, sort);
    }

    /** Trimmed search text, or null when blank. */
    public static String text(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    /** Upper-cased status filter, or null when blank; 400 when it is not one of the allowed values. */
    public static String status(String status, List<String> allowed) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BadRequestException("Status must be one of " + allowed + ".");
        }
        return normalized;
    }
}
