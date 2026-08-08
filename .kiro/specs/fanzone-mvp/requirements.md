# Requirements Document

## Introduction

Fanzone is a football-first social platform that replaces fragmented, toxic football discussion spaces (primarily X/Twitter) with a dedicated community built around club loyalty, matchday engagement, and quality contributions. The platform exposes a REST API backend (with WebSocket support for real-time features) consumed by mobile and web clients. The MVP focuses on authentication, personalized feeds, posts, comments, live match threads, a reputation system, and AI-powered toxicity moderation.

## Glossary

- **App**: The Fanzone platform (REST API backend + client applications)
- **Client**: Any consumer of the Fanzone API (mobile app, web app, etc.)
- **User**: A registered person who has completed the sign-up flow and selected a club
- **Club**: A football club entity stored in the database that users can follow
- **Club_Hub**: A dedicated view aggregating all content, match threads, and discussions for a specific club
- **Feed_Engine**: The backend component responsible for scoring and ranking posts in the Home Feed
- **Post**: User-generated content in the form of text, image, poll, or match analysis
- **Comment**: A reply to a Post, supporting nesting, upvotes, and mentions
- **Match_Thread**: A real-time discussion space tied to a specific football match with pre-match, live, and post-match phases
- **Reputation_System**: The server-side component that calculates and maintains user reputation scores based on community interactions
- **Toxicity_Shield**: The AI-powered moderation component that warns users before they post potentially harmful content
- **Auth_Service**: The authentication component supporting email/password, Google OAuth2, and Apple sign-in via Spring Security
- **Realtime_Service**: The WebSocket-based component providing live updates for match threads and notifications
- **Player_Rating**: A user-submitted rating (1-10) for a player's performance in a specific match
- **MOTM**: Man of the Match — the community-voted best player in a match
- **Reputation_Level**: A tier assigned to a user based on their accumulated reputation score (Rookie, Active, Trusted, Club Expert)
- **Follow_Relationship**: A directional connection from one user to another, used to curate the Following feed tab
- **Interest_Preference**: A user's selected content interests (Matchday, Transfers, Tactics, Memes, News) used to personalize feed sections
- **Activity_Feed**: The in-app notifications/activity screen showing mentions, replies, and match alerts
- **Club_Theme**: The dynamic visual adaptation of accent colors and highlights based on the user's favorite club

## Requirements

### Requirement 1: User Registration

**User Story:** As a new user, I want to sign up using my email, Google account, or Apple account, so that I can quickly create an account and start using Fanzone.

#### Acceptance Criteria

1. WHEN a user initiates sign-up with a valid email and a password that is between 8 and 128 characters and contains at least one uppercase letter, one lowercase letter, and one digit, THE Auth_Service SHALL create a new user account and send an email verification link that expires after 24 hours
2. WHEN a user initiates sign-up via Google OAuth2, THE Auth_Service SHALL authenticate the user through Google and create a new account with the returned profile data (display name and email)
3. WHEN a user initiates sign-up via Apple Sign In, THE Auth_Service SHALL authenticate the user through Apple and create a new account with the returned profile data (display name and email)
4. IF a user attempts sign-up with an email already associated with an existing account, THEN THE Auth_Service SHALL return a generic error indicating the email is already registered and shall not reveal whether the existing account uses email, Google, or Apple sign-in
5. IF a user provides an email that does not contain exactly one @ symbol followed by a domain with at least one dot, THEN THE Auth_Service SHALL return a validation error before processing the request
6. IF the Google or Apple OAuth provider is unavailable or returns an error during sign-up, THEN THE Auth_Service SHALL return an error indicating the sign-in service is temporarily unavailable and allow the client to retry or offer a different sign-up method
7. WHILE a user's email is not yet verified, THE Auth_Service SHALL allow the user to access the API in a limited state (read-only) and the client SHALL display a prompt to verify their email before they can create posts or comments

### Requirement 2: User Login

**User Story:** As a returning user, I want to log in using my existing credentials, so that I can access my personalized feed and content.

#### Acceptance Criteria

