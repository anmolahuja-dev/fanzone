# Implementation Plan: Fanzone MVP

## Overview

This plan implements the Fanzone MVP as a Java/Spring Boot microservices backend with PostgreSQL, Redis, Apache Kafka, AWS S3, and Spring WebSocket. The system is built as a multi-module Maven project with 8 services (auth, feed, post, match-thread, reputation, notification, moderation, and a shared common library). Development follows a 3-month timeline: Month 1 (infrastructure, auth, feed, posts, comments), Month 2 (match threads, reputation, notifications, moderation), Month 3 (integration, polish, load testing). Property-based tests use jqwik to verify the 24 correctness properties defined in the design.

## Tasks

- [x] 1. Set up project infrastructure and common library
  - [x] 1.1 Create multi-module Maven project structure
    - Initialize parent POM with Spring Boot 3.x parent, Java 21
    - Create modules: `fanzone-common`, `auth-service`, `feed-service`, `post-service`, `match-thread-service`, `reputation-service`, `notification-service`, `moderation-service`
    - Configure shared dependencies in parent POM: Spring Boot Starter Web, Spring Boot Starter Data JPA, Spring Boot Starter Validation, Spring Kafka, Spring Data Redis, Lombok, MapStruct, jqwik (test)
    - Add Dockerfile per service (multi-stage build: Maven build → JRE runtime)
    - _Requirements: 18.1, 18.2_

  - [x] 1.2 Set up Docker Compose for local development
    - Create `docker-compose.yml` with PostgreSQL 16, Redis 7, Kafka (Confluent 7.5), Zookeeper
    - Configure database initialization script to create schemas per service
    - Add health checks for all infrastructure containers
    - Create `docker-compose.infra.yml` for infrastructure-only mode (services run on host)
    - _Requirements: N/A (infrastructure)_

  - [x] 1.3 Create fanzone-common shared library
    - Implement shared DTOs: `CursorPage<T>`, `ErrorResponse`
    - Implement Kafka event records: `PostUpvotedEvent`, `CommentUpvotedEvent`, `CommentCreatedEvent`, `GoalScoredEvent`, `MatchStatusChangedEvent`, `ReputationChangedEvent`, `FeedInvalidationEvent`
    - Implement exception hierarchy: `FanzoneException`, `ValidationException`, `AuthenticationException`, `ForbiddenException`, `NotFoundException`, `ConflictException`, `RateLimitException`, `BusinessRuleException`
    - Implement `GlobalExceptionHandler` (`@ControllerAdvice`) mapping exceptions to `ErrorResponse`
    - Implement `UserPrincipal` and JWT utility classes
    - Add enums: `PostType`, `MatchPhase`, `ReputationLevel`, `ReputationEvent`, `NotificationType`
    - _Requirements: 11.8, 11.9, 11.10, 11.11_

  - [x] 1.4 Set up PostgreSQL schema with Liquibase
    - Create Liquibase changelog master file per service
    - Implement all 18 tables as defined in design (users, clubs, players, user_favorite_players, user_interests, posts, poll_options, poll_votes, comments, comment_upvotes, post_upvotes, matches, player_ratings, follows, notifications, notification_preferences, reports, match_thread_comments, goal_reactions, activity_feed)
    - Create the `compute_feed_score()` PostgreSQL function
    - Add CHECK constraints for reputation >= 0, rating 1-10, nesting_level <= 3, status enum values
    - Create indexes as defined in design for feed, comments, follows, activity_feed
    - _Requirements: 5.1, 7.6, 9.5, 11.12_

  - [x] 1.5 Configure Spring Security and JWT infrastructure
    - Create `SecurityConfig` with JWT filter chain (stateless session, CORS, CSRF disabled for API)
    - Implement `JwtTokenProvider`: generate access token (1hr expiry) and refresh token (7-day expiry)
    - Implement `JwtAuthenticationFilter` extracting principal from Authorization header
    - Configure public endpoints (auth routes, health checks) vs authenticated endpoints
    - _Requirements: 2.5, 2.6_

  - [x] 1.6 Configure Kafka infrastructure
    - Create Kafka producer configuration with JSON serializer
    - Create Kafka consumer configuration with consumer groups per service
    - Define topic names as constants in common library
    - Configure Dead Letter Queue (DLQ) topics: `{topic}.dlq` for failed messages
    - Implement retry template: 3 retries with exponential backoff before DLQ
    - _Requirements: N/A (infrastructure)_

  - [x] 1.7 Configure Redis
    - Set up Spring Data Redis with Lettuce client
    - Create Redis configuration for connection pooling
    - Implement cache key naming conventions: `feed:{userId}:{tab}`, `rate:{matchId}:{userId}`
    - _Requirements: N/A (infrastructure)_

