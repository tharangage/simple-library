package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.springframework.stereotype.Service;

@Service
public class BorrowerService {
    private final BorrowerRepository borrowerRepository;

    public BorrowerService(final BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }
}