1. WHEN a registered user provides valid email and password credentials, THE Auth_Service SHALL authenticate the user and navigate to the Home Feed
2. WHEN a registered user initiates login via Google OAuth, THE Auth_Service SHALL authenticate the user and navigate to the Home Feed
3. WHEN a registered user initiates login via Apple Sign In, THE Auth_Service SHALL authenticate the user and navigate to the Home Feed
4. IF a user provides incorrect credentials, THEN THE Auth_Service SHALL display an error message indicating invalid credentials without revealing which field is incorrect, and IF the user fails authentication 5 consecutive times, THEN THE Auth_Service SHALL lock the account for 15 minutes and display a message indicating the lockout duration
5. WHILE a user session is active and the session token has not exceeded 7 days since last activity, THE App SHALL maintain the authenticated state without requiring re-login
6. IF the session token has exceeded 7 days since last activity, THEN THE App SHALL end the authenticated session, navigate the user to the login screen, and display a message indicating the session has expired
7. IF a user initiates login via Google OAuth or Apple Sign In and the external provider is unavailable or returns an error, THEN THE Auth_Service SHALL display an error message indicating the sign-in service is temporarily unavailable and allow the user to retry or choose an alternative login method

### Requirement 3: Club Selection During Onboarding

**User Story:** As a new user, I want to select my favorite football club during onboarding, so that my feed is immediately personalized to content I care about.

#### Acceptance Criteria

1. WHEN a user completes account creation, THE App SHALL present the club selection screen before any other content
2. THE App SHALL display a searchable list of all clubs stored in the database, showing each club's name and logo, and SHALL filter the displayed list to show clubs whose name contains the user's search input
3. WHEN a user selects a club from the list, THE App SHALL visually highlight the selected club and store the selection in the user's `favorite_club_id` field, replacing any previously highlighted selection
4. WHEN a user confirms club selection, THE App SHALL navigate to the favorite players selection screen
5. IF a user attempts to proceed without choosing a club, THEN THE App SHALL display an inline message indicating that club selection is required and SHALL remain on the club selection screen
6. IF the App fails to load the club list due to a network or server error, THEN THE App SHALL display an error message indicating the failure and provide a retry option

### Requirement 4: Favorite Players Selection During Onboarding

**User Story:** As a new user, I want to choose my favorite players after selecting my club, so that I receive relevant player-specific content.

#### Acceptance Criteria

1. WHEN a user completes club selection, THE App SHALL display a scrollable list of players associated with the selected club, showing each player's name and photo
2. WHEN a user taps on one or more players (up to a maximum of 5), THE App SHALL visually indicate the selected players and enable a confirmation action
3. WHEN a user confirms player selection with at least 1 player selected, THE App SHALL store the selected players in the user's profile and navigate to the interest selection screen
4. WHEN a user chooses to skip player selection, THE App SHALL navigate to the interest selection screen without storing player preferences
5. IF the selected club has no players available in the system, THEN THE App SHALL skip the player selection step and navigate the user directly to the interest selection screen

### Requirement 5: Home Feed Display and Ranking

**User Story:** As a user, I want to see a personalized feed ranked by relevance to my club and content quality, so that I find engaging football content without wading through noise.

#### Acceptance Criteria

1. WHEN a user opens the Home Feed, THE Feed_Engine SHALL retrieve and display posts ranked by a composite score, loading 20 posts per page
2. THE Feed_Engine SHALL apply a +100 score modifier to posts associated with the same club as the user's `favorite_club_id`
3. THE Feed_Engine SHALL apply a +30 score modifier to posts authored by users with a Reputation_Level of Trusted or Club Expert
4. THE Feed_Engine SHALL apply a +20 score modifier to posts that have received a new comment or upvote within the last 2 hours
5. THE Feed_Engine SHALL apply a +15 score modifier to posts with 10 or more comments
6. THE Feed_Engine SHALL apply a -100 score modifier to posts that have received 3 or more toxic content reports
7. THE App SHALL display Home Feed tabs for "For You", "Club", and "Following" views, with "For You" as the default tab on launch
8. WHEN a user selects the "For You" tab, THE Feed_Engine SHALL display posts from all clubs ranked by the composite scoring algorithm
9. WHEN a user selects the "Club" tab, THE Feed_Engine SHALL display only posts associated with the user's favorite club, ranked by recency
10. WHEN a user selects the "Following" tab, THE Feed_Engine SHALL display only posts authored by users the current user follows, ranked by recency
11. THE App SHALL display feed content sections categorized as Trending, Matchday, News, Tactics, Memes, and Transfers
12. WHEN a user scrolls to the bottom of the loaded posts, THE Feed_Engine SHALL load the next page of 20 ranked posts
13. IF the Feed_Engine fails to load posts due to a network or server error, THEN THE App SHALL display an error message and provide a retry option