- [x] 2. Implement authentication and session management (auth-service)
  - [x] 2.1 Implement AuthRepository (JPA)
    - Create `UserEntity` JPA entity mapping to `users` table
    - Create `UserRepository` extending `JpaRepository` with `findByEmail()`, `findByUsername()`
    - Implement password hashing with BCrypt (Spring Security `PasswordEncoder`)
    - _Requirements: 1.1, 2.1_

  - [x] 2.2 Implement AuthService with validation and lockout
    - Create `AuthService` with `register()`, `login()`, `oauthLogin()`, `refreshToken()`
    - Email validation: exactly one `@` followed by domain with at least one dot
    - Password validation: 8-128 chars, 1 uppercase, 1 lowercase, 1 digit
    - Account lockout: increment `failed_login_attempts` on failure, lock for 15 min after 5 consecutive failures
    - Session validity: reject tokens where `lastActiveAt` exceeds 7 days
    - Generic error messages: never reveal which field (email/password) is incorrect
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 2.3 Implement OAuth2 integration (Google + Apple)
    - Create `GoogleOAuthClient` using Spring WebClient to verify Google ID tokens
    - Create `AppleOAuthClient` to verify Apple Sign In tokens (JWT validation with Apple public keys)
    - Map OAuth profile data (email, display name) to user account creation/login
    - Handle provider unavailability with appropriate error response
    - _Requirements: 1.2, 1.3, 1.6, 2.2, 2.3, 2.7_

  - [x] 2.4 Implement email verification flow
    - Generate verification token (UUID) with 24-hour expiry on registration
    - Send verification email (placeholder for SMTP service in MVP)
    - Implement verification endpoint: validate token, mark `email_verified = true`
    - Enforce limited access for unverified users (read-only, no post/comment creation)
    - _Requirements: 1.1, 1.7_

  - [x] 2.5 Implement AuthController REST endpoints
    - `POST /api/v1/auth/register` — with `@Valid RegisterRequest`
    - `POST /api/v1/auth/login` — with `@Valid LoginRequest`
    - `POST /api/v1/auth/oauth/google` — with `@Valid OAuthRequest`
    - `POST /api/v1/auth/oauth/apple` — with `@Valid OAuthRequest`
    - `POST /api/v1/auth/refresh` — with refresh token in body
    - `POST /api/v1/auth/logout` — invalidate refresh token
    - `POST /api/v1/auth/verify-email` — with verification token
    - `POST /api/v1/auth/resend-verification` — rate limited
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

  - [x]* 2.6 Write property tests for email validation (Property 1)
    - Generate random strings: accept iff exactly one `@` + domain with at least one dot
    - **Validates: Requirements 1.5**

  - [x]* 2.7 Write property tests for session validity (Property 23)
    - Generate random timestamps relative to last activity: valid iff within 7 days
    - **Validates: Requirements 2.5, 2.6**

  - [x]* 2.8 Write property tests for account lockout (Property 24)
    - Generate random failed attempt counts (0-10): locked iff attempts >= 5, lockout = 15 min
    - **Validates: Requirements 2.4**

- [ ] 3. Implement onboarding (clubs, players, interests)
  - [ ] 3.1 Implement onboarding endpoints
    - `GET /api/v1/clubs` — searchable list with `?search=` query param, returns name + logo
    - `GET /api/v1/clubs/{clubId}/players` — players for selected club
    - `PUT /api/v1/users/me/club` — set favorite club (required, validates club exists)
    - `PUT /api/v1/users/me/players` — set 1-5 favorite players (optional, validates belong to club)
    - `PUT /api/v1/users/me/interests` — set interests (at least 1 required from: matchday, transfers, tactics, memes, news)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2, 4.3, 4.4, 4.5, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_

  - [ ] 3.2 Seed club and player data
    - Create Liquibase changeset with initial club data (top 20 European clubs)
    - Create Liquibase changeset with player data per club (squad lists)
    - Include club theme colors (primary_color, secondary_color) for club-adaptive theming
    - _Requirements: 3.2, 20.1, 20.4, 20.5_

