package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Borrower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BorrowerRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    private Borrower save(final String email, final String keycloakUserId) {
        final Borrower borrower = new Borrower();
        borrower.setFirstname("Ann");
        borrower.setLastname("Lee");
        borrower.setEmail(email);
        borrower.setKeycloakUserId(keycloakUserId);
        return borrowerRepository.saveAndFlush(borrower);
    }

    @Test
    void saveAndFlush_duplicateKeycloakUserId_throwsDataIntegrityViolation() {
        save("a@example.com", "kc-1");

        assertThatThrownBy(() -> save("b@example.com", "kc-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_severalNullKeycloakUserIdsAndEmails_areAllowed() {
        save(null, null);
        save(null, null);

        assertThat(borrowerRepository.count()).isEqualTo(2);
    }

    @Test
    void findByKeycloakUserId_knownId_returnsBorrower() {
        final Borrower saved = save("a@example.com", "kc-1");

        assertThat(borrowerRepository.findByKeycloakUserId("kc-1")).get().extracting(Borrower::getId).isEqualTo(saved.getId());
        assertThat(borrowerRepository.findByKeycloakUserId("other")).isEmpty();
    }

    @Test
    void findFirstByEmailIgnoreCase_differentCase_returnsBorrower() {
        save("ann@example.com", null);

        assertThat(borrowerRepository.findFirstByEmailIgnoreCase("ANN@Example.com")).isPresent();
    }

    @Test
    void linkIfUnlinked_unlinkedRow_setsKeycloakUserId() {
        final Borrower legacy = save("a@example.com", null);

        assertThat(borrowerRepository.linkIfUnlinked(legacy.getId(), "kc-1")).isEqualTo(1);
        assertThat(borrowerRepository.findById(legacy.getId())).get()
                .extracting(Borrower::getKeycloakUserId).isEqualTo("kc-1");
    }

    @Test
    void linkIfUnlinked_alreadyLinkedRow_changesNothing() {
        final Borrower linked = save("a@example.com", "kc-1");

        assertThat(borrowerRepository.linkIfUnlinked(linked.getId(), "kc-2")).isZero();
        assertThat(borrowerRepository.findById(linked.getId())).get()
                .extracting(Borrower::getKeycloakUserId).isEqualTo("kc-1");
    }

    @Test
    void linkIfUnlinked_secondCallForSameRow_firstLinkWins() {
        final Borrower legacy = save("a@example.com", null);

        assertThat(borrowerRepository.linkIfUnlinked(legacy.getId(), "kc-1")).isEqualTo(1);
        assertThat(borrowerRepository.linkIfUnlinked(legacy.getId(), "kc-2")).isZero();
        assertThat(borrowerRepository.findByKeycloakUserId("kc-1")).isPresent();
        assertThat(borrowerRepository.findByKeycloakUserId("kc-2")).isEmpty();
    }
}
