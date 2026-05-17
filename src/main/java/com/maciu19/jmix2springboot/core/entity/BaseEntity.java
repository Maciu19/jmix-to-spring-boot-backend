package com.maciu19.jmix2springboot.core.entity;

import jakarta.persistence.*;

import java.util.UUID;

@MappedSuperclass
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Version
    @Column(name = "version")
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public UUID getId() {
        return id;
    }
}
