package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Book wall at the public API: an assumed-claims request is only served for the ASSUMED book.
 * A missing or different book header returns 404, never 403, so the response cannot confirm that
 * an assumed claim exists in another book (SPEC §3.6.2, WALL-06).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BookScopeFilter extends OncePerRequestFilter {
    public static final String BOOK_HEADER = "X-EIA-Book";
    public static final String ASSUMED_BOOK = "ASSUMED";

    private final ObjectMapper objectMapper;

    public BookScopeFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")
                && !ASSUMED_BOOK.equals(request.getHeader(BOOK_HEADER))) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiError.bookScope(request.getRequestURI()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
