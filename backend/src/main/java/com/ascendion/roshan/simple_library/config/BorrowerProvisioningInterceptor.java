package com.ascendion.roshan.simple_library.config;

import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * US-02.4: every caller with the role {@code user} has a Borrower. Creates it on the first call and
 * exposes it to the controller as the request attribute {@link #CURRENT_BORROWER} (borrow checks it).
 * Callers without the role (admin only) get no Borrower. Registered only for the paths that need it.
 */
public class BorrowerProvisioningInterceptor implements HandlerInterceptor {

    public static final String CURRENT_BORROWER = "currentBorrower";

    private final BorrowerProvisioningService provisioningService;

    public BorrowerProvisioningInterceptor(final BorrowerProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof Jwt jwt
                && authentication.getAuthorities().stream().anyMatch(a -> SecurityConfig.ROLE_USER.equals(a.getAuthority()))) {
            request.setAttribute(CURRENT_BORROWER, provisioningService.getOrCreate(jwt));
        }
        return true;
    }
}
