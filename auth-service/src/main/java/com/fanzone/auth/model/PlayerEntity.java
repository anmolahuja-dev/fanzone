package com.fanzone.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "club_id")
    private UUID clubId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(length = 30)
    private String position;
}
