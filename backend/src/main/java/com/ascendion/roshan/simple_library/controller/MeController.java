package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerProfile;
import com.ascendion.roshan.simple_library.dto.UserProfileResponse;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.security.CurrentUser;
import com.ascendion.roshan.simple_library.security.CurrentUserHolder;
import com.ascendion.roshan.simple_library.service.BorrowedBookService;
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
    private final BorrowedBookService borrowedBookService;

    public MeController(final BorrowerProvisioningService provisioningService,
                        final BorrowedBookService borrowedBookService) {
        this.provisioningService = provisioningService;
        this.borrowedBookService = borrowedBookService;
    }

    @GetMapping
    public UserProfileResponse me() {
        final CurrentUser caller = CurrentUserHolder.require();
        BorrowerProfile borrower = null;
        String mobile = null;
        if (caller.isUser()) {
            final Borrower entity = provisioningService.getOrCreate(caller.jwt());
            mobile = entity.getMobile();
            borrower = BorrowerProfile.of(entity, borrowedBookService.borrowedCount(entity.getId()),
                    borrowedBookService.maxItemsPerBorrower());
        }
        return UserProfileResponse.of(caller.jwt(), caller.roles(), mobile, borrower);
    }
}
