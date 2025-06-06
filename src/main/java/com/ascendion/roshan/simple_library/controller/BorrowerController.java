package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.service.BorrowerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(final BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    public Borrower registerBorrower(@Valid @RequestBody BorrowerCreateRequest borrowerCreateRequest) {

        return borrowerService.registerBorrower(borrowerCreateRequest);
    }

}
