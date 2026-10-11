package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerProfile;
import com.ascendion.roshan.simple_library.dto.UserProfileResponse;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.security.CurrentUser;
import com.ascendion.roshan.simple_library.security.CurrentUserHolder;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own profile, the same call for every user: identity from the token, plus a {@code borrower}
 * section for a caller with the role {@code user}. That caller's Borrower is created here on the first call
 * (US-02.4). The web app calls this once after login.
 */
@RestController
@RequestMapping("/apis/v1/me")
public class MeController {

    private final BorrowerProvisioningService provisioningService;
    private final BookBorrowerService bookBorrowerService;

    public MeController(final BorrowerProvisioningService provisioningService,
                        final BookBorrowerService bookBorrowerService) {
        this.provisioningService = provisioningService;
        this.bookBorrowerService = bookBorrowerService;
    }

    @GetMapping
    public UserProfileResponse me() {
        final CurrentUser caller = CurrentUserHolder.require();
        BorrowerProfile borrower = null;
        if (caller.isUser()) {
            final Borrower entity = provisioningService.getOrCreate(caller.jwt());
            borrower = BorrowerProfile.of(entity, bookBorrowerService.borrowedCount(entity.getId()),
                    bookBorrowerService.maxItemsPerBorrower());
        }
        return UserProfileResponse.of(caller.jwt(), borrower);
    }
}
