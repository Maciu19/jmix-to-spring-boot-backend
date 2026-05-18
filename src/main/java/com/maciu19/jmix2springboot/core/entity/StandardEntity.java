package com.maciu19.jmix2springboot.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;

@MappedSuperclass
@SoftDelete(columnName = "deleted_date", converter = DeletedAtConverter.class)
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
