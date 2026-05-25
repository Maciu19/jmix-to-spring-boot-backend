package com.maciu19.jmix2springboot.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SoftDelete;

import java.time.Instant;

@MappedSuperclass
public class StandardEntity extends AuditableEntity {

    @Column(name = "deleted_date")
    private Instant deletedDate;

    public Instant getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Instant deletedDate) {
        this.deletedDate = deletedDate;
    }
}
