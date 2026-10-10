package com.ascendion.roshan.simple_library.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Basic abuse protection for the public registration endpoint: a real request is well under 1 KB,
 * so anything declaring more than 4 KB is refused with 413 before the body is read.
 * (Requests without Content-Length are still bounded by the field size limits in RegistrationRequest.)
 * Rate limiting is not done here: do it at the ingress / reverse proxy.
 */
@Component
public class RegistrationSizeLimitFilter extends OncePerRequestFilter {

    static final long MAX_BYTES = 4096;
    private static final String PATH = "/apis/v1/registrations";

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large");
            return;
        }
        chain.doFilter(request, response);
    }
}