### Requirement 6: Post Creation

**User Story:** As a user, I want to create posts of various types (text, image, poll, match analysis), so that I can share my football opinions and content with the community.

#### Acceptance Criteria

1. WHEN a user submits a text post with content between 1 and 2000 characters, THE App SHALL create and publish the post to the feed
2. WHEN a user submits an image post with an attached image file in JPEG, PNG, or WebP format not exceeding 10 MB in size, THE App SHALL upload the image to storage and create the post with the image URL
3. WHEN a user submits a poll post with a question between 1 and 200 characters and 2 to 4 options each between 1 and 100 characters, THE App SHALL create the poll and display it as a voteable post
4. WHEN a user submits a match analysis post with content between 1 and 10000 characters, THE App SHALL create a long-form post supporting bold, italic, and heading formatting
5. THE App SHALL associate each new post with the user's `favorite_club_id` as the post's club context
6. IF a user submits a post with empty content and no image attachment, THEN THE App SHALL display a validation error and prevent submission
7. IF a user attaches an image file that exceeds 10 MB or is not in JPEG, PNG, or WebP format, THEN THE App SHALL display a validation error indicating the file constraint violated and prevent submission
8. IF the image upload to storage fails, THEN THE App SHALL display an error message indicating the upload failed, retain the post content in the editor, and not publish the post

### Requirement 7: Comments and Replies

**User Story:** As a user, I want to comment on posts and reply to other comments, so that I can engage in discussions with other fans.

#### Acceptance Criteria

1. WHEN a user submits a comment on a post with content between 1 and 1000 characters, THE App SHALL create the comment and display it in the post's comment thread ordered by newest first
2. WHEN a user submits a reply to an existing comment with content between 1 and 1000 characters, THE App SHALL create a nested reply and display it indented under the parent comment
3. WHEN a user upvotes a comment they have not already upvoted, THE App SHALL increment the comment's upvote count by 1 and increment the comment author's reputation by 1, and SHALL prevent the same user from upvoting the same comment more than once
4. WHEN a user mentions another user using the @ symbol followed by a valid username that exists in the system, THE App SHALL create a notification for the mentioned user
5. WHEN a user reports a comment, THE App SHALL record the report and flag the comment for moderation review, and SHALL prevent the same user from reporting the same comment more than once
6. THE App SHALL support up to 3 levels of nested replies per comment thread
7. IF a user attempts to submit a reply at nesting level 4 or deeper, THEN THE App SHALL prevent submission and display the reply input at the maximum allowed nesting level (level 3) instead
8. IF a user submits a comment with empty content or content exceeding 1000 characters, THEN THE App SHALL display a validation error and prevent submission
9. IF a user mentions a username using the @ symbol that does not match any existing user, THEN THE App SHALL publish the comment without creating a notification for that mention

### Requirement 8: Match Thread - Pre-Match Phase

**User Story:** As a matchday fan, I want to see upcoming match details and join the match thread before kickoff, so that I can participate in pre-match discussion.

#### Acceptance Criteria

