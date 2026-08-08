# Design Document: Fanzone MVP

## Overview

Fanzone MVP is a football-first social platform built as a Java/Spring Boot microservices backend with PostgreSQL, Redis, Apache Kafka, and AWS S3. The system provides club-centric content feeds, real-time match threads via WebSockets, a reputation-based community system, and AI-powered toxicity moderation. The backend exposes REST APIs and WebSocket endpoints consumed by mobile (Flutter/native) and web clients.

### Key Design Decisions

1. **Spring Boot microservices** — Each domain (auth, feed, posts, match threads, reputation, notifications, moderation) is a separate service, enabling independent scaling and deployment.
2. **Apache Kafka as event bus** — Decouples services via domain events (reputation changes, goal events, notifications). Enables async processing and replay.
3. **Redis for caching and rate limiting** — Hot feeds cached in Redis sorted sets. Match thread rate limiting uses Redis sliding windows.
4. **PostgreSQL with Liquibase migrations** — Primary data store with schema versioning. Feed scoring via SQL function for consistency.
5. **Spring WebSocket (STOMP)** — Real-time match thread updates pushed to clients without polling.
6. **Spring Security with JWT** — Stateless auth with access/refresh token pattern. OAuth2 for Google/Apple.
7. **AWS S3 for file storage** — Profile pictures, images, memes stored in S3 with presigned URLs.
8. **Docker + Kubernetes + Helm** — Containerized services deployed to K8s. Helm charts for environment configuration.
9. **GitLab CI/CD** — Automated build, test, and deploy pipelines.
10. **Dynatrace for observability** — Distributed tracing, metrics, and alerting across all services.

---

## Architecture

### High-Level System Diagram

```
┌───────────────────────────────────────┐
│         Clients (Mobile / Web)         │
│    Flutter, React, or native apps      │
└──────────────────┬────────────────────┘
                   │ HTTPS + WebSocket
                   ▼
┌───────────────────────────────────────┐
│          API Gateway (Spring)          │
│   Rate Limiting, Auth, Routing         │
└──────────────────┬────────────────────┘
                   │
       ┌───────────┼───────────────┐
       ▼           ▼               ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│Auth Service│ │Feed Service│ │Post Service│
└─────┬─────┘ └─────┬─────┘ └─────┬─────┘
      │              │              │
      │   ┌──────────┼──────────┐  │
      ▼   ▼          ▼          ▼  ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│Match Thread│ │Reputation │ │Notification│
│  Service   │ │  Service  │ │  Service   │
└─────┬─────┘ └─────┬─────┘ └─────┬─────┘
      │              │              │
      └──────────────┼──────────────┘
                     ▼
┌───────────────────────────────────────┐
│         Infrastructure Layer           │
├───────────┬───────────┬───────────────┤
│PostgreSQL │   Redis   │  Apache Kafka  │
│(Liquibase)│  (Cache)  │  (Event Bus)   │
└───────────┴───────────┴───────────────┘
                     │
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│  AWS S3   │ │ OpenAI API│ │Firebase FCM│
│ (Storage) │ │(Moderation)│ │  (Push)   │
└───────────┘ └───────────┘ └───────────┘
```

### Service Decomposition

| Service | Responsibility | Port |
|---------|---------------|------|
| **api-gateway** | Request routing, rate limiting, JWT validation, CORS | 8080 |
| **auth-service** | Registration, login, OAuth2, JWT issuance, session mgmt | 8081 |
| **feed-service** | Feed scoring, pagination, timeline assembly | 8082 |
| **post-service** | Post CRUD, image upload, poll management, comments | 8083 |
| **match-thread-service** | Live match threads, WebSocket, player ratings, MOTM | 8084 |
| **reputation-service** | Score calculation, level assignment, event consumption | 8085 |
| **notification-service** | Push notifications, activity feed, preferences | 8086 |
| **moderation-service** | Toxicity analysis via OpenAI, content flagging | 8087 |

### Kafka Topics and Event Flow

```
┌─────────────────────────────────────────────────────┐
│                  Kafka Topics                        │
├─────────────────────────────────────────────────────┤
│ fanzone.reputation.events    → Reputation Service   │
│ fanzone.notification.events  → Notification Service │
│ fanzone.moderation.requests  → Moderation Service   │
│ fanzone.match.events         → Match Thread Service │
│ fanzone.feed.invalidation    → Feed Service (cache) │
└─────────────────────────────────────────────────────┘
```

**Event Flow Examples:**

1. **Post Upvote** → Post Service publishes `PostUpvotedEvent` to `fanzone.reputation.events` → Reputation Service consumes, updates score → publishes `ReputationChangedEvent` to `fanzone.notification.events` → Notification Service creates activity feed entry.

2. **Goal Scored** → Match Thread Service publishes `GoalScoredEvent` to `fanzone.match.events` → Notification Service consumes, sends push notifications to subscribed users.

3. **Comment Created** → Post Service publishes `CommentCreatedEvent` to `fanzone.notification.events` → Notification Service creates reply/mention notifications. Also publishes to `fanzone.feed.invalidation` → Feed Service invalidates cached feed entries.

### Inter-Service Communication

