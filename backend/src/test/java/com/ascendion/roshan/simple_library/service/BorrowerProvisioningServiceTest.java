package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerProvisioningServiceTest {

    private static final String SUB = "11111111-2222-3333-4444-555555555555";

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BorrowerInserter inserter;

    @InjectMocks
    private BorrowerProvisioningService service;

    private static Jwt jwt(final Map<String, Object> extraClaims) {
        final Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("sub", SUB);
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }

    private static Borrower borrowerWithKeycloakId(final String keycloakUserId) {
        final Borrower borrower = new Borrower();
        borrower.setId("b-1");
        borrower.setKeycloakUserId(keycloakUserId);
        return borrower;
    }

    @Test
    void getOrCreate_existingBorrower_returnsItWithoutChanges() {
        final Borrower existing = borrowerWithKeycloakId(SUB);
        existing.setFirstname("Stored");
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.of(existing));

        final Borrower result = service.getOrCreate(jwt(Map.of("given_name", "Changed", "email", "x@y.com")));

        assertThat(result).isSameAs(existing);
        assertThat(result.getFirstname()).isEqualTo("Stored");
        verifyNoInteractions(inserter);
    }

    @Test
    void getOrCreate_noBorrower_createsFromClaims() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.empty());
        when(inserter.insert(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        final Borrower result = service.getOrCreate(jwt(Map.of(
                "given_name", "Ann", "family_name", "Lee", "email", " Ann@Example.com ", "email_verified", true)));

        assertThat(result.getFirstname()).isEqualTo("Ann");
        assertThat(result.getLastname()).isEqualTo("Lee");
        assertThat(result.getEmail()).isEqualTo("ann@example.com");
        assertThat(result.getKeycloakUserId()).isEqualTo(SUB);
        assertThat(result.getMobile()).isNull();
    }

    @Test
    void getOrCreate_missingNames_fallsBackToPreferredUsername() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(inserter.insert(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        final Borrower result = service.getOrCreate(jwt(Map.of("preferred_username", "ann42")));

        assertThat(result.getFirstname()).isEqualTo("ann42");
        assertThat(result.getLastname()).isEqualTo("ann42");
    }

    @Test
    void getOrCreate_unverifiedEmail_createsWithoutStoringEmail() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.empty());
        when(inserter.insert(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        final Borrower result = service.getOrCreate(jwt(Map.of("given_name", "Ann", "email", "ann@example.com")));

        assertThat(result.getEmail()).isNull();
        assertThat(result.getFirstname()).isEqualTo("Ann");
    }

    @Test
    void getOrCreate_noNamesAndNoEmail_throwsIllegalState() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreate(jwt(Map.of())))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(inserter);
    }

    @Test
    void getOrCreate_missingEmail_createsWithNullEmail() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(inserter.insert(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        final Borrower result = service.getOrCreate(jwt(Map.of("given_name", "Ann", "family_name", "Lee")));

        assertThat(result.getEmail()).isNull();
        verify(borrowerRepository, never()).findFirstByEmailIgnoreCase(any());
    }

    @Test
    void getOrCreate_concurrentFirstCall_returnsRowInsertedByOtherRequest() {
        final Borrower winner = borrowerWithKeycloakId(SUB);
        when(borrowerRepository.findByKeycloakUserId(SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(inserter.insert(any(Borrower.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(service.getOrCreate(jwt(Map.of("given_name", "Ann")))).isSameAs(winner);
    }

    @Test
    void getOrCreate_duplicateKeyButNoRowForSub_throwsEmailConflict() {
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(inserter.insert(any(Borrower.class))).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> service.getOrCreate(jwt(Map.of("given_name", "Ann"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email already registered to another account");
    }

    @Test
    void getOrCreate_legacyBorrowerAndVerifiedEmail_linksBySub() {
        final Borrower legacy = borrowerWithKeycloakId(null);
        final Borrower linked = borrowerWithKeycloakId(SUB);
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty(), Optional.of(linked));
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.of(legacy));
        when(borrowerRepository.linkIfUnlinked("b-1", SUB)).thenReturn(1);

        final Borrower result = service.getOrCreate(jwt(Map.of("email", "ann@example.com", "email_verified", true)));

        assertThat(result).isSameAs(linked);
        verifyNoInteractions(inserter);
    }

    @Test
    void getOrCreate_legacyLinkedConcurrentlyBySameUser_returnsLinkedRow() {
        final Borrower legacy = borrowerWithKeycloakId(null);
        final Borrower linked = borrowerWithKeycloakId(SUB);
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty(), Optional.of(linked));
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.of(legacy));
        when(borrowerRepository.linkIfUnlinked("b-1", SUB)).thenReturn(0);

        assertThat(service.getOrCreate(jwt(Map.of("email", "ann@example.com", "email_verified", true))))
                .isSameAs(linked);
    }

    @Test
    void getOrCreate_legacyLinkedFirstByAnotherUser_throwsIllegalState() {
        final Borrower legacy = borrowerWithKeycloakId(null);
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.of(legacy));
        when(borrowerRepository.linkIfUnlinked("b-1", SUB)).thenReturn(0);

        assertThatThrownBy(() -> service.getOrCreate(jwt(Map.of("email", "ann@example.com", "email_verified", true))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email already registered to another account");
    }

    @Test
    void getOrCreate_legacyBorrowerAndUnverifiedEmail_throwsIllegalState() {
        final Borrower legacy = borrowerWithKeycloakId(null);
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> service.getOrCreate(jwt(Map.of("email", "ann@example.com"))))
                .isInstanceOf(IllegalStateException.class);
        verify(borrowerRepository, never()).linkIfUnlinked(any(), any());
    }

    @Test
    void getOrCreate_emailBelongsToOtherKeycloakUser_throwsIllegalState() {
        final Borrower other = borrowerWithKeycloakId("someone-else");
        when(borrowerRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(borrowerRepository.findFirstByEmailIgnoreCase("ann@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getOrCreate(jwt(Map.of("email", "ann@example.com", "email_verified", true))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(other.getKeycloakUserId()).isEqualTo("someone-else");
    }
}