1. WHEN a match involving the user's favorite club is scheduled within 24 hours, THE App SHALL display the match in the Matchday feed section with a countdown timer that updates every minute
2. THE App SHALL display the home club name, away club name, and scheduled start time for each upcoming match
3. WHEN a user taps "Join Match Thread" on an upcoming match, THE App SHALL navigate the user to the Match_Thread view for that match
4. WHILE a match is in pre-match phase (more than 0 minutes to kickoff), THE Match_Thread SHALL accept and display user comments to all connected clients within 3 seconds of submission via the Realtime_Service
5. IF the Realtime_Service WebSocket connection is lost while a user is in a pre-match Match_Thread, THEN THE Client SHALL display a connection status indicator and attempt to reconnect automatically at intervals of 5 seconds for a maximum of 5 attempts

### Requirement 9: Match Thread - Live Phase

**User Story:** As a matchday fan, I want to discuss the match in real time during play, so that I can share reactions and engage with other fans as events unfold.

#### Acceptance Criteria

1. WHEN a match status changes to "live", THE Match_Thread SHALL switch to live mode and display a real-time comment stream via the Realtime_Service
2. WHILE a match is in live status, THE Match_Thread SHALL deliver new comments to all connected clients within 3 seconds of submission
3. WHEN a user submits a comment in a live Match_Thread, THE Realtime_Service SHALL broadcast the comment to all clients currently viewing that thread
4. WHILE a match is in live status, WHEN a goal event is recorded, THE Match_Thread SHALL display a goal reaction prompt for 30 seconds, allowing users to select a single reaction from a predefined set of options
5. WHILE a match is in live status, THE App SHALL allow users to submit one Player_Rating value between 1 and 10 per player in the match, and SHALL allow the user to update their rating for the same player at any time before the match ends
6. WHILE a match is in live status, THE Match_Thread SHALL allow users with a Reputation_Level of Trusted or Club Expert to create live polls with 2 to 4 options, and SHALL allow all users viewing the thread to cast one vote per poll
7. IF the Realtime_Service WebSocket connection is lost while a match is in live status, THEN THE Client SHALL display a connection status indicator and attempt to reconnect automatically, and SHALL display any comments missed during disconnection upon successful reconnection
8. WHILE a match is in live status, THE Match_Thread SHALL limit each user to a maximum of 20 comments per minute

### Requirement 10: Match Thread - Post-Match Phase

**User Story:** As a user, I want to see a summary of the match after it ends, so that I can review the community's perspective and player ratings.

#### Acceptance Criteria

1. WHEN a match status changes to "finished", THE Match_Thread SHALL transition to post-match view
2. WHEN a match transitions to post-match, THE App SHALL display the final score with home and away club goals
3. WHEN a match transitions to post-match and at least 10 player ratings have been submitted across all players, THE App SHALL calculate and display the community MOTM based on the highest average Player_Rating
4. THE App SHALL display the top 3 rated players in descending order of their average Player_Rating for the match, and IF two players have the same average rating, THEN THE App SHALL rank the player with more individual ratings higher
5. WHILE a match is in post-match phase, THE Match_Thread SHALL continue to accept comments for post-match discussion
6. IF fewer than 10 total player ratings have been submitted for a match, THEN THE App SHALL display a message indicating insufficient ratings to determine MOTM and SHALL NOT display the MOTM section
7. WHILE the match is in post-match phase for up to 48 hours after the match ended, THE Match_Thread SHALL remain open for comments, and after 48 hours THE Match_Thread SHALL become read-only

### Requirement 11: Reputation Scoring

**User Story:** As an engaged user, I want to earn reputation for quality contributions, so that my standing in the community reflects my knowledge and behavior.

#### Acceptance Criteria

