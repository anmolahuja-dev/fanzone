package com.fanzone.feed.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only club entity for Club Hub aggregation.
 */
@Entity
@Table(name = "clubs")
@Immutable
public class ClubEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    protected ClubEntity() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
}
