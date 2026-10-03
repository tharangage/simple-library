package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
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
            return borrowerRepository.save(borrowerNew);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Email already exists", ex);
        }
    }
}