| Pattern | Use Case | Implementation |
|---------|----------|----------------|
| Synchronous REST | Auth validation, feed assembly | Spring WebClient with circuit breaker (Resilience4j) |
| Async Events (Kafka) | Reputation updates, notifications, feed invalidation | Spring Kafka with consumer groups |
| WebSocket (STOMP) | Live match thread comments, goal reactions | Spring WebSocket + Redis pub/sub for multi-instance |

---

## API Design

### REST API Conventions

- Base path: `/api/v1/`
- Resource naming: plural kebab-case nouns (e.g., `/api/v1/posts`, `/api/v1/match-threads`)
- Max path depth: 3 segments after version
- Pagination: cursor-based with `?cursor=<id>&size=20`
- Versioning: URL path (`/v1/`)

### Core Endpoints

#### Auth Service

```
POST   /api/v1/auth/register          - Email registration
POST   /api/v1/auth/login             - Email login
POST   /api/v1/auth/oauth/google      - Google OAuth2 login/register
POST   /api/v1/auth/oauth/apple       - Apple sign-in login/register
POST   /api/v1/auth/refresh           - Refresh access token
POST   /api/v1/auth/logout            - Invalidate session
POST   /api/v1/auth/verify-email      - Verify email with token
POST   /api/v1/auth/resend-verification - Resend verification email
```

#### Feed Service

```
GET    /api/v1/feeds/for-you          - Personalized feed (scored)
GET    /api/v1/feeds/club             - Club-specific feed (by recency)
GET    /api/v1/feeds/following         - Following feed (by recency)
```

#### Post Service

```
POST   /api/v1/posts                  - Create post (text/image/poll/analysis)
GET    /api/v1/posts/{postId}         - Get single post
DELETE /api/v1/posts/{postId}         - Delete own post
POST   /api/v1/posts/{postId}/upvotes - Upvote a post
DELETE /api/v1/posts/{postId}/upvotes - Remove upvote
POST   /api/v1/posts/{postId}/reports - Report a post
GET    /api/v1/posts/{postId}/comments - List comments for post
POST   /api/v1/posts/{postId}/comments - Create comment on post
POST   /api/v1/comments/{commentId}/replies   - Reply to comment
POST   /api/v1/comments/{commentId}/upvotes   - Upvote comment
DELETE /api/v1/comments/{commentId}/upvotes   - Remove comment upvote
POST   /api/v1/comments/{commentId}/reports   - Report comment
```

#### Match Thread Service

```
GET    /api/v1/matches                - List upcoming/live matches
GET    /api/v1/matches/{matchId}      - Get match details
GET    /api/v1/match-threads/{matchId}/comments - Get thread comments
POST   /api/v1/match-threads/{matchId}/comments - Post to match thread
POST   /api/v1/match-threads/{matchId}/ratings  - Submit player rating
POST   /api/v1/match-threads/{matchId}/reactions - Submit goal reaction
GET    /api/v1/match-threads/{matchId}/motm     - Get MOTM results
WS     /ws/match-threads/{matchId}              - WebSocket subscription
```

#### User & Profile

```
GET    /api/v1/users/me               - Current user profile
PUT    /api/v1/users/me               - Update profile
POST   /api/v1/users/me/avatar        - Upload profile picture
GET    /api/v1/users/{userId}         - Other user profile
GET    /api/v1/users/{userId}/posts   - User's posts
POST   /api/v1/users/{userId}/follows - Follow user
DELETE /api/v1/users/{userId}/follows - Unfollow user
```

#### Onboarding & Preferences

```
GET    /api/v1/clubs                  - List all clubs (with search)
GET    /api/v1/clubs/{clubId}/players - List players for a club
PUT    /api/v1/users/me/club          - Set favorite club
PUT    /api/v1/users/me/players       - Set favorite players
PUT    /api/v1/users/me/interests     - Set interests
GET    /api/v1/club-hubs/{clubId}     - Get club hub data
```

#### Notifications

```
GET    /api/v1/activity-feeds         - Get activity feed items
PUT    /api/v1/activity-feeds/read    - Mark all as read
GET    /api/v1/notification-preferences - Get preferences
PUT    /api/v1/notification-preferences - Update preferences
```

---

## Components and Interfaces

### 1. Authentication Module (auth-service)

```java
// AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);
    
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);
    
    @PostMapping("/oauth/google")
    ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody OAuthRequest request);
    
    @PostMapping("/oauth/apple")
    ResponseEntity<AuthResponse> appleAuth(@Valid @RequestBody OAuthRequest request);
    
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request);
    
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal);
}

// AuthService.java
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse oauthLogin(OAuthProvider provider, String token);
    void handleFailedLogin(String email); // lockout after 5 attempts
    boolean isSessionValid(UUID userId, Instant lastActivity); // 7-day check
}

// Validation
// Email: exactly one @ followed by domain with at least one dot
// Password: 8-128 chars, 1 uppercase, 1 lowercase, 1 digit
// Lockout: 5 consecutive failures → 15-minute lock
// Session: expires after 7 days of inactivity
```

### 2. Feed Engine (feed-service)

