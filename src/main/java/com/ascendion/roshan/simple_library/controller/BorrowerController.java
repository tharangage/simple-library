package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.model.Borrower;
import com.ascendion.roshan.simple_library.service.BorrowerService;
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
    public Borrower createBorrower(@RequestBody Borrower borrower) {

        //TODO
        borrower.setId("borrower-123");
        return borrower;
    }

}
