package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class BorrowerService {
    private final BorrowerRepository borrowerRepository;
    private final ModelMapper modelMapper;

    public BorrowerService(final BorrowerRepository borrowerRepository,
                           final ModelMapper modelMapper) {
        this.borrowerRepository = borrowerRepository;
        this.modelMapper = modelMapper;
    }

    public Borrower registerBorrower(final BorrowerCreateRequest borrowerCreateRequest) {
        final Borrower borrowerNew = modelMapper.map(borrowerCreateRequest, Borrower.class);
        try {
            // saveAndFlush forces the INSERT now, so a unique-email violation is raised
            // inside this try block even if the method later runs in a wider transaction.
            return borrowerRepository.saveAndFlush(borrowerNew);
        } catch (DataIntegrityViolationException ex) {
            // The only unique constraint on Borrower is idx_borrower_email.
            throw new IllegalStateException("Email already exists", ex);
        }
    }
}
