package com.fanzone.auth.service;

import com.fanzone.auth.dto.ClubDto;
import com.fanzone.auth.dto.PlayerDto;
import com.fanzone.auth.model.ClubEntity;
import com.fanzone.auth.model.PlayerEntity;
import com.fanzone.auth.model.UserEntity;
import com.fanzone.auth.model.UserFavoritePlayer;
import com.fanzone.auth.model.UserInterest;
import com.fanzone.auth.repository.ClubRepository;
import com.fanzone.auth.repository.PlayerRepository;
import com.fanzone.auth.repository.UserFavoritePlayerRepository;
import com.fanzone.auth.repository.UserInterestRepository;
import com.fanzone.auth.repository.UserRepository;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.common.exceptions.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OnboardingService {

    private static final Set<String> VALID_INTERESTS = Set.of(
            "matchday", "transfers", "tactics", "memes", "news"
    );

    private final ClubRepository clubRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final UserFavoritePlayerRepository userFavoritePlayerRepository;
    private final UserInterestRepository userInterestRepository;

    public OnboardingService(ClubRepository clubRepository,
                             PlayerRepository playerRepository,
                             UserRepository userRepository,
                             UserFavoritePlayerRepository userFavoritePlayerRepository,
                             UserInterestRepository userInterestRepository) {
        this.clubRepository = clubRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.userFavoritePlayerRepository = userFavoritePlayerRepository;
        this.userInterestRepository = userInterestRepository;
    }

    public List<ClubDto> searchClubs(String search) {
        List<ClubEntity> clubs;
        if (search == null || search.isBlank()) {
            clubs = clubRepository.findAll();
        } else {
            clubs = clubRepository.findByNameContainingIgnoreCase(search.trim());
        }
        return clubs.stream()
                .map(club -> new ClubDto(club.getId(), club.getName(), club.getLogoUrl()))
                .toList();
    }

    public List<PlayerDto> getClubPlayers(UUID clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("CLUB_NOT_FOUND", "Club not found");
        }
        List<PlayerEntity> players = playerRepository.findByClubId(clubId);
        return players.stream()
                .map(player -> new PlayerDto(player.getId(), player.getName(), player.getPhotoUrl(), player.getPosition()))
                .toList();
    }

    @Transactional
    public void setFavoriteClub(UUID userId, UUID clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("CLUB_NOT_FOUND", "Club not found");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        user.setFavoriteClubId(clubId);
        userRepository.save(user);
    }

    @Transactional
    public void setFavoritePlayers(UUID userId, List<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            throw new ValidationException("ONBOARDING_VALIDATION_FAILED", "At least 1 player must be selected");
        }
        if (playerIds.size() > 5) {
            throw new ValidationException("ONBOARDING_VALIDATION_FAILED", "Maximum 5 players can be selected");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (user.getFavoriteClubId() == null) {
            throw new ValidationException("ONBOARDING_VALIDATION_FAILED", "Favorite club must be set before selecting players");
        }

        // Validate all players exist and belong to the user's club
        List<PlayerEntity> players = playerRepository.findByIdIn(playerIds);
        if (players.size() != playerIds.size()) {
            throw new NotFoundException("PLAYER_NOT_FOUND", "One or more players not found");
        }

        for (PlayerEntity player : players) {
            if (!user.getFavoriteClubId().equals(player.getClubId())) {
                throw new ValidationException("ONBOARDING_VALIDATION_FAILED",
                        "All selected players must belong to the user's favorite club");
            }
        }

        // Replace existing favorite players
        userFavoritePlayerRepository.deleteByUserId(userId);
        List<UserFavoritePlayer> favorites = playerIds.stream()
                .map(playerId -> UserFavoritePlayer.builder()
                        .userId(userId)
                        .playerId(playerId)
                        .build())
                .toList();
        userFavoritePlayerRepository.saveAll(favorites);
    }

    @Transactional
    public void setInterests(UUID userId, List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            throw new ValidationException("ONBOARDING_VALIDATION_FAILED", "At least 1 interest must be selected");
        }

        // Validate all interests are valid values
        for (String interest : interests) {
            if (!VALID_INTERESTS.contains(interest)) {
                throw new ValidationException("ONBOARDING_VALIDATION_FAILED",
                        "Invalid interest: " + interest + ". Valid values are: matchday, transfers, tactics, memes, news");
            }
        }

        // Replace existing interests
        userInterestRepository.deleteByUserId(userId);
        List<UserInterest> userInterests = interests.stream()
                .map(interest -> UserInterest.builder()
                        .userId(userId)
                        .interest(interest)
                        .build())
                .toList();
        userInterestRepository.saveAll(userInterests);
    }
}
