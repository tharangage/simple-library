package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.RegistrationRequest;
import com.ascendion.roshan.simple_library.dto.RegistrationResponse;
import com.ascendion.roshan.simple_library.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Public endpoint: a visitor creates their own account. */
@RestController
@RequestMapping("/apis/v1/registrations")
public class RegistrationController {

    // The "current user" endpoint is planned (E01); the Location header points to it already.
    private static final URI ME = URI.create("/apis/v1/me");

    private final RegistrationService registrationService;

    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody final RegistrationRequest request) {
        return ResponseEntity.created(ME)
                .body(RegistrationResponse.from(registrationService.register(request)));
    }
}