- [ ] 4. Checkpoint — Ensure all tests pass
  - Run `mvn verify` across all modules
  - Verify auth-service starts and serves health check
  - Verify Docker Compose brings up all infrastructure
  - Ask the user if questions arise

- [ ] 5. Implement Home Feed with scoring engine (feed-service)
  - [ ] 5.1 Implement FeedScorer
    - Create `FeedScorer.computeScore()` as a pure function (no side effects)
    - Apply scoring: +100 same club, +30 trusted/expert author, +20 recent activity (2hr), +15 high comments (>=10), -100 toxic reports (>=3)
    - Ensure determinism: identical inputs always produce identical output
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 5.2 Write property tests for feed scoring (Property 2)
    - Generate random posts with varying clubId, author levels, timestamps, comment counts, report counts
    - Verify determinism and correct additive scoring
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6**

  - [ ] 5.3 Implement FeedRepository with pagination
    - Create `FeedRepository` with cursor-based pagination (20 posts per page)
    - "For You": query all posts, order by `compute_feed_score()` DESC, then `created_at` DESC
    - "Club": filter by `club_id = user.favoriteClubId`, order by `created_at` DESC
    - "Following": filter by `user_id IN (followed users)`, order by `created_at` DESC
    - _Requirements: 5.1, 5.8, 5.9, 5.10, 5.12_

  - [ ] 5.4 Implement Redis feed caching
    - Cache "For You" feed per user in Redis sorted set (score = feed score, member = post ID)
    - TTL: 5 minutes per cached feed page
    - Invalidate on `FeedInvalidationEvent` consumption from Kafka
    - Fallback to DB query on cache miss
    - _Requirements: 5.1, 5.12_

  - [ ]* 5.5 Write property tests for Club tab filtering (Property 3)
    - Generate random post sets across multiple clubs; verify only matching clubId posts appear, ordered by createdAt desc
    - **Validates: Requirements 5.9**

  - [ ]* 5.6 Write property tests for Following tab filtering (Property 4)
    - Generate random post sets + follow graphs; verify only followed authors' posts appear, ordered by createdAt desc
    - **Validates: Requirements 5.10, 14.3**

  - [ ] 5.7 Implement FeedController
    - `GET /api/v1/feeds/for-you` — cursor pagination, returns `CursorPage<PostDto>`
    - `GET /api/v1/feeds/club` — filters by user's club
    - `GET /api/v1/feeds/following` — filters by followed users
    - Include author info (username, reputationLevel, profilePicture) in response DTO
    - _Requirements: 5.7, 5.8, 5.9, 5.10, 5.11, 5.12, 5.13_

- [ ] 6. Implement Post creation and display (post-service)
  - [ ] 6.1 Implement PostValidator
    - Text posts: 1-2000 characters
    - Poll posts: question 1-200 chars, 2-4 options each 1-100 chars
    - Match analysis: 1-10000 characters
    - Reject empty content with no image
    - Validate image format (JPEG, PNG, WebP) and size (<=10MB) via content-type and Content-Length
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6, 6.7_

  - [ ]* 6.2 Write property tests for post content validation (Property 5)
    - Generate random strings of varying lengths; verify acceptance iff within character bounds per type
    - **Validates: Requirements 6.1, 6.3, 6.4, 6.6**

  - [ ] 6.3 Implement PostService and S3 image upload
    - Create `PostService` for post creation with validation
    - Implement S3 image upload with presigned URL generation
    - Associate each post with user's `favorite_club_id`
    - Publish `FeedInvalidationEvent` on post creation
    - _Requirements: 6.2, 6.5, 6.8_

  - [ ]* 6.4 Write property tests for post club association (Property 6)
    - Generate random users with clubs and post content; verify post.clubId always equals author's favoriteClubId
    - **Validates: Requirements 6.5**

  - [ ] 6.5 Implement PostController
    - `POST /api/v1/posts` — create post (multipart for image, JSON for text/poll/analysis)
    - `GET /api/v1/posts/{postId}` — get single post with comments count
    - `DELETE /api/v1/posts/{postId}` — delete own post only
    - `POST /api/v1/posts/{postId}/upvotes` — upvote (idempotent, prevent self-upvote)
    - `DELETE /api/v1/posts/{postId}/upvotes` — remove upvote
    - `POST /api/v1/posts/{postId}/reports` — report (one per user per post)
    - Publish `PostUpvotedEvent` to Kafka on upvote
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

