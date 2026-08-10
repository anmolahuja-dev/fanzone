package com.fanzone.feed.scoring;

import com.fanzone.common.enums.ReputationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedScorerTest {

    private FeedScorer scorer;
    private UUID clubId;
    private UUID differentClubId;
    private Instant now;

    @BeforeEach
    void setUp() {
        scorer = new FeedScorer();
        clubId = UUID.randomUUID();
        differentClubId = UUID.randomUUID();
        now = Instant.now();
    }

    @Nested
    @DisplayName("Same club bonus (+100)")
    class SameClubBonus {

        @Test
        @DisplayName("should add +100 when post club matches user club")
        void sameClub() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, clubId, now);
            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("should not add bonus when clubs differ")
        void differentClub() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should not add bonus when post club is null")
        void nullPostClub() {
            int score = scorer.computeScore(null, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, clubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should not add bonus when user club is null")
        void nullUserClub() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, null, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("High reputation bonus (+30)")
    class HighReputationBonus {

        @Test
        @DisplayName("should add +30 for TRUSTED author")
        void trustedAuthor() {
            int score = scorer.computeScore(clubId, ReputationLevel.TRUSTED, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("should add +30 for CLUB_EXPERT author")
        void clubExpertAuthor() {
            int score = scorer.computeScore(clubId, ReputationLevel.CLUB_EXPERT, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("should not add bonus for ACTIVE author")
        void activeAuthor() {
            int score = scorer.computeScore(clubId, ReputationLevel.ACTIVE, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should not add bonus for ROOKIE author")
        void rookieAuthor() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should not add bonus when author level is null")
        void nullAuthorLevel() {
            int score = scorer.computeScore(clubId, null, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Recent activity bonus (+20)")
    class RecentActivityBonus {

        @Test
        @DisplayName("should add +20 when last activity within 2 hours")
        void recentActivity() {
            Instant oneHourAgo = now.minus(Duration.ofHours(1));
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, oneHourAgo,
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(20);
        }

        @Test
        @DisplayName("should not add bonus when last activity is exactly 2 hours ago")
        void exactlyTwoHoursAgo() {
            Instant twoHoursAgo = now.minus(Duration.ofHours(2));
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, twoHoursAgo,
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should not add bonus when last activity is older than 2 hours")
        void olderThanTwoHours() {
            Instant threeHoursAgo = now.minus(Duration.ofHours(3));
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, threeHoursAgo,
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should add bonus when last activity is just under 2 hours ago")
        void justUnderTwoHours() {
            Instant justUnder = now.minus(Duration.ofHours(2)).plus(Duration.ofSeconds(1));
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, justUnder,
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(20);
        }

        @Test
        @DisplayName("should not add bonus when lastActivityAt is null")
        void nullLastActivity() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, null,
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("High comments bonus (+15)")
    class HighCommentsBonus {

        @Test
        @DisplayName("should add +15 when comment count is exactly 10")
        void exactlyTenComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    10, 0, differentClubId, now);
            assertThat(score).isEqualTo(15);
        }

        @Test
        @DisplayName("should add +15 when comment count exceeds 10")
        void moreThanTenComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    50, 0, differentClubId, now);
            assertThat(score).isEqualTo(15);
        }

        @Test
        @DisplayName("should not add bonus when comment count is below 10")
        void belowTenComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    9, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Toxic penalty (-100)")
    class ToxicPenalty {

        @Test
        @DisplayName("should apply -100 when toxic report count is exactly 3")
        void exactlyThreeReports() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 3, differentClubId, now);
            assertThat(score).isEqualTo(-100);
        }

        @Test
        @DisplayName("should apply -100 when toxic report count exceeds 3")
        void moreThanThreeReports() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 10, differentClubId, now);
            assertThat(score).isEqualTo(-100);
        }

        @Test
        @DisplayName("should not apply penalty when toxic report count is below 3")
        void belowThreeReports() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 2, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Combined scoring")
    class CombinedScoring {

        @Test
        @DisplayName("should sum all bonuses for maximum score of +165")
        void maximumScore() {
            Instant recentActivity = now.minus(Duration.ofMinutes(30));
            int score = scorer.computeScore(clubId, ReputationLevel.CLUB_EXPERT, recentActivity,
                    15, 0, clubId, now);
            // +100 (same club) + 30 (expert) + 20 (recent) + 15 (comments) = 165
            assertThat(score).isEqualTo(165);
        }

        @Test
        @DisplayName("should compute -100 for toxic post with no other bonuses")
        void minimumMeaningfulScore() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 5, differentClubId, now);
            assertThat(score).isEqualTo(-100);
        }

        @Test
        @DisplayName("should net positive for toxic post with all bonuses")
        void toxicWithAllBonuses() {
            Instant recentActivity = now.minus(Duration.ofMinutes(30));
            int score = scorer.computeScore(clubId, ReputationLevel.TRUSTED, recentActivity,
                    20, 5, clubId, now);
            // +100 + 30 + 20 + 15 - 100 = 65
            assertThat(score).isEqualTo(65);
        }

        @Test
        @DisplayName("should handle combination of club match and high comments only")
        void clubAndComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    12, 0, clubId, now);
            // +100 + 15 = 115
            assertThat(score).isEqualTo(115);
        }
    }

    @Nested
    @DisplayName("Determinism")
    class Determinism {

        @Test
        @DisplayName("should return identical score for identical inputs across multiple calls")
        void identicalInputsProduceIdenticalOutput() {
            Instant fixedNow = Instant.parse("2024-06-15T12:00:00Z");
            Instant fixedActivity = Instant.parse("2024-06-15T11:00:00Z");
            UUID fixedClub = UUID.fromString("00000000-0000-0000-0000-000000000001");

            int score1 = scorer.computeScore(fixedClub, ReputationLevel.TRUSTED, fixedActivity,
                    10, 2, fixedClub, fixedNow);
            int score2 = scorer.computeScore(fixedClub, ReputationLevel.TRUSTED, fixedActivity,
                    10, 2, fixedClub, fixedNow);
            int score3 = scorer.computeScore(fixedClub, ReputationLevel.TRUSTED, fixedActivity,
                    10, 2, fixedClub, fixedNow);

            assertThat(score1).isEqualTo(score2).isEqualTo(score3);
            // +100 (same club) + 30 (trusted) + 20 (recent) + 15 (comments) = 165
            assertThat(score1).isEqualTo(165);
        }

        @Test
        @DisplayName("should return same result from different FeedScorer instances")
        void differentInstances() {
            FeedScorer scorer2 = new FeedScorer();
            Instant fixedNow = Instant.parse("2024-06-15T12:00:00Z");
            Instant fixedActivity = Instant.parse("2024-06-15T11:30:00Z");
            UUID fixedClub = UUID.fromString("00000000-0000-0000-0000-000000000002");

            int result1 = scorer.computeScore(fixedClub, ReputationLevel.ACTIVE, fixedActivity,
                    5, 1, fixedClub, fixedNow);
            int result2 = scorer2.computeScore(fixedClub, ReputationLevel.ACTIVE, fixedActivity,
                    5, 1, fixedClub, fixedNow);

            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("PostScoreContext overload")
    class ContextOverload {

        @Test
        @DisplayName("should produce same result as individual params method")
        void contextMethodMatchesDirectMethod() {
            Instant recentActivity = now.minus(Duration.ofMinutes(45));
            PostScoreContext context = new PostScoreContext(
                    clubId, ReputationLevel.TRUSTED, recentActivity, 12, 1);

            int directScore = scorer.computeScore(clubId, ReputationLevel.TRUSTED, recentActivity,
                    12, 1, clubId, now);
            int contextScore = scorer.computeScore(context, clubId, now);

            assertThat(contextScore).isEqualTo(directScore);
        }

        @Test
        @DisplayName("should handle null fields in context record")
        void nullFieldsInContext() {
            PostScoreContext context = new PostScoreContext(null, null, null, 0, 0);
            int score = scorer.computeScore(context, clubId, now);
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return 0 when all inputs are neutral")
        void allNeutral() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle zero comment count")
        void zeroComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    0, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle negative comment count gracefully")
        void negativeComments() {
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, now.minus(Duration.ofHours(3)),
                    -5, 0, differentClubId, now);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle future lastActivityAt (activity in the future)")
        void futureActivity() {
            Instant futureActivity = now.plus(Duration.ofHours(1));
            int score = scorer.computeScore(clubId, ReputationLevel.ROOKIE, futureActivity,
                    0, 0, differentClubId, now);
            // Duration.between(future, now) is negative, so !isNegative() fails
            assertThat(score).isEqualTo(0);
        }
    }
}
