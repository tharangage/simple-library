package com.ascendion.roshan.simple_library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /apis/v1/registrations}. The size limits also bound the request size.
 * Rules match the React form (assumption A-08: format checks only).
 */
public record RegistrationRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 64, message = "First name must be at most 64 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 64, message = "Last name must be at most 64 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must be at most 254 characters")
        @Email(message = "Email must be valid")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must be valid")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Mobile number must be in international format, e.g. +14155550100")
        String mobile,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be 8 to 128 characters")
        String password) {

    /** Never print the password or mobile number (they must not reach logs). */
    @Override
    public String toString() {
        return "RegistrationRequest[redacted]";
    }
}
