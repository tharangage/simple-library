package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real beans and database, no surrounding test transaction: proves the insert commits in its own
 * transaction (proxy applies) and that a duplicate-key failure leaves the data readable afterwards.
 */
@DataJpaTest
@Import({BorrowerInserter.class, BorrowerProvisioningService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BorrowerInserterTest {

    private static final String SUB = "kc-1";

    @Autowired
    private BorrowerInserter inserter;

    @Autowired
    private BorrowerProvisioningService service;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @AfterEach
    void cleanUp() {
        borrowerRepository.deleteAll();
    }

    private static Borrower borrower(final String sub) {
        final Borrower borrower = new Borrower();
        borrower.setFirstname("Ann");
        borrower.setKeycloakUserId(sub);
        return borrower;
    }

    private static Jwt jwt() {
        return new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", SUB, "given_name", "Ann"));
    }

    @Test
    void insert_newBorrower_isCommittedImmediately() {
        inserter.insert(borrower(SUB));

        assertThat(borrowerRepository.findByKeycloakUserId(SUB)).isPresent();
    }

    @Test
    void insert_duplicateKeycloakUserId_throwsAndKeepsFirstRow() {
        inserter.insert(borrower(SUB));

        assertThatThrownBy(() -> inserter.insert(borrower(SUB)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(borrowerRepository.count()).isEqualTo(1);
    }

    @Test
    void getOrCreate_calledTwice_createsOneBorrower() {
        final Borrower first = service.getOrCreate(jwt());
        final Borrower second = service.getOrCreate(jwt());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(borrowerRepository.count()).isEqualTo(1);
    }
}
