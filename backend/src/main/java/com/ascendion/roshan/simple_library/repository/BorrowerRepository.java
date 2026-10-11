package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, String> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Borrower> findByKeycloakUserId(String keycloakUserId);

    Optional<Borrower> findFirstByEmailIgnoreCase(String email);

    /**
     * Atomically sets the Keycloak id on a row that has none. Returns 0 when another request linked it first,
     * so two users can never overwrite each other's link.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Borrower b set b.keycloakUserId = :sub where b.id = :id and b.keycloakUserId is null")
    int linkIfUnlinked(@Param("id") String id, @Param("sub") String sub);
}