- [ ] 7. Implement Comments and Replies system (post-service)
  - [ ] 7.1 Implement CommentService with nesting and validation
    - Create `CommentService` with `createComment()`, `createReply()`, `upvote()`, `removeUpvote()`, `report()`
    - Enforce nesting depth max 3 levels (redirect deeper replies to level 3)
    - Validate content: 1-1000 characters
    - Enforce one upvote per user per comment (idempotent via composite PK)
    - Enforce one report per user per comment (idempotent via unique constraint)
    - Prevent self-upvoting (return 403)
    - Publish `CommentUpvotedEvent` and `CommentCreatedEvent` to Kafka
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 7.6, 7.7, 7.8, 7.9_

  - [ ]* 7.2 Write property tests for comment nesting depth (Property 7)
    - Generate random comment trees of varying depth; verify nestingLevel never exceeds 3
    - **Validates: Requirements 7.2, 7.6, 7.7**

  - [ ]* 7.3 Write property tests for upvote idempotence (Property 8)
    - Generate random user-comment pairs with repeated upvote actions; verify count increments at most once
    - **Validates: Requirements 7.3, 7.5**

  - [ ]* 7.4 Write property tests for self-upvote prevention (Property 17)
    - Generate random user-content ownership combos; verify self-upvote always rejected (403), no score change
    - **Validates: Requirements 11.15**

  - [ ] 7.5 Implement Comment endpoints
    - `GET /api/v1/posts/{postId}/comments` — paginated, threaded (include nested replies)
    - `POST /api/v1/posts/{postId}/comments` — create comment on post
    - `POST /api/v1/comments/{commentId}/replies` — reply to comment
    - `POST /api/v1/comments/{commentId}/upvotes` — upvote comment
    - `DELETE /api/v1/comments/{commentId}/upvotes` — remove comment upvote
    - `POST /api/v1/comments/{commentId}/reports` — report comment
    - Parse @mentions from content, publish notification events for valid usernames
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [ ] 8. Implement Reputation system (reputation-service)
  - [ ] 8.1 Implement ReputationService
    - Create `ReputationService` with `adjustReputation()` and `computeLevel()`
    - Implement all event point values: POST_UPVOTED +2, COMMENT_UPVOTED +1, HELPFUL_BADGE +10, ACCURATE_PREDICTION +20, POST_UPVOTE_REMOVED -2, COMMENT_UPVOTE_REMOVED -1, COMMENT_REMOVED_BY_MOD -20, TOXIC_CONTENT_CONFIRMED -50, FAKE_NEWS_CONFIRMED -100
    - Clamp score to minimum 0 (floor invariant)
    - Level assignment: 0-99 Rookie, 100-499 Active, 500-1999 Trusted, 2000+ Club Expert
    - Update `reputation_level` column immediately on threshold crossing
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 11.10, 11.11, 11.12, 11.13, 11.14_

  - [ ] 8.2 Implement Kafka consumer for reputation events
    - Consume from `fanzone.reputation.events` topic
    - Process events transactionally (DB update + level check in single transaction)
    - Publish `ReputationChangedEvent` to `fanzone.notification.events` if level changes
    - DLQ: failed messages → `fanzone.reputation.events.dlq`
    - _Requirements: 11.1, 11.12, 11.14_

  - [ ]* 8.3 Write property tests for reputation events (Property 14)
    - Generate random sequences of all event types; verify final score equals sum of points clamped to >= 0
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.13**

  - [ ]* 8.4 Write property tests for reputation floor (Property 15)
    - Generate heavy negative event sequences; verify score never goes below 0
    - **Validates: Requirements 11.12**

  - [ ]* 8.5 Write property tests for reputation level assignment (Property 16)
    - Generate random integers 0-5000+; verify correct level assigned per threshold boundaries
    - **Validates: Requirements 11.8, 11.9, 11.10, 11.11, 11.14**

