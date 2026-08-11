package com.fanzone.feed.controller;

import com.fanzone.feed.dto.ClubHubResponse;
import com.fanzone.feed.model.ClubEntity;
import com.fanzone.feed.repository.ClubRepository;
import com.fanzone.feed.service.ClubHubService;
import com.fanzone.common.exceptions.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Club Hub and club theme endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class ClubHubController {

    private final ClubHubService clubHubService;
    private final ClubRepository clubRepository;

    public ClubHubController(ClubHubService clubHubService, ClubRepository clubRepository) {
        this.clubHubService = clubHubService;
        this.clubRepository = clubRepository;
    }

    /**
     * Get the Club Hub — aggregated club data with categorized post sections.
     */
    @GetMapping("/club-hubs/{clubId}")
    public ResponseEntity<ClubHubResponse> getClubHub(@PathVariable UUID clubId) {
        ClubHubResponse hub = clubHubService.getClubHub(clubId);
        return ResponseEntity.ok(hub);
    }

    /**
     * Get club theme colors for client-side theming.
     */
    @GetMapping("/clubs/{clubId}/theme")
    public ResponseEntity<Map<String, String>> getClubTheme(@PathVariable UUID clubId) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("CLUB_NOT_FOUND", "Club not found"));

        Map<String, String> theme = Map.of(
                "primaryColor", club.getPrimaryColor() != null ? club.getPrimaryColor() : "#1a1a2e",
                "secondaryColor", club.getSecondaryColor() != null ? club.getSecondaryColor() : "#16213e"
        );

        return ResponseEntity.ok(theme);
    }
}
