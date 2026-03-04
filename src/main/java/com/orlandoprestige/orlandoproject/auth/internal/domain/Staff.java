package com.orlandoprestige.orlandoproject.auth.internal.domain;

import com.orlandoprestige.orlandoproject.shared.domain.entities.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "staff", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@SQLDelete(sql = "UPDATE staff SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Staff extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "super_admin", nullable = false, columnDefinition = "boolean not null default false")
    private boolean superAdmin = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_permissions", joinColumns = @JoinColumn(name = "staff_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Returns true if this staff member has the given permission,
     * either explicitly or implicitly (super admins have all permissions).
     */
    public boolean hasPermission(Permission permission) {
        return superAdmin || permissions.contains(permission);
    }
}