- [ ] 9. Checkpoint — Ensure all tests pass
  - Run `mvn verify` across all modules
  - Verify post-service, feed-service, reputation-service start and serve health checks
  - Verify Kafka event flow: post upvote → reputation update → level change
  - Ask the user if questions arise

- [ ] 10. Implement Follow system and Profile (post-service)
  - [ ] 10.1 Implement FollowRepository and FollowService
    - Create `FollowRepository` with `existsByFollowerAndFollowed()`, count queries
    - Implement `follow()`: prevent self-follow (403), idempotent (ON CONFLICT DO NOTHING)
    - Implement `unfollow()`: delete if exists, no error if not following
    - `getFollowerCount()`, `getFollowingCount()`, `isFollowing()`
    - Do NOT publish follow notification event (anti-follower-farming)
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

  - [ ]* 10.2 Write property tests for follow integrity (Property 20)
    - Generate random user pairs including self-pairs and duplicate attempts
    - Verify self-follow prevented, at most one relationship, idempotent creation
    - **Validates: Requirements 14.6, 14.7**

  - [ ] 10.3 Implement Profile endpoints
    - `GET /api/v1/users/me` — current user profile (username, club, reputation, level, avatar)
    - `PUT /api/v1/users/me` — update username
    - `POST /api/v1/users/me/avatar` — upload profile picture to S3 (JPEG/PNG, <=5MB)
    - `GET /api/v1/users/{userId}` — other user profile
    - `GET /api/v1/users/{userId}/posts` — paginated posts by user (20 per page, cursor)
    - `POST /api/v1/users/{userId}/follows` — follow user
    - `DELETE /api/v1/users/{userId}/follows` — unfollow user
    - _Requirements: 14.1, 14.2, 14.4, 14.5, 14.6, 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7_

- [ ] 11. Implement Match Thread system (match-thread-service)
  - [ ] 11.1 Implement MatchThreadService with phase management
    - Create `MatchThreadService` with `getMatchThread()`, `submitComment()`, `submitPlayerRating()`, `submitGoalReaction()`, `calculateMotm()`
    - Enforce monotonic phase transitions: scheduled → live → finished (never backward)
    - Implement rate limiter: max 20 comments per user per 60-second rolling window (Redis sorted set)
    - Implement 48-hour post-match timeout (thread becomes read-only, reject comments with 422)
    - _Requirements: 9.1, 9.8, 10.1, 10.7_

  - [ ]* 11.2 Write property tests for match phase monotonicity (Property 9)
    - Generate random sequences of status change attempts; verify only forward transitions succeed
    - **Validates: Requirements 9.1, 10.1**

  - [ ]* 11.3 Write property tests for rate limiting (Property 11)
    - Generate random comment timestamps within windows; verify max 20 per 60s, 21st rejected
    - **Validates: Requirements 9.8**

  - [ ]* 11.4 Write property tests for post-match timeout (Property 13)
    - Generate random timestamps relative to finished_at; verify comments accepted iff within 48h
    - **Validates: Requirements 10.7**

  - [ ] 11.5 Implement player ratings and MOTM calculation
    - `submitPlayerRating()`: integer 1-10, one per user per player per match, upsert on resubmit (UNIQUE constraint)
    - `calculateMotm()`: highest average rating when >= 10 total ratings; top-3 ordered by avg desc, ties broken by count desc
    - _Requirements: 9.5, 10.3, 10.4, 10.6_

  - [ ]* 11.6 Write property tests for player rating bounds (Property 10)
    - Generate random integers and player-user-match combos; verify accepted iff 1-10, upsert behavior
    - **Validates: Requirements 9.5**

  - [ ]* 11.7 Write property tests for MOTM calculation (Property 12)
    - Generate random rating sets; verify MOTM = highest avg when >=10 ratings, correct tie-breaking, null when <10
    - **Validates: Requirements 10.3, 10.4, 10.6**

  - [ ] 11.8 Implement WebSocket (STOMP) for live match threads
    - Configure Spring WebSocket with STOMP protocol
    - Subscription endpoint: `/topic/match-threads/{matchId}`
    - Broadcast new comments to all subscribers within 3 seconds
    - Use Redis pub/sub for multi-instance WebSocket broadcasting
    - Handle disconnection: client-side reconnect, replay missed messages from Redis sorted set (last 5 minutes)
    - Implement goal reaction prompts: broadcast `GoalScoredEvent` with 30-second display window
    - _Requirements: 8.4, 8.5, 9.1, 9.2, 9.3, 9.4, 9.7_

  - [ ] 11.9 Implement Match Thread REST endpoints
    - `GET /api/v1/matches` — list upcoming/live matches for user's club
    - `GET /api/v1/matches/{matchId}` — match details (clubs, score, status, time)
    - `GET /api/v1/match-threads/{matchId}/comments` — paginated comments (cursor)
    - `POST /api/v1/match-threads/{matchId}/comments` — post comment (rate limited)
    - `POST /api/v1/match-threads/{matchId}/ratings` — submit player rating
    - `POST /api/v1/match-threads/{matchId}/reactions` — submit goal reaction
    - `GET /api/v1/match-threads/{matchId}/motm` — get MOTM results
    - _Requirements: 8.1, 8.2, 8.3, 9.1, 9.5, 9.6, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

