package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.RegistrationRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.EmailAlreadyRegisteredException;
import com.ascendion.roshan.simple_library.exception.KeycloakAdminException;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final RegistrationRequest REQUEST = new RegistrationRequest(
            " Ann ", "Lee", "Ann@Example.com ", "+14155550100", "s3cret-pass");

    @Mock
    private KeycloakAdminClient keycloak;

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private RegistrationService service;

    @Test
    void register_newEmail_createsKeycloakUserThenBorrowerLinkedToIt() {
        when(borrowerRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(false);
        when(keycloak.createUser("ann@example.com", "Ann", "Lee", "+14155550100", "s3cret-pass"))
                .thenReturn("kc-123");
        when(borrowerRepository.saveAndFlush(any(Borrower.class))).thenAnswer(i -> i.getArgument(0));

        final Borrower result = service.register(REQUEST);

        final ArgumentCaptor<Borrower> saved = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getKeycloakUserId()).isEqualTo("kc-123");
        assertThat(saved.getValue().getEmail()).isEqualTo("ann@example.com");
        assertThat(saved.getValue().getFirstname()).isEqualTo("Ann");
        assertThat(saved.getValue().getMobile()).isEqualTo("+14155550100");
        assertThat(result.getKeycloakUserId()).isEqualTo("kc-123");
        verify(keycloak, never()).deleteUser(any());
    }

    @Test
    void register_emailAlreadyInDatabase_throws409AndNeverCallsKeycloak() {
        when(borrowerRepository.existsByEmailIgnoreCase("ann@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(REQUEST)).isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(keycloak);
    }

    @Test
    void register_emailAlreadyInKeycloak_throws409AndSavesNothing() {
        when(keycloak.createUser(any(), any(), any(), any(), any())).thenThrow(new EmailAlreadyRegisteredException());

        assertThatThrownBy(() -> service.register(REQUEST)).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(borrowerRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_saveFails_deletesKeycloakUserAndRethrows() {
        when(keycloak.createUser(any(), any(), any(), any(), any())).thenReturn("kc-123");
        when(borrowerRepository.saveAndFlush(any())).thenThrow(new IllegalArgumentException("db down"));

        assertThatThrownBy(() -> service.register(REQUEST)).isInstanceOf(IllegalArgumentException.class);

        verify(keycloak).deleteUser("kc-123");
    }

    @Test
    void register_duplicateEmailRaceOnInsert_deletesKeycloakUserAndThrows409() {
        when(keycloak.createUser(any(), any(), any(), any(), any())).thenReturn("kc-123");
        when(borrowerRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.register(REQUEST)).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(keycloak).deleteUser("kc-123");
    }

    @Test
    void register_saveFailsAndCompensationFails_stillReportsTheOriginalError() {
        when(keycloak.createUser(any(), any(), any(), any(), any())).thenReturn("kc-123");
        when(borrowerRepository.saveAndFlush(any())).thenThrow(new IllegalArgumentException("db down"));
        doThrow(new KeycloakAdminException("delete failed")).when(keycloak).deleteUser("kc-123");

        assertThatThrownBy(() -> service.register(REQUEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("db down");
    }
}