```java
// FeedController.java
@RestController
@RequestMapping("/api/v1/feeds")
public class FeedController {
    @GetMapping("/for-you")
    ResponseEntity<CursorPage<PostDto>> forYouFeed(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size);
    
    @GetMapping("/club")
    ResponseEntity<CursorPage<PostDto>> clubFeed(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size);
    
    @GetMapping("/following")
    ResponseEntity<CursorPage<PostDto>> followingFeed(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size);
}

// FeedScorer.java
public class FeedScorer {
    /**
     * Deterministic composite scoring.
     * +100 if post.clubId == user.favoriteClubId
     * +30  if author.reputationLevel in [TRUSTED, CLUB_EXPERT]
     * +20  if post has activity within last 2 hours
     * +15  if post.commentCount >= 10
     * -100 if post.toxicReportCount >= 3
     */
    public int computeScore(Post post, UUID userClubId, Instant now);
}
```

### 3. Match Thread System (match-thread-service)

```java
// MatchThreadController.java
@RestController
@RequestMapping("/api/v1/match-threads")
public class MatchThreadController {
    @GetMapping("/{matchId}/comments")
    ResponseEntity<CursorPage<MatchCommentDto>> getComments(
        @PathVariable UUID matchId,
        @RequestParam(required = false) String cursor);
    
    @PostMapping("/{matchId}/comments")
    ResponseEntity<MatchCommentDto> postComment(
        @PathVariable UUID matchId,
        @Valid @RequestBody CreateCommentRequest request,
        @AuthenticationPrincipal UserPrincipal user);
    
    @PostMapping("/{matchId}/ratings")
    ResponseEntity<Void> submitRating(
        @PathVariable UUID matchId,
        @Valid @RequestBody PlayerRatingRequest request,
        @AuthenticationPrincipal UserPrincipal user);
    
    @GetMapping("/{matchId}/motm")
    ResponseEntity<MotmResponse> getMotm(@PathVariable UUID matchId);
}

// MatchThreadWebSocketHandler.java (STOMP)
// Subscription: /topic/match-threads/{matchId}
// Sends: new comments, goal reactions, match status changes
// Rate limit: max 20 comments per user per 60-second rolling window (Redis)

// Match phase transitions: scheduled → live → finished (monotonic, never backward)
// Post-match timeout: thread becomes read-only 48 hours after finished_at
```

### 4. Reputation System (reputation-service)

```java
// ReputationService.java
public interface ReputationService {
    /**
     * Processes reputation events consumed from Kafka.
     * Score never goes below 0 (floor invariant).
     * Level thresholds: 0-99 Rookie, 100-499 Active, 500-1999 Trusted, 2000+ Expert
     */
    int adjustReputation(UUID userId, ReputationEvent event);
    ReputationLevel computeLevel(int score);
}

public enum ReputationEvent {
    POST_UPVOTED(2),
    COMMENT_UPVOTED(1),
    HELPFUL_BADGE(10),
    ACCURATE_PREDICTION(20),
    POST_UPVOTE_REMOVED(-2),
    COMMENT_UPVOTE_REMOVED(-1),
    COMMENT_REMOVED_BY_MOD(-20),
    TOXIC_CONTENT_CONFIRMED(-50),
    FAKE_NEWS_CONFIRMED(-100);
    
    private final int points;
}

public enum ReputationLevel {
    ROOKIE(0, 99),
    ACTIVE(100, 499),
    TRUSTED(500, 1999),
    CLUB_EXPERT(2000, Integer.MAX_VALUE);
}
```

### 5. Toxicity Shield (moderation-service)

```java
// ModerationService.java
public interface ModerationService {
    /**
     * Analyzes content via OpenAI API.
     * Returns within 2 seconds. On timeout → returns safe, queues for async review.
     * Does NOT flag football performance criticism without personal attacks.
     */
    ToxicityResult analyzeContent(String content);
}

public record ToxicityResult(
    boolean isToxic,
    String reason,   // null if not toxic
    double confidence
) {}
```

### 6. Notification Service (notification-service)

```java
// NotificationService.java
public interface NotificationService {
    void sendMatchAlert(UUID matchId, List<UUID> userIds);
    void sendGoalNotification(GoalEvent event);
    void sendReplyNotification(UUID recipientId, String senderUsername, String preview);
    void sendMentionNotification(UUID recipientId, String senderUsername, String preview);
    boolean shouldSend(UUID userId, NotificationType type); // preference check
}

// Preview truncation: max 100 characters
// FCM retry: up to 3 times with exponential backoff
// Preference enforcement: never deliver disabled types
```

### 7. Follow System (post-service)

```java
// FollowController.java
@RestController
@RequestMapping("/api/v1/users")
public class FollowController {
    @PostMapping("/{userId}/follows")
    ResponseEntity<Void> follow(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal);
    
    @DeleteMapping("/{userId}/follows")
    ResponseEntity<Void> unfollow(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal);
}

// Constraints:
// - Self-follow prevented (follower_id ≠ followed_id)
// - Idempotent: duplicate follow → no error, no duplicate row
// - No notification on follow (anti-follower-farming)
```

---

## Data Models

### Database Schema (PostgreSQL + Liquibase)

