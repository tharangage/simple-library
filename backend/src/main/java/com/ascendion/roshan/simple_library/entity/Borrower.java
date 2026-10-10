package com.ascendion.roshan.simple_library.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_borrower_email", columnList = "email", unique = true),
        // Nullable: rows created before self-registration have no Keycloak user. Several NULLs are allowed.
        @Index(name = "idx_borrower_keycloak_user_id", columnList = "keycloakUserId", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
// Not @Data: Lombok equals/hashCode over mutable fields and a generated id breaks
// JPA entities in Sets and across persist; identity equality is used instead.
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String firstname;
    private String lastname;
    private String email;

    /** Mobile number in E.164 format. Optional for older rows; only the owner may see it. */
    @ToString.Exclude
    private String mobile;

    /** The {@code sub} of the borrower's Keycloak user (assumption A-01: one user, one borrower). */
    private String keycloakUserId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