- [ ] 12. Implement Toxicity Shield (moderation-service)
  - [ ] 12.1 Implement OpenAI moderation client
    - Create `OpenAiModerationClient` using Spring WebClient
    - Call OpenAI moderation endpoint with 2-second timeout
    - Return `ToxicityResult` with `isToxic`, `reason`, `confidence`
    - On timeout: return safe result, publish to `fanzone.moderation.requests` for async review
    - Configure Resilience4j circuit breaker for OpenAI calls
    - _Requirements: 12.1, 12.5, 12.6, 12.7_

  - [ ] 12.2 Implement moderation REST endpoint
    - `POST /api/v1/moderation/analyze` — called by post-service before publishing content
    - Return `ToxicityResult` to calling service
    - Do NOT flag football performance criticism without personal attacks
    - On AI failure/timeout: return safe immediately, queue for background review
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8_

  - [ ] 12.3 Implement async moderation consumer
    - Consume from `fanzone.moderation.requests` topic for queued reviews
    - Re-analyze content with longer timeout (10s)
    - Update post/comment `is_flagged` if toxic
    - Increment `toxic_report_count` and publish reputation event if confirmed toxic
    - _Requirements: 12.7_

- [ ] 13. Implement Notifications and Activity Feed (notification-service)
  - [ ] 13.1 Implement Firebase Cloud Messaging (FCM) client
    - Create `FcmClient` using Firebase Admin SDK for Java
    - Implement push notification dispatch with retry (3x exponential backoff)
    - Format notifications: match alert (club names + time), goal (scorer + score + minute), reply (username + 100 char preview), mention (username + 100 char preview)
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.8_

  - [ ] 13.2 Implement NotificationService with preference enforcement
    - Create `NotificationService` with `shouldSend()` checking user preferences
    - Default all notification types to enabled on account creation (via Liquibase default)
    - Respect per-type toggle: match_alerts, goals, replies, mentions
    - Never deliver disabled notification types
    - Truncate preview text to exactly 100 characters
    - _Requirements: 13.5, 13.6, 13.7_

  - [ ] 13.3 Implement Kafka consumers for notification events
    - Consume from `fanzone.notification.events` topic
    - Process: `CommentCreatedEvent` → reply/mention notifications
    - Process: `GoalScoredEvent` → goal push notifications to club fans
    - Process: `MatchStatusChangedEvent` → match alert notifications (15 min before)
    - Process: `ReputationChangedEvent` → activity feed entry for level changes
    - DLQ: `fanzone.notification.events.dlq`
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [ ]* 13.4 Write property tests for notification preference enforcement (Property 18)
    - Generate random events × random preference configs; verify delivery iff type enabled
    - **Validates: Requirements 13.5, 13.6**

  - [ ]* 13.5 Write property tests for preview truncation (Property 19)
    - Generate random strings 0-5000 chars; verify preview <= 100 chars, truncated at exactly 100 when over
    - **Validates: Requirements 13.3, 13.4**

  - [ ] 13.6 Implement Activity Feed endpoints
    - `GET /api/v1/activity-feeds` — paginated (20 per page, cursor), reverse chronological
    - `PUT /api/v1/activity-feeds/read` — mark all as read, reset unread badge
    - Include activity types: mentions, replies, match alerts, reputation changes
    - Return `unreadCount` in response header for badge display
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6, 21.7_

  - [ ] 13.7 Implement notification preferences endpoints
    - `GET /api/v1/notification-preferences` — get current preferences
    - `PUT /api/v1/notification-preferences` — update per-type toggles
    - _Requirements: 13.5, 13.6, 13.7_

  - [ ]* 13.8 Write property tests for activity feed ordering (Property 21)
    - Generate random activity items with random timestamps; verify descending order
    - **Validates: Requirements 21.1**

  - [ ]* 13.9 Write property tests for activity badge count (Property 22)
    - Generate random sequences of arrivals and feed-opens; verify badge = count since last open, reset to 0 on open
    - **Validates: Requirements 21.5**

