package com.ascendion.roshan.simple_library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BookCreateRequest {

    @NotBlank(message = "ISBN cannot be blank")
    @Size(min = 10, max = 13, message = "ISBN must be 10 or 13 characters")
    @Pattern(regexp = "^(?=(?:[^0-9]*[0-9]){10}(?:(?:[^0-9]*[0-9]){3})?$)[\\d-]+$", message = "ISBN must be in correct format")
    private String isbn;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 254, message = "Title must be between 1 and 254 characters")
    private String title;

    @NotBlank(message = "Author cannot be blank")
    @Size(min = 1, max = 128, message = "Author must be between 1 and 128 characters")
    private String author;
}
