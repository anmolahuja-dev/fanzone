package com.fanzone.matchthread.model;

import com.fanzone.common.enums.MatchPhase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "home_club_id", nullable = false)
    private UUID homeClubId;

    @Column(name = "away_club_id", nullable = false)
    private UUID awayClubId;

    @Column(name = "home_score")
    private int homeScore = 0;

    @Column(name = "away_score")
    private int awayScore = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchPhase status = MatchPhase.SCHEDULED;

    @Column(name = "kick_off_time")
    private Instant kickOffTime;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
