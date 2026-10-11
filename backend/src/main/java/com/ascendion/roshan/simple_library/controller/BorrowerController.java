package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.dto.BorrowerResponse;
import com.ascendion.roshan.simple_library.dto.MyBorrowerResponse;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import com.ascendion.roshan.simple_library.service.BorrowerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final BorrowerProvisioningService provisioningService;
    private final BookBorrowerService bookBorrowerService;

    public BorrowerController(final BorrowerService borrowerService,
                              final BorrowerProvisioningService provisioningService,
                              final BookBorrowerService bookBorrowerService) {
        this.borrowerService = borrowerService;
        this.provisioningService = provisioningService;
        this.bookBorrowerService = bookBorrowerService;
    }

    /**
     * The caller's own Borrower, created on the first call (US-02.4), with how many items they hold
     * ("x of 3 used"). Role {@code user} only ({@code SecurityConfig}); the web app calls it after login.
     */
    @GetMapping("/me")
    public MyBorrowerResponse me(@AuthenticationPrincipal final Jwt jwt) {
        final Borrower borrower = provisioningService.getOrCreate(jwt);
        return MyBorrowerResponse.of(borrower, bookBorrowerService.borrowedCount(borrower.getId()),
                bookBorrowerService.maxItemsPerBorrower());
    }

    @PostMapping
    public BorrowerResponse registerBorrower(@Valid @RequestBody final BorrowerCreateRequest borrowerCreateRequest) {
        return BorrowerResponse.from(borrowerService.registerBorrower(borrowerCreateRequest));
    }

}
