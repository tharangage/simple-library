package com.ascendion.roshan.simple_library.config;

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

    /** Provision on {@code /me} and on borrow only. Return is an admin action and needs no Borrower for the caller. */
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new BorrowerProvisioningInterceptor(provisioningService))
                .addPathPatterns("/apis/v1/me", "/apis/v1/me/**", "/apis/v1/borrowers/*/books");
    }
}