1. WHEN a user's post receives an upvote, THE Reputation_System SHALL increase the post author's reputation score by 2 points
2. WHEN a user's comment receives an upvote, THE Reputation_System SHALL increase the comment author's reputation score by 1 point
3. WHEN a user receives a helpful badge from another user, THE Reputation_System SHALL increase the recipient's reputation score by 10 points
4. WHEN a user makes an accurate prediction (verified by match outcome), THE Reputation_System SHALL increase the user's reputation score by 20 points
5. WHEN a user's comment is removed by moderation, THE Reputation_System SHALL decrease the user's reputation score by 20 points
6. WHEN a user's content is flagged and confirmed as toxic, THE Reputation_System SHALL decrease the user's reputation score by 50 points
7. WHEN a user's content is confirmed as fake news by moderation, THE Reputation_System SHALL decrease the user's reputation score by 100 points
8. THE Reputation_System SHALL assign the "Rookie Fan" level to users with reputation between 0 and 99
9. THE Reputation_System SHALL assign the "Active Fan" level to users with reputation between 100 and 499
10. THE Reputation_System SHALL assign the "Trusted Fan" level to users with reputation between 500 and 1999
11. THE Reputation_System SHALL assign the "Club Expert" level to users with reputation of 2000 or above
12. THE Reputation_System SHALL initialize new user accounts with a reputation score of 0 and SHALL NOT allow a user's reputation score to fall below 0
13. WHEN a user removes their upvote from a post, THE Reputation_System SHALL decrease the post author's reputation score by 2 points, and WHEN a user removes their upvote from a comment, THE Reputation_System SHALL decrease the comment author's reputation score by 1 point
14. WHEN a user's reputation score crosses a level threshold (0, 100, 500, or 2000), THE Reputation_System SHALL update the user's Reputation_Level immediately and apply the corresponding permissions and feed ranking modifiers
15. IF a user attempts to upvote their own post or comment, THEN THE Reputation_System SHALL reject the action and NOT modify the author's reputation score

### Requirement 12: Toxicity Shield Moderation

**User Story:** As a user, I want to receive a warning before posting potentially harmful content, so that I can reconsider and edit my message rather than being banned.

#### Acceptance Criteria

1. WHEN a user submits a comment or post, THE Toxicity_Shield SHALL analyze the content using AI moderation before publishing
2. IF the Toxicity_Shield detects content that violates community guidelines (personal attacks, hate speech, harassment, or spam), THEN THE App SHALL display a warning message indicating the content may violate community rules and offering options to edit or proceed
3. WHEN a user receives a toxicity warning and chooses to edit, THE App SHALL return the user to the content editor with the original text preserved
4. WHEN a user receives a toxicity warning and confirms submission without editing, THE App SHALL publish the content and record it as a flagged submission for moderation review
5. THE Toxicity_Shield SHALL complete content analysis and return a result within 2 seconds of submission
6. THE Toxicity_Shield SHALL NOT flag content that contains football-related criticism of player performance without personal attacks (e.g., "Mbappe played poorly tonight" is acceptable)
7. IF the Toxicity_Shield fails to respond within 2 seconds or encounters an error, THEN THE App SHALL publish the content without a warning and queue it for asynchronous moderation review
8. WHEN a user receives a toxicity warning and dismisses the warning without choosing to edit or confirm, THEN THE App SHALL discard the submission and return the user to the content editor with the original text preserved

### Requirement 13: Push Notifications

**User Story:** As a user, I want to receive push notifications for important events, so that I stay informed about matches and interactions without constantly checking the app.

#### Acceptance Criteria

1. WHEN a match involving the user's favorite club is about to start (15 minutes before kickoff), THE App SHALL send a push notification containing the home club name, away club name, and scheduled kickoff time
2. WHEN a goal is scored in a match the user is following, THE App SHALL send a push notification containing the scorer name, current match score, and minute of the goal
3. WHEN another user replies to the user's comment or post, THE App SHALL send a push notification containing the replying user's username and the first 100 characters of the reply text
4. WHEN another user mentions the user in a comment, THE App SHALL send a push notification containing the mentioning user's username and the first 100 characters of the comment text
5. THE App SHALL allow users to configure notification preferences for each notification type (match alerts, goals, replies, mentions), with all notification types enabled by default upon account creation
6. WHILE a user has disabled a specific notification type in preferences, THE App SHALL NOT send push notifications of that type to the user
7. IF the user has not granted device notification permissions, THEN THE App SHALL display a prompt explaining the benefits of notifications and requesting permission
8. IF delivery of a push notification fails, THEN THE Notification_Service SHALL retry delivery up to 3 times with exponential backoff before discarding the notification

### Requirement 14: Follow System

**User Story:** As a user, I want to follow other users whose football opinions I value, so that I can curate a personalized "Following" feed of quality content.

