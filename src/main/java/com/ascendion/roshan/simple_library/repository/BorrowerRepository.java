package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, String> {
}
