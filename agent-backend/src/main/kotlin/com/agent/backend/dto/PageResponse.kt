package com.agent.backend.dto

import org.springframework.data.domain.Page

/**
 * Generic paginated response wrapper for REST APIs.
 * Provides a stable JSON structure for paginated data.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: PageMetadata
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                page = PageMetadata(
                    number = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    first = page.isFirst,
                    last = page.isLast,
                    empty = page.isEmpty
                )
            )
        }

        fun <T, R> from(page: Page<T>, transform: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = page.content.map(transform),
                page = PageMetadata(
                    number = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    first = page.isFirst,
                    last = page.isLast,
                    empty = page.isEmpty
                )
            )
        }
    }
}

/**
 * Pagination metadata.
 */
data class PageMetadata(
    val number: Int,          // Current page number (0-based)
    val size: Int,            // Page size
    val totalElements: Long,  // Total number of elements
    val totalPages: Int,      // Total number of pages
    val first: Boolean,       // Is this the first page?
    val last: Boolean,        // Is this the last page?
    val empty: Boolean        // Is the page empty?
)