```sql
-- Core tables
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255), -- null for OAuth-only users
  auth_provider VARCHAR(20) NOT NULL DEFAULT 'email',
  favorite_club_id UUID REFERENCES clubs(id),
  reputation INTEGER NOT NULL DEFAULT 0 CHECK (reputation >= 0),
  reputation_level VARCHAR(20) NOT NULL DEFAULT 'ROOKIE',
  profile_picture_url TEXT,
  theme_preference VARCHAR(10) DEFAULT 'system' CHECK (theme_preference IN ('light', 'dark', 'system')),
  email_verified BOOLEAN DEFAULT false,
  failed_login_attempts INTEGER DEFAULT 0,
  locked_until TIMESTAMP,
  last_active_at TIMESTAMP DEFAULT now(),
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE clubs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  logo_url TEXT,
  league VARCHAR(100),
  primary_color VARCHAR(7), -- hex color for club theme
  secondary_color VARCHAR(7),
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE players (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  club_id UUID REFERENCES clubs(id),
  name VARCHAR(100) NOT NULL,
  photo_url TEXT,
  position VARCHAR(30)
);

CREATE TABLE user_favorite_players (
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  player_id UUID REFERENCES players(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, player_id)
);

CREATE TABLE user_interests (
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  interest VARCHAR(20) CHECK (interest IN ('matchday', 'transfers', 'tactics', 'memes', 'news')),
  PRIMARY KEY (user_id, interest)
);

CREATE TABLE posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) NOT NULL,
  club_id UUID REFERENCES clubs(id),
  post_type VARCHAR(20) NOT NULL CHECK (post_type IN ('text', 'image', 'poll', 'match_analysis')),
  content TEXT,
  image_url TEXT,
  comment_count INTEGER DEFAULT 0,
  upvote_count INTEGER DEFAULT 0,
  toxic_report_count INTEGER DEFAULT 0,
  is_flagged BOOLEAN DEFAULT false,
  last_activity_at TIMESTAMP DEFAULT now(),
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE poll_options (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  option_text VARCHAR(100) NOT NULL,
  vote_count INTEGER DEFAULT 0,
  display_order INTEGER NOT NULL
);

CREATE TABLE poll_votes (
  user_id UUID REFERENCES users(id),
  option_id UUID REFERENCES poll_options(id),
  PRIMARY KEY (user_id, option_id)
);

CREATE TABLE comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID REFERENCES posts(id) NOT NULL,
  user_id UUID REFERENCES users(id) NOT NULL,
  parent_comment_id UUID REFERENCES comments(id),
  content TEXT NOT NULL,
  upvote_count INTEGER DEFAULT 0,
  nesting_level INTEGER NOT NULL DEFAULT 0 CHECK (nesting_level <= 3),
  is_flagged BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE comment_upvotes (
  user_id UUID REFERENCES users(id),
  comment_id UUID REFERENCES comments(id),
  PRIMARY KEY (user_id, comment_id)
);

CREATE TABLE post_upvotes (
  user_id UUID REFERENCES users(id),
  post_id UUID REFERENCES posts(id),
  PRIMARY KEY (user_id, post_id)
);

CREATE TABLE matches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  home_club_id UUID REFERENCES clubs(id) NOT NULL,
  away_club_id UUID REFERENCES clubs(id) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'live', 'finished')),
  home_score INTEGER DEFAULT 0,
  away_score INTEGER DEFAULT 0,
  finished_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE player_ratings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID REFERENCES matches(id) NOT NULL,
  player_id UUID REFERENCES players(id) NOT NULL,
  user_id UUID REFERENCES users(id) NOT NULL,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 10),
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE (match_id, player_id, user_id)
);

CREATE TABLE follows (
  follower_id UUID REFERENCES users(id),
  followed_id UUID REFERENCES users(id),
  created_at TIMESTAMP DEFAULT now(),
  PRIMARY KEY (follower_id, followed_id),
  CHECK (follower_id != followed_id)
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) NOT NULL,
  type VARCHAR(30) NOT NULL CHECK (type IN ('mention', 'reply', 'match_alert', 'goal', 'reputation_change')),
  title VARCHAR(255),
  body VARCHAR(255),
  source_id UUID,
  source_type VARCHAR(30),
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE notification_preferences (
  user_id UUID REFERENCES users(id) PRIMARY KEY,
  match_alerts BOOLEAN DEFAULT true,
  goals BOOLEAN DEFAULT true,
  replies BOOLEAN DEFAULT true,
  mentions BOOLEAN DEFAULT true
);

CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID REFERENCES users(id) NOT NULL,
  target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('post', 'comment')),
  target_id UUID NOT NULL,
  reason TEXT,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE (reporter_id, target_type, target_id)
);

CREATE TABLE match_thread_comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID REFERENCES matches(id) NOT NULL,
  user_id UUID REFERENCES users(id) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE goal_reactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID REFERENCES matches(id) NOT NULL,
  user_id UUID REFERENCES users(id) NOT NULL,
  reaction_type VARCHAR(30) NOT NULL,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE (match_id, user_id, reaction_type)
);

CREATE TABLE activity_feed (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) NOT NULL,
  type VARCHAR(30) NOT NULL CHECK (type IN ('mention', 'reply', 'match_alert', 'reputation_change')),
  source_id UUID,
  source_type VARCHAR(30),
  preview_text VARCHAR(100),
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT now()
);

-- Feed scoring function (mirrors FeedScorer.java logic)
CREATE OR REPLACE FUNCTION compute_feed_score(
  p_post_club_id UUID,
  p_user_club_id UUID,
  p_author_reputation_level TEXT,
  p_last_activity_at TIMESTAMP,
  p_comment_count INTEGER,
  p_toxic_report_count INTEGER,
  p_now TIMESTAMP DEFAULT now()
) RETURNS INTEGER AS $$
DECLARE
  score INTEGER := 0;
BEGIN
  IF p_post_club_id = p_user_club_id THEN score := score + 100; END IF;
  IF p_author_reputation_level IN ('TRUSTED', 'CLUB_EXPERT') THEN score := score + 30; END IF;
  IF p_last_activity_at > (p_now - INTERVAL '2 hours') THEN score := score + 20; END IF;
  IF p_comment_count >= 10 THEN score := score + 15; END IF;
  IF p_toxic_report_count >= 3 THEN score := score - 100; END IF;
  RETURN score;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Indexes for performance
CREATE INDEX idx_posts_club_id ON posts(club_id);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_match_thread_comments_match_id ON match_thread_comments(match_id, created_at);
CREATE INDEX idx_player_ratings_match_id ON player_ratings(match_id);
CREATE INDEX idx_activity_feed_user_id ON activity_feed(user_id, created_at DESC);
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_followed ON follows(followed_id);
```