- [ ] 14. Checkpoint — Ensure all tests pass
  - Run `mvn verify` across all modules
  - Verify all services start and serve health checks
  - Verify Kafka event flows end-to-end: post → moderation → publish → reputation → notification
  - Verify WebSocket match thread with local Kafka + Redis
  - Ask the user if questions arise

- [ ] 15. Implement Club Hub and remaining features
  - [ ] 15.1 Implement Club Hub endpoint
    - `GET /api/v1/club-hubs/{clubId}` — returns aggregated club data
    - Sections: news, matchday, discussions, transfers, memes, tactical (10 most recent posts per section, filtered by post content/tags)
    - Include live/upcoming match thread info when match within 24h
    - Return club name, logo, active member count (users with club as favorite, active in last 7 days)
    - Sort posts by recency within each section
    - Return empty arrays for sections with no content
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7_

  - [ ] 15.2 Implement club theme endpoint
    - `GET /api/v1/clubs/{clubId}/theme` — returns club colors for client theming
    - Return `primaryColor`, `secondaryColor` (hex strings)
    - Return null/default if club has no defined theme
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 20.1, 20.2, 20.3, 20.4, 20.5, 20.6_

  - [ ] 15.3 Implement settings endpoints
    - `PUT /api/v1/users/me/theme` — set theme preference (light, dark, system)
    - `PUT /api/v1/users/me/club` — change favorite club (updates theme context)
    - _Requirements: 19.3, 19.6, 20.6_

- [ ] 16. Integration, CI/CD, and deployment setup
  - [ ] 16.1 Configure GitLab CI/CD pipeline
    - Create `.gitlab-ci.yml` with stages: build, test, package, deploy
    - Build stage: `mvn clean compile` per service
    - Test stage: `mvn verify` with Testcontainers (PostgreSQL, Redis, Kafka)
    - Package stage: Docker build + push to container registry
    - Deploy stage: Helm upgrade to target environment
    - _Requirements: N/A (infrastructure)_

  - [ ] 16.2 Create Helm charts
    - Create umbrella Helm chart in `helm/fanzone/`
    - Sub-charts per service with Deployment, Service, ConfigMap, HPA
    - External secrets reference (vault integration for DB credentials, JWT secret, OpenAI key, FCM key)
    - Ingress configuration for API gateway
    - Values files: `values-dev.yaml`, `values-staging.yaml`
    - _Requirements: N/A (infrastructure)_

  - [ ] 16.3 Wire all services end-to-end
    - Verify inter-service communication: post-service → moderation-service (REST with circuit breaker)
    - Verify Kafka event flows: post events → reputation, notification, feed invalidation
    - Verify WebSocket broadcasting across multiple match-thread-service instances (Redis pub/sub)
    - Verify feed cache invalidation on new posts/comments/upvotes
    - Verify club theme changes propagate correctly
    - _Requirements: 5.3, 11.14, 14.3, 20.6_

  - [ ] 16.4 Write integration tests with Testcontainers
    - Auth flow: register → verify email → login → JWT token → access protected endpoint
    - Post flow: create post → appears in feed → comment → upvote → reputation update (Kafka)
    - Match thread flow: join → live comments (WebSocket) → player ratings → MOTM display
    - Follow flow: follow user → posts appear in Following feed
    - Moderation flow: toxic content → warning → proceed → flagged
    - _Requirements: 1.1, 3.1, 5.1, 9.1, 10.3, 14.3_

