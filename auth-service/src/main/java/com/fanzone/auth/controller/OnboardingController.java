package com.fanzone.auth.controller;

import com.fanzone.auth.dto.ClubDto;
import com.fanzone.auth.dto.PlayerDto;
import com.fanzone.auth.dto.SetClubRequest;
import com.fanzone.auth.dto.SetInterestsRequest;
import com.fanzone.auth.dto.SetPlayersRequest;
import com.fanzone.auth.service.OnboardingService;
import com.fanzone.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/clubs")
    public ResponseEntity<List<ClubDto>> searchClubs(
            @RequestParam(required = false) String search) {
        List<ClubDto> clubs = onboardingService.searchClubs(search);
        return ResponseEntity.ok(clubs);
    }

    @GetMapping("/clubs/{clubId}/players")
    public ResponseEntity<List<PlayerDto>> getClubPlayers(@PathVariable UUID clubId) {
        List<PlayerDto> players = onboardingService.getClubPlayers(clubId);
        return ResponseEntity.ok(players);
    }

    @PutMapping("/users/me/club")
    public ResponseEntity<Void> setFavoriteClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetClubRequest request) {
        onboardingService.setFavoriteClub(principal.getUserId(), request.clubId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/me/players")
    public ResponseEntity<Void> setFavoritePlayers(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetPlayersRequest request) {
        onboardingService.setFavoritePlayers(principal.getUserId(), request.playerIds());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/me/interests")
    public ResponseEntity<Void> setInterests(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetInterestsRequest request) {
        onboardingService.setInterests(principal.getUserId(), request.interests());
        return ResponseEntity.noContent().build();
    }
}
