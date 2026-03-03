package com.orlandoprestige.orlandoproject.shared.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for soft-deletable entities.
 * <p>
 * Entities extending this class will have a {@code deleted} boolean column
 * that is set to {@code true} instead of physically removing the row.
 * Each entity must also declare {@code @SQLDelete} and {@code @SQLRestriction}
 * annotations to intercept deletes and filter queries automatically.
 */
@MappedSuperclass
public abstract class SoftDeletableEntity {

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean not null default false")
    @JsonIgnore
    private boolean deleted = false;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