### Kafka Event Schemas

```java
// Domain events published to Kafka topics

public record PostUpvotedEvent(
    UUID postId,
    UUID authorId,
    UUID upvoterId,
    Instant timestamp
) {} // → fanzone.reputation.events

public record CommentUpvotedEvent(
    UUID commentId,
    UUID authorId,
    UUID upvoterId,
    Instant timestamp
) {} // → fanzone.reputation.events

public record CommentCreatedEvent(
    UUID commentId,
    UUID postId,
    UUID authorId,
    UUID parentCommentAuthorId, // null if top-level
    List<String> mentionedUsernames,
    String previewText,
    Instant timestamp
) {} // → fanzone.notification.events

public record GoalScoredEvent(
    UUID matchId,
    UUID homeClubId,
    UUID awayClubId,
    String scorerName,
    int homeScore,
    int awayScore,
    int minute,
    Instant timestamp
) {} // → fanzone.match.events, fanzone.notification.events

public record MatchStatusChangedEvent(
    UUID matchId,
    String previousStatus,
    String newStatus,
    Instant timestamp
) {} // → fanzone.match.events

public record ReputationChangedEvent(
    UUID userId,
    int previousScore,
    int newScore,
    String previousLevel,
    String newLevel,
    String eventType,
    Instant timestamp
) {} // → fanzone.notification.events

public record FeedInvalidationEvent(
    UUID clubId,
    UUID postId,
    String reason, // "new_comment", "new_upvote", "new_post"
    Instant timestamp
) {} // → fanzone.feed.invalidation
```

---

## Project Structure

```
fanzone/
├── docker-compose.yml              # Local dev: PostgreSQL, Redis, Kafka, Zookeeper
├── docker-compose.infra.yml        # Infrastructure services only
├── pom.xml                         # Parent POM (multi-module Maven)
│
├── fanzone-common/                 # Shared library
│   ├── src/main/java/
│   │   └── com/fanzone/common/
│   │       ├── dto/                # Shared DTOs, page models
│   │       ├── events/             # Kafka event records
│   │       ├── exceptions/         # Common exception hierarchy
│   │       ├── security/           # JWT utilities, UserPrincipal
│   │       └── validation/         # Shared validators
│   └── pom.xml
│
├── auth-service/
│   ├── src/main/java/com/fanzone/auth/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── config/                 # SecurityConfig, OAuth2Config
│   │   └── AuthServiceApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/changelog/          # Liquibase changesets
│   ├── src/test/java/
│   ├── Dockerfile
│   └── pom.xml
│
├── feed-service/
│   ├── src/main/java/com/fanzone/feed/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── scoring/                # FeedScorer
│   │   ├── cache/                  # Redis feed cache
│   │   └── FeedServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── post-service/
│   ├── src/main/java/com/fanzone/post/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── validation/             # PostValidator, CommentValidator
│   │   ├── kafka/                  # Event publishers
│   │   └── PostServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── match-thread-service/
│   ├── src/main/java/com/fanzone/matchthread/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── websocket/              # STOMP config, handlers
│   │   ├── ratelimit/              # Redis-based rate limiter
│   │   ├── repository/
│   │   └── MatchThreadServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── reputation-service/
│   ├── src/main/java/com/fanzone/reputation/
│   │   ├── service/
│   │   ├── kafka/                  # Event consumers
│   │   ├── repository/
│   │   └── ReputationServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── notification-service/
│   ├── src/main/java/com/fanzone/notification/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── kafka/                  # Event consumers
│   │   ├── fcm/                    # Firebase Cloud Messaging client
│   │   ├── repository/
│   │   └── NotificationServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── moderation-service/
│   ├── src/main/java/com/fanzone/moderation/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── openai/                 # OpenAI client
│   │   └── ModerationServiceApplication.java
│   ├── Dockerfile
│   └── pom.xml
│
├── helm/
│   ├── fanzone/                    # Umbrella Helm chart
│   │   ├── Chart.yaml
│   │   ├── values.yaml
│   │   └── templates/
│   └── values-dev.yaml
│
├── k8s/
│   └── local/                      # Local K8s manifests (optional)
│
├── .gitlab-ci.yml                  # CI/CD pipeline
└── README.md
```

