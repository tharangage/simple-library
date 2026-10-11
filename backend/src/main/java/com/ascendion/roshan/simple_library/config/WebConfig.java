package com.ascendion.roshan.simple_library.config;

import com.ascendion.roshan.simple_library.security.CurrentUserInterceptor;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BorrowerProvisioningService provisioningService;

    public WebConfig(final BorrowerProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    /**
     * 1. {@link CurrentUserInterceptor}: token details of the caller for every API call that has a token.
     * 2. {@link BorrowerProvisioningInterceptor}: creates the caller's Borrower on borrow. {@code GET /me} creates it
     *    itself because it returns it. Return is an admin action and needs no Borrower for the caller.
     */
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new CurrentUserInterceptor())
                .addPathPatterns("/apis/**")
                .order(0);
        registry.addInterceptor(new BorrowerProvisioningInterceptor(provisioningService))
                .addPathPatterns("/apis/v1/borrowers/*/books")
                .order(1);
    }
}