#### Acceptance Criteria

1. WHEN a user taps "Follow" on another user's profile or post, THE App SHALL create a follow relationship between the two users and update the Follow button to display "Unfollow"
2. WHEN a user taps "Unfollow" on a user they currently follow, THE App SHALL remove the follow relationship, update the button to display "Follow", and exclude the unfollowed user's posts from the "Following" feed tab
3. WHEN a user follows another user, THE App SHALL include posts from the followed user in the "Following" feed tab
4. THE App SHALL display the follower count and following count on each user's profile
5. THE App SHALL NOT send a push notification when a user is followed by another user (to avoid follower-farming incentives)
6. IF a user attempts to follow themselves, THEN THE App SHALL prevent the follow action and not display the Follow button on the user's own profile
7. IF a user taps "Follow" on a user they already follow, THEN THE App SHALL not create a duplicate follow relationship and SHALL maintain the existing relationship unchanged

### Requirement 15: Club Hub

**User Story:** As a fan, I want a dedicated hub for my club that aggregates all club-specific content, so that I have a single destination for news, match threads, transfers, and discussions about my team.

#### Acceptance Criteria

1. THE App SHALL provide a dedicated Club Hub screen for the user's favorite club, accessible from the main navigation
2. THE Club Hub SHALL display sections for News, Matchday, Discussions, Transfers, Memes, and Tactical content related to the club, loading the 10 most recent posts per section on initial display
3. WHEN a match involving the user's club is live or scheduled within the next 24 hours, THE Club Hub SHALL display the match thread above all section content
4. WHEN a user navigates to the Club Hub, THE App SHALL display the club name, logo, and the count of active community members (users with the club as their favorite who have been active in the last 7 days)
5. WHEN a user views the Club Hub, THE App SHALL show posts sorted by recency within each section, most recent first
6. IF a section in the Club Hub contains no posts, THEN THE App SHALL display an empty state message indicating no content is available for that section
7. IF no match involving the user's club is live or scheduled within 24 hours, THEN THE Club Hub SHALL hide the match thread area and display section content starting from the top

### Requirement 16: User Profile

**User Story:** As a user, I want to view and manage my profile, so that I can see my reputation, posts, and personalize my presence on the platform.

#### Acceptance Criteria

1. THE App SHALL display the user's username, profile picture, favorite club, reputation score, and Reputation_Level on the profile screen
2. WHEN a user navigates to their profile, THE App SHALL display the first 20 posts authored by that user in reverse chronological order and load the next 20 posts when the user scrolls to the bottom of the list
3. WHEN a user updates their profile picture with an image file in JPEG or PNG format not exceeding 5 MB in size, THE App SHALL upload the image to storage and update the user's profile record
4. WHEN a user views another user's profile, THE App SHALL display that user's username, reputation score, Reputation_Level, favorite club, and posts in reverse chronological order
5. THE App SHALL display the user's reputation score and level badge on all posts and comments authored by that user
6. IF a user selects a profile picture file that exceeds 5 MB or is not in JPEG or PNG format, THEN THE App SHALL display a validation error indicating the file size or format constraint and prevent the upload
7. IF the profile picture upload fails due to a network or storage error, THEN THE App SHALL display an error message indicating the upload failed and retain the user's existing profile picture

### Requirement 17: Interest Selection During Onboarding

**User Story:** As a new user, I want to choose my content interests during onboarding, so that my feed is personalized to the football topics I care about most.

#### Acceptance Criteria

1. WHEN a user completes player selection (or skips it), THE App SHALL display the interest selection screen showing the following options: Matchday, Transfers, Tactics, Memes, and News
2. WHEN a user selects one or more Interest_Preference options, THE App SHALL visually indicate the selected interests and enable a confirmation action
3. WHEN a user confirms interest selection with at least 1 interest selected, THE App SHALL store the selected Interest_Preference values in the user's profile and navigate to the Home Feed
4. WHEN a user chooses to skip interest selection, THE App SHALL navigate to the Home Feed with all Interest_Preference options enabled by default
5. THE Feed_Engine SHALL use the user's stored Interest_Preference values to determine which feed content sections are displayed prominently in the Home Feed
6. IF a user confirms interest selection with no interests selected, THEN THE App SHALL display a validation message indicating at least one interest must be selected and SHALL remain on the interest selection screen

