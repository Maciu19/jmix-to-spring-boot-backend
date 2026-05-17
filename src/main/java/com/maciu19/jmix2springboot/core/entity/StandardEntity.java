package com.maciu19.jmix2springboot.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;

@MappedSuperclass
@SoftDelete
public class StandardEntity extends AuditableEntity {

    // TODO: This isn't finished, because i cannot make this work

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "deleted_by")
    private String deletedBy;

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Instant getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Instant deletedDate) {
        this.deletedDate = deletedDate;
    }
}
