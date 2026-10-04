package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BorrowerService borrowerService;

    private BorrowerCreateRequest request;
    private Borrower mapped;

    @BeforeEach
    void setUp() {
        request = new BorrowerCreateRequest();
        ReflectionTestUtils.setField(request, "firstname", "Ann");
        ReflectionTestUtils.setField(request, "lastname", "Lee");
        ReflectionTestUtils.setField(request, "email", "ann@example.com");
        mapped = new Borrower();
        mapped.setEmail("ann@example.com");
    }

    @Test
    void registerBorrower_newEmail_savesAndReturnsSavedBorrower() {
        final Borrower saved = new Borrower();
        saved.setId("generated-id");
        when(modelMapper.map(request, Borrower.class)).thenReturn(mapped);
        when(borrowerRepository.saveAndFlush(mapped)).thenReturn(saved);

        final Borrower result = borrowerService.registerBorrower(request);

        assertThat(result).isSameAs(saved);
        verify(borrowerRepository).saveAndFlush(mapped);
    }

    @Test
    void registerBorrower_duplicateEmail_throwsIllegalStateWithCause() {
        final DataIntegrityViolationException duplicate = new DataIntegrityViolationException("idx_borrower_email");
        when(modelMapper.map(request, Borrower.class)).thenReturn(mapped);
        when(borrowerRepository.saveAndFlush(mapped)).thenThrow(duplicate);

        assertThatThrownBy(() -> borrowerService.registerBorrower(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email already exists")
                .hasCause(duplicate);
    }
}