---

## Error Handling

### Error Response Model

All services return a consistent error structure:

```java
public record ErrorResponse(
    String code,         // DOMAIN_ERROR_NAME format (e.g., AUTH_INVALID_CREDENTIALS)
    String message,      // Human-readable, no PII
    String severity,     // ERROR, WARN, INFO
    String source,       // Service name
    Instant timestamp,
    String traceId       // From MDC
) {}
```

### HTTP Status Mapping

| Scenario | Status | Error Code |
|----------|--------|-----------|
| Validation failure | 400 | `{DOMAIN}_VALIDATION_FAILED` |
| Invalid credentials | 401 | `AUTH_INVALID_CREDENTIALS` |
| Account locked | 401 | `AUTH_ACCOUNT_LOCKED` |
| Email not verified | 403 | `AUTH_EMAIL_NOT_VERIFIED` |
| Self-upvote | 403 | `REPUTATION_SELF_UPVOTE_DENIED` |
| Resource not found | 404 | `{DOMAIN}_NOT_FOUND` |
| Duplicate follow | 409 | `FOLLOW_ALREADY_EXISTS` |
| Rate limit exceeded | 429 | `MATCH_THREAD_RATE_LIMITED` |
| Thread read-only | 422 | `MATCH_THREAD_CLOSED` |
| Internal error | 500 | `{DOMAIN}_INTERNAL_ERROR` |

### Error Handling Strategy

| Layer | Strategy |
|-------|----------|
| **Network (REST clients)** | Resilience4j circuit breaker + 3 retries with exponential backoff |
| **Auth** | Generic messages, never reveal which field is wrong. Lock after 5 failures. |
| **Validation** | Jakarta Bean Validation (`@Valid`) with `ConstraintViolationException` → 400 |
| **File Upload** | Validate format/size before S3 upload. On failure, return error preserving client state. |
| **WebSocket** | Auto-reconnect with missed message replay via Redis sorted sets. |
| **Toxicity AI** | 2-second timeout (WebClient). On timeout → safe result, queue async review. |
| **Push (FCM)** | 3 retries with exponential backoff. Discard after max retries. |
| **Kafka consumers** | Retry 3 times → Dead Letter Queue (`{topic}.dlq`) |

### Exception Hierarchy

```java
public abstract class FanzoneException extends RuntimeException {
    abstract String getCode();
    abstract int getHttpStatus();
}

public class ValidationException extends FanzoneException { /* 400 */ }
public class AuthenticationException extends FanzoneException { /* 401 */ }
public class ForbiddenException extends FanzoneException { /* 403 */ }
public class NotFoundException extends FanzoneException { /* 404 */ }
public class ConflictException extends FanzoneException { /* 409 */ }
public class RateLimitException extends FanzoneException { /* 429 */ }
public class BusinessRuleException extends FanzoneException { /* 422 */ }
```

---

## Correctness Properties

### Property 1: Email Validation Correctness

*For any* string, the email validation function SHALL accept it if and only if it contains exactly one `@` symbol followed by a domain with at least one dot. Strings missing the `@`, containing multiple `@` symbols, or lacking a dot in the domain portion SHALL be rejected.

**Validates: Requirements 1.5**

### Property 2: Feed Scoring Determinism and Correctness

*For any* post and user context, the composite feed score SHALL be deterministic (identical inputs always produce identical output) and SHALL equal the sum of: +100 if post.clubId matches user.favoriteClubId, +30 if author reputation level is Trusted or Club Expert, +20 if last activity is within 2 hours of the reference time, +15 if commentCount >= 10, and -100 if toxicReportCount >= 3.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6**

### Property 3: Club Tab Filtering

*For any* set of posts and a user with a favorite club, the "Club" tab feed SHALL contain only posts where post.clubId equals user.favoriteClubId, and those posts SHALL be ordered by createdAt descending (most recent first).

**Validates: Requirements 5.9**

### Property 4: Following Tab Filtering

*For any* set of posts, a user, and a set of follow relationships, the "Following" tab feed SHALL contain only posts authored by users in the current user's following set, and those posts SHALL be ordered by createdAt descending.

**Validates: Requirements 5.10, 14.3**

### Property 5: Post Content Validation

*For any* post submission: text posts SHALL be accepted if and only if content length is between 1 and 2000 characters; poll posts SHALL be accepted if and only if the question is 1-200 characters, option count is 2-4, and each option is 1-100 characters; match analysis posts SHALL be accepted if and only if content is 1-10000 characters. Posts with empty content and no image SHALL always be rejected.

**Validates: Requirements 6.1, 6.3, 6.4, 6.6**

### Property 6: Post Club Association Invariant

*For any* newly created post, the post's clubId SHALL equal the authoring user's favoriteClubId at the time of creation. No post SHALL exist without a club association when the author has a favorite club set.

**Validates: Requirements 6.5**

### Property 7: Comment Nesting Depth Invariant

