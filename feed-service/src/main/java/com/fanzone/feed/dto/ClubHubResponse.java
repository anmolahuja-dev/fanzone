package com.fanzone.feed.dto;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated Club Hub response with sections of recent posts,
 * live/upcoming match info, and club metadata.
 */
public record ClubHubResponse(
        UUID clubId,
        String clubName,
        String logoUrl,
        long activeMemberCount,
        LiveMatchInfo liveMatch,
        List<PostDto> news,
        List<PostDto> matchday,
        List<PostDto> discussions,
        List<PostDto> transfers,
        List<PostDto> memes,
        List<PostDto> tactical
) {

    public record LiveMatchInfo(
            UUID matchId,
            UUID homeClubId,
            UUID awayClubId,
            int homeScore,
            int awayScore,
            String status,
            String kickOffTime
    ) {}
}