### Requirement 18: Navigation Structure

**User Story:** As a user, I want clear and consistent bottom navigation, so that I can quickly access all major sections of the app without confusion.

#### Acceptance Criteria

1. THE App SHALL display a bottom navigation bar with exactly 5 tabs: Home, Matchday, Create, Activity, and Profile
2. THE App SHALL NOT use a hamburger menu or hidden navigation drawer for primary navigation
3. WHEN a user taps a navigation tab, THE App SHALL navigate to the corresponding screen and visually highlight the active tab
4. WHILE a user is on any screen within the app, THE App SHALL keep the bottom navigation bar visible and accessible
5. THE App SHALL display the Home tab as the active default tab when a user launches the app after authentication

### Requirement 19: Dark Mode Support

**User Story:** As a user, I want to use the app in dark mode, so that I can comfortably browse during night matches or in low-light environments.

#### Acceptance Criteria

1. THE App SHALL support both light mode and dark mode display themes
2. THE App SHALL default to following the device system theme preference (light or dark) upon first launch
3. WHEN a user manually selects a theme preference (light, dark, or system) in settings, THE App SHALL apply the selected theme immediately and persist the preference across sessions
4. WHILE dark mode is active, THE App SHALL use near-black backgrounds, subtle surface colors, and maintain sufficient contrast ratios (minimum 4.5:1 for body text, 3:1 for large text) for readability
5. WHILE light mode is active, THE App SHALL use white backgrounds and neutral grays with sufficient contrast ratios (minimum 4.5:1 for body text, 3:1 for large text)
6. IF the device system theme changes while the user's preference is set to "system", THEN THE App SHALL switch the active theme to match the new system setting without requiring an app restart

### Requirement 20: Club-Adaptive Theming

**User Story:** As a fan, I want the app to reflect my club's identity through subtle color accents, so that the experience feels personalized to my football community.

#### Acceptance Criteria

1. THE App SHALL dynamically apply the Club_Theme based on the user's favorite club, adapting accent colors, header treatments, and notification highlights to the club's primary color palette
2. THE App SHALL maintain a consistent design system and layout structure across all clubs, adapting only accent colors, highlight colors, and contextual visual elements
3. THE App SHALL NOT recolor entire screen backgrounds or compromise readability when applying Club_Theme colors
4. WHILE the user's favorite club has a defined Club_Theme, THE App SHALL apply the club accent color to interactive elements including buttons, active tab indicators, and link text
5. IF a club does not have a defined Club_Theme in the system, THEN THE App SHALL fall back to the default application accent color
6. WHEN a user changes their favorite club in settings, THE App SHALL update the Club_Theme to reflect the new club's color palette within the same session without requiring an app restart

### Requirement 21: Activity Feed

**User Story:** As a user, I want a dedicated activity screen, so that I can see all my mentions, replies, match alerts, and notifications in one place.

#### Acceptance Criteria

1. WHEN a user taps the Activity tab in the bottom navigation, THE App SHALL display the Activity_Feed screen showing the user's recent activity items in reverse chronological order
2. THE Activity_Feed SHALL display items for mentions, replies to the user's posts and comments, match alerts for the user's favorite club, and reputation changes
3. THE App SHALL load the 20 most recent Activity_Feed items on initial display and load the next 20 items when the user scrolls to the bottom of the list
4. WHEN a user taps an item in the Activity_Feed, THE App SHALL navigate to the source content (the post, comment, or match thread) associated with that activity item
5. THE App SHALL display a badge on the Activity navigation tab indicating the count of unread activity items, and SHALL clear the badge when the user opens the Activity_Feed
6. IF the Activity_Feed fails to load due to a network or server error, THEN THE App SHALL display an error message and provide a retry option
7. WHILE the user has no activity items, THE App SHALL display an empty state message indicating there is no activity yet