*For any* comment in the system, its nestingLevel SHALL be at most 3. For any reply to a comment at nestingLevel N where N < 3, the reply SHALL have nestingLevel N+1. For any reply attempted at nestingLevel 3, the reply SHALL be placed at nestingLevel 3 (not deeper).

**Validates: Requirements 7.2, 7.6, 7.7**

### Property 8: Upvote Idempotence

*For any* user and comment pair, the user SHALL be able to upvote the comment at most once. Subsequent upvote attempts on the same comment by the same user SHALL have no effect on the upvoteCount or the author's reputation.

**Validates: Requirements 7.3, 7.5**

### Property 9: Match Phase Monotonicity

*For any* match, status transitions SHALL be monotonic following the sequence: scheduled → live → finished. A match SHALL never transition backward. The set of valid transitions is exactly: {scheduled → live, live → finished}.

**Validates: Requirements 9.1, 10.1**

### Property 10: Player Rating Bounds

*For any* player rating submission, the rating value SHALL be an integer between 1 and 10 inclusive. Each user SHALL have at most one rating per player per match, and subsequent submissions SHALL overwrite (upsert) the previous rating.

**Validates: Requirements 9.5**

### Property 11: Live Match Comment Rate Limiting

*For any* user in a live match thread, within any rolling 60-second window, the system SHALL accept at most 20 comments. The 21st comment attempt within any 60-second window SHALL be rejected with 429.

**Validates: Requirements 9.8**

### Property 12: MOTM Calculation Correctness

*For any* match with 10 or more total player ratings submitted, the MOTM SHALL be the player with the highest average rating. Top-3 ordered by average desc, ties broken by count desc. If fewer than 10 total ratings exist, no MOTM SHALL be returned.

**Validates: Requirements 10.3, 10.4, 10.6**

### Property 13: Post-Match Thread Timeout

*For any* finished match, the match thread SHALL accept comments if and only if the current time is within 48 hours of finished_at. After 48 hours, comment submissions SHALL be rejected with 422.

**Validates: Requirements 10.7**

### Property 14: Reputation Event Application

*For any* reputation event applied to a user, the score change SHALL equal exactly the event's defined point value. The final score SHALL be the sum of all applied events clamped to minimum 0.

**Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.13**

### Property 15: Reputation Floor Invariant

*For any* sequence of reputation events, the user's reputation score SHALL never be less than 0. If applying an event would result in a negative value, the score SHALL be clamped to 0.

**Validates: Requirements 11.12**

### Property 16: Reputation Level Assignment

*For any* user with reputation score S: "Rookie Fan" if 0 ≤ S ≤ 99, "Active Fan" if 100 ≤ S ≤ 499, "Trusted Fan" if 500 ≤ S ≤ 1999, "Club Expert" if S ≥ 2000. Level assignment SHALL update immediately on threshold crossing.

**Validates: Requirements 11.8, 11.9, 11.10, 11.11, 11.14**

### Property 17: Self-Upvote Prevention

*For any* user attempting to upvote their own post or comment, the system SHALL reject with 403. The upvoteCount and reputation score SHALL remain unchanged.

**Validates: Requirements 11.15**

### Property 18: Notification Preference Enforcement

*For any* notification event, delivery SHALL occur if and only if the corresponding type is enabled in the user's preferences. Disabled types SHALL never be delivered.

**Validates: Requirements 13.5, 13.6**

### Property 19: Notification Preview Truncation

*For any* notification containing reply or mention text, the preview SHALL contain at most 100 characters. Text exceeding 100 characters SHALL be truncated to exactly 100.

**Validates: Requirements 13.3, 13.4**

### Property 20: Follow Relationship Integrity

*For any* user, self-follow SHALL be prevented (follower_id ≠ followed_id). At most one follow relationship per pair. Creating a follow that already exists SHALL be idempotent (no duplicate, no error).

**Validates: Requirements 14.6, 14.7**

### Property 21: Activity Feed Ordering

*For any* set of activity feed items, items SHALL be ordered by createdAt descending. No item with a later timestamp SHALL appear after one with an earlier timestamp.

**Validates: Requirements 21.1**

### Property 22: Activity Badge Count Accuracy

*For any* sequence of activity arrivals and feed-open events, the unread badge count SHALL equal items created after the user's most recent feed-open. Opening the feed SHALL reset to 0.

**Validates: Requirements 21.5**

### Property 23: Session Validity Window

*For any* session, it SHALL remain valid if and only if time since last activity ≤ 7 days. Sessions exceeding 7 days SHALL be expired.

**Validates: Requirements 2.5, 2.6**

### Property 24: Account Lockout Threshold

*For any* user, the account SHALL be locked if and only if consecutive failed login attempts ≥ 5. Lockout duration SHALL be exactly 15 minutes. After expiry, the counter resets.

**Validates: Requirements 2.4**

---

## Testing Strategy

### Testing Approach

The Fanzone MVP uses a layered testing strategy:

1. **Property-based tests (JUnit 5 + jqwik)** — Verify universal correctness properties across randomized inputs (minimum 100 iterations per property)
2. **Unit tests (JUnit 5 + Mockito)** — Verify specific scenarios, edge cases, and service logic
3. **Integration tests (Spring Boot Test + Testcontainers)** — Verify database operations, Kafka consumers/producers, Redis caching with real dependencies
4. **API tests (MockMvc / WebTestClient)** — Verify REST endpoints, validation, error responses
5. **Load tests (K6)** — Verify performance under expected load (match thread concurrency)

