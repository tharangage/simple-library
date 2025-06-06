package com.ascendion.roshan.simple_library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BorrowerCreateRequest {

    @NotBlank(message = "Firstname cannot be blank")
    @Size(min = 1, max = 64, message = "Firstname must be between 1 and 64 characters")
    private String firstname;

    @NotBlank(message = "Lastname cannot be blank")
    @Size(min = 1, max = 64, message = "Lastname must be between 1 and 64 characters")
    private String lastname;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;
}
