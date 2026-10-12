package com.ascendion.roshan.simple_library.config;

import com.ascendion.roshan.simple_library.security.CurrentUserHolder;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * US-02.4: every caller with the role {@code user} has a Borrower. Creates it on the first call.
 * Callers without the role (admin only) get no Borrower. Registered only for the paths that need it,
 * after {@code CurrentUserInterceptor} has filled {@code CurrentUserHolder}.
 */
public class BorrowerProvisioningInterceptor implements HandlerInterceptor {

    private final BorrowerProvisioningService provisioningService;

    public BorrowerProvisioningInterceptor(final BorrowerProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        // Borrow (POST) and return (DELETE) share one path; only borrow provisions.
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        CurrentUserHolder.get()
                .filter(user -> user.isUser())
                .ifPresent(user -> provisioningService.getOrCreate(user.jwt()));
        return true;
    }
}