### Property-Based Testing Configuration

- **Library**: jqwik (JUnit 5 property-based testing for Java)
- **Minimum iterations**: 100 per property
- **Tag format**: `@Tag("property") @Label("Property {N}: {title}")`

### Test Organization

```
{service}/src/test/java/com/fanzone/{service}/
├── property/
│   ├── FeedScoringPropertyTest.java
│   ├── ReputationPropertyTest.java
│   ├── MatchThreadPropertyTest.java
│   ├── CommentNestingPropertyTest.java
│   ├── ValidationPropertyTest.java
│   ├── FollowPropertyTest.java
│   ├── NotificationPropertyTest.java
│   └── ActivityFeedPropertyTest.java
├── unit/
│   ├── AuthServiceTest.java
│   ├── FeedScorerTest.java
│   ├── PostValidatorTest.java
│   ├── CommentServiceTest.java
│   ├── ReputationServiceTest.java
│   └── RateLimiterTest.java
├── integration/
│   ├── PostRepositoryIntegrationTest.java
│   ├── KafkaConsumerIntegrationTest.java
│   ├── RedisCacheIntegrationTest.java
│   └── WebSocketIntegrationTest.java
└── api/
    ├── AuthControllerTest.java
    ├── FeedControllerTest.java
    ├── PostControllerTest.java
    └── MatchThreadControllerTest.java
```

### Key Algorithms

#### Feed Scoring (Java + SQL mirror)

```java
public int computeScore(Post post, UUID userClubId, Instant now) {
    int score = 0;
    if (post.getClubId().equals(userClubId)) score += 100;
    if (post.getAuthorLevel().isHighReputation()) score += 30;
    if (post.getLastActivityAt().isAfter(now.minus(2, ChronoUnit.HOURS))) score += 20;
    if (post.getCommentCount() >= 10) score += 15;
    if (post.getToxicReportCount() >= 3) score -= 100;
    return score;
}
```

#### Reputation Level Calculation

```java
public ReputationLevel computeLevel(int score) {
    if (score >= 2000) return CLUB_EXPERT;
    if (score >= 500) return TRUSTED;
    if (score >= 100) return ACTIVE;
    return ROOKIE;
}
```

#### MOTM Calculation

```java
public Optional<List<MotmEntry>> calculateMotm(UUID matchId) {
    List<PlayerRating> ratings = ratingRepo.findByMatchId(matchId);
    if (ratings.size() < 10) return Optional.empty();
    
    Map<UUID, List<PlayerRating>> grouped = ratings.stream()
        .collect(Collectors.groupingBy(PlayerRating::getPlayerId));
    
    List<MotmEntry> sorted = grouped.entrySet().stream()
        .map(e -> new MotmEntry(e.getKey(), average(e.getValue()), e.getValue().size()))
        .sorted(Comparator.comparingDouble(MotmEntry::average).reversed()
            .thenComparing(Comparator.comparingInt(MotmEntry::count).reversed()))
        .limit(3)
        .toList();
    
    return Optional.of(sorted);
}
```

#### Rate Limiter (Redis sliding window)

```java
public boolean canPostComment(UUID userId, UUID matchId, Instant now) {
    String key = "rate:" + matchId + ":" + userId;
    Instant windowStart = now.minusSeconds(60);
    // Remove entries outside window
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart.toEpochMilli());
    // Count entries in window
    Long count = redisTemplate.opsForZSet().zCard(key);
    return count != null && count < 20;
}
```

---

## Infrastructure

### Local Development (Docker Compose)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: fanzone
      POSTGRES_USER: fanzone
      POSTGRES_PASSWORD: ${DB_PASSWORD}
  
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]
```

### Deployment (Kubernetes + Helm)

Each service is deployed as a K8s Deployment with:
- HPA (Horizontal Pod Autoscaler) for match-thread-service (scales on WebSocket connections)
- Resource limits and requests defined per service
- ConfigMaps for non-sensitive config, Secrets (from vault) for credentials
- Readiness/liveness probes on Spring Actuator endpoints

### CI/CD Pipeline (GitLab CI)

```yaml
stages:
  - build
  - test
  - package
  - deploy

build:
  script: mvn clean compile -pl ${SERVICE}

test:
  script: mvn verify -pl ${SERVICE}
  services: [postgres:16, redis:7-alpine]

package:
  script: docker build -t registry/fanzone/${SERVICE}:${CI_COMMIT_SHA} .

deploy:
  script: helm upgrade --install fanzone ./helm/fanzone -f values-${ENV}.yaml
```

### Observability (Dynatrace)

- Distributed tracing: Spring Cloud Sleuth / Micrometer Tracing propagates traceId across REST, Kafka, WebSocket
- Metrics: Micrometer → Dynatrace (request latency, error rates, Kafka consumer lag)
- Logging: Structured JSON logs with traceId in MDC, shipped to Dynatrace Log Monitoring
- Alerting: Match thread latency > 3s, Kafka consumer lag > 1000, error rate > 5%