- [ ] 17. Load testing and final polish
  - [ ] 17.1 Write K6 load tests
    - Match thread concurrent users: 1000 users posting comments simultaneously
    - Feed endpoint: 500 concurrent requests, p95 < 200ms
    - Auth endpoint: 100 concurrent logins
    - Rate limiter validation under load
    - _Requirements: N/A (performance validation)_

  - [ ] 17.2 Performance optimization
    - Add database connection pooling (HikariCP tuning)
    - Optimize feed query with proper indexing and query plans
    - Redis pipeline for batch feed score calculations
    - Kafka producer batching for high-throughput events
    - _Requirements: 5.1, 9.2_

  - [ ] 17.3 Observability setup
    - Configure Dynatrace OneAgent integration
    - Set up Micrometer metrics: request latency, error rates, Kafka consumer lag
    - Configure structured JSON logging with traceId in MDC
    - Set up alerting: match thread latency > 3s, consumer lag > 1000, error rate > 5%
    - _Requirements: N/A (operations)_

- [ ] 18. Final checkpoint — Ensure all tests pass
  - Run `mvn verify` across all modules
  - Run K6 load tests, verify p95 latency targets
  - Verify all services deploy to K8s via Helm
  - Verify end-to-end user journey in staging environment
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional property-based tests and can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Checkpoints (tasks 4, 9, 14, 18) ensure incremental validation
- Property tests use jqwik (JUnit 5 property-based testing for Java) to verify the 24 correctness properties
- Unit tests use JUnit 5 + Mockito for service-level logic
- Integration tests use Testcontainers for real PostgreSQL, Redis, and Kafka instances
- All Kafka consumers implement retry (3x exponential backoff) + DLQ pattern
- Inter-service REST calls use Resilience4j circuit breaker + timeout
- Secrets (DB password, JWT secret, OpenAI API key, FCM credentials) are injected via environment variables / K8s secrets (never hardcoded)
- Development follows Month 1 (tasks 1-9), Month 2 (tasks 10-14), Month 3 (tasks 15-18) timeline

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["1.4", "1.5", "1.6", "1.7"] },
    { "id": 3, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 4, "tasks": ["2.4", "2.5", "2.6", "2.7", "2.8"] },
    { "id": 5, "tasks": ["3.1", "3.2"] },
    { "id": 6, "tasks": ["5.1", "5.2"] },
    { "id": 7, "tasks": ["5.3", "5.4", "5.5", "5.6"] },
    { "id": 8, "tasks": ["5.7", "6.1", "6.2"] },
    { "id": 9, "tasks": ["6.3", "6.4", "6.5"] },
    { "id": 10, "tasks": ["7.1", "7.2", "7.3", "7.4"] },
    { "id": 11, "tasks": ["7.5", "8.1"] },
    { "id": 12, "tasks": ["8.2", "8.3", "8.4", "8.5"] },
    { "id": 13, "tasks": ["10.1", "10.2", "10.3"] },
    { "id": 14, "tasks": ["11.1", "11.2", "11.3", "11.4"] },
    { "id": 15, "tasks": ["11.5", "11.6", "11.7"] },
    { "id": 16, "tasks": ["11.8", "11.9"] },
    { "id": 17, "tasks": ["12.1", "12.2", "12.3"] },
    { "id": 18, "tasks": ["13.1", "13.2", "13.3"] },
    { "id": 19, "tasks": ["13.4", "13.5", "13.6", "13.7", "13.8", "13.9"] },
    { "id": 20, "tasks": ["15.1", "15.2", "15.3"] },
    { "id": 21, "tasks": ["16.1", "16.2"] },
    { "id": 22, "tasks": ["16.3", "16.4"] },
    { "id": 23, "tasks": ["17.1", "17.2", "17.3"] }
  ]
}
```
