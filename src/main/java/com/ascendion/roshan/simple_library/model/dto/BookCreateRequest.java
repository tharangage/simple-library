package com.ascendion.roshan.simple_library.model.dto;

import lombok.Getter;

@Getter
public class BookCreateRequest {

    private String isbn;
    private String title;
    private String author;
}
