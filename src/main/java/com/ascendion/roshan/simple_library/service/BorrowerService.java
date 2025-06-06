package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.model.Borrower;
import com.ascendion.roshan.simple_library.model.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.modelmapper.ModelMapper;
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
        return borrowerRepository.save(borrowerNew);
    }
}
