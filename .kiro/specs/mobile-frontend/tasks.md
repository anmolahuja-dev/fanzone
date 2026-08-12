# Implementation Plan: Fanzone Mobile Frontend

## Overview

React Native + Expo mobile app with club-specific theming, fluid animations, and a stadium-like match thread experience. Built with Expo SDK 51, Reanimated 3, Skia, FlashList, Zustand, and TanStack Query. File-based routing via Expo Router.

## Tasks

- [ ] 1. Project scaffolding and core infrastructure
  - [ ] 1.1 Initialize Expo project with TypeScript
    - `npx create-expo-app mobile --template expo-template-blank-typescript`
    - Configure `app.json` / `app.config.ts` (name, slug, bundle ID, icon, splash)
    - Install core deps: `expo-router`, `react-native-reanimated`, `@shopify/flash-list`, `react-native-skia`, `moti`, `zustand`, `@tanstack/react-query`, `axios`, `expo-haptics`
    - Set up path aliases (`@/components`, `@/hooks`, `@/theme`, etc.) in `tsconfig.json`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

  - [ ] 1.2 Set up folder structure
    - Create directory layout per design: `app/`, `components/`, `hooks/`, `services/`, `stores/`, `theme/`, `types/`, `utils/`
    - Create root `_layout.tsx` with providers: `ThemeProvider`, `QueryClientProvider`, `GestureHandlerRootView`
    - _Requirements: 1.3_

  - [ ] 1.3 Implement club theming system
    - Create `theme/clubs.ts` with 20 club theme definitions (colors, gradients, crest URLs)
    - Create `theme/tokens.ts` with spacing, typography (Inter + Oswald), borderRadius, shadows
    - Create `theme/ThemeProvider.tsx` with context providing full AppTheme
    - Create `hooks/useClubTheme.ts` hook for components to consume theme
    - Implement animated theme transitions (Reanimated color interpolation on club change)
    - Support dark/light/system mode with club colors adapting
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

  - [ ] 1.4 Set up API client and auth interceptors
    - Create `services/api.ts` with axios instance, base URL config
    - Implement token interceptor (attach Bearer token from Zustand store)
    - Implement 401 interceptor with auto token refresh + request retry
    - Create `services/auth.ts` with register, login, oauth, refresh, logout functions
    - _Requirements: 4.5_

  - [ ] 1.5 Set up Zustand stores
    - `stores/authStore.ts`: accessToken, refreshToken, user, isAuthenticated, login/logout/refresh actions
    - `stores/themeStore.ts`: clubTheme, mode (dark/light/system), setClub, setMode, persisted to AsyncStorage
    - _Requirements: 1.8, 2.6, 2.8_

  - [ ] 1.6 Install fonts (Inter + Oswald)
    - Add `expo-font` with Inter (Regular, SemiBold, Bold) and Oswald (Medium, Bold)
    - Create font loading hook that shows splash until fonts ready
    - _Requirements: 2.7_

- [ ] 2. Authentication screens
  - [ ] 2.1 Create login screen
    - Club gradient background (uses current/default theme)
    - Email + password inputs with floating labels
    - Inline validation with shake animation on error
    - Login button with loading → success checkmark animation
    - "Don't have an account?" link to register
    - _Requirements: 4.1, 4.3_

  - [ ] 2.2 Create register screen
    - Same gradient styling as login
    - Email, username, password, confirm password fields
    - Real-time validation (email format, password strength indicator, username availability)
    - Register button → success → navigate to onboarding
    - _Requirements: 4.1, 4.3_

  - [ ] 2.3 Social auth buttons
    - Google Sign-In button (expo-auth-session)
    - Apple Sign-In button (expo-apple-authentication)
    - Styled per platform guidelines with divider "or continue with"
    - _Requirements: 4.2_

- [ ] 3. Onboarding flow
  - [ ] 3.1 Club selection screen
    - Grid of 20 club crests (2 columns) with search filter
    - On select: crest scales up, club colors explode outward (Skia radial gradient animation)
    - Confirm button locks in selection and transitions to next step
    - _Requirements: 3.1, 3.2, 3.7_

  - [ ] 3.2 Player favorites screen (optional)
    - Horizontal carousel of squad players for selected club
    - Tap to select (1-5), selected players get club-colored ring + scale
    - Skip button available
    - _Requirements: 3.3, 3.6_

  - [ ] 3.3 Interests screen
    - Animated pill/chip selector (matchday, transfers, tactics, memes, news)
    - At least 1 required, visual feedback on selection (fill + bounce)
    - "Get started" → full-screen club theme takeover → navigate to main app
    - _Requirements: 3.4, 3.5, 3.7_

- [ ] 4. Home Feed
  - [ ] 4.1 Feed tab layout with three sub-tabs
    - Segmented control or swipeable tab bar (For You / Club / Following)
    - Smooth tab indicator animation with club accent color
    - Shared FlashList underneath that changes data source per tab
    - _Requirements: 5.1_

  - [ ] 4.2 PostCard component
    - Glassmorphism card with club accent border/glow
    - Author row: avatar, username, reputation badge, timestamp
    - Content area: text (expandable at 3 lines), image (full-bleed with aspect ratio)
    - Footer: upvote button (animated), comment count, share
    - Press animation: scale(0.98) with shadow lift
    - _Requirements: 5.3, 5.4, 5.6, 5.10_

  - [ ] 4.3 PollCard component
    - Renders poll options as animated progress bars
    - Tap to vote → bar animates to show percentages
    - Club-colored fill on voted option
    - _Requirements: 5.4_

  - [ ] 4.4 Feed infinite scroll + pull-to-refresh
    - TanStack Query `useInfiniteQuery` with cursor pagination
    - Pull-to-refresh with club-colored spinner
    - Skeleton loading shimmer (club-tinted) while fetching
    - Empty state with club illustration
    - _Requirements: 5.2, 5.5, 5.7, 5.8_

  - [ ] 4.5 Feed header with parallax
    - Club gradient header that compresses on scroll (parallax)
    - Shows club name + crest when expanded
    - Collapses to just tabs on scroll down
    - _Requirements: 5.9_

- [ ] 5. Post Creation
  - [ ] 5.1 Create post modal/screen
    - Full-screen modal with type selector tabs (Text, Image, Poll, Analysis)
    - Text input with character counter (club-colored progress ring approaching limit)
    - Image picker integration (expo-image-picker) with preview
    - Poll builder: dynamically add 2-4 option inputs
    - Post button with loading → checkmark animation
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ] 5.2 Optimistic feed update
    - On post creation success, immediately insert card at top of Club feed
    - Pending indicator until confirmed by server
    - _Requirements: 6.6_

- [ ] 6. Comments & Replies
  - [ ] 6.1 Comments bottom sheet
    - Swipe-up bottom sheet (react-native-bottom-sheet) from post card
    - Threaded display with indentation levels (max 3)
    - Collapse/expand animation for thread branches
    - _Requirements: 7.1, 7.2_

  - [ ] 6.2 Comment input
    - Sticky bottom input that slides up above keyboard
    - @mention autocomplete dropdown (filtered user list)
    - Send button with club accent
    - New comment appears at bottom with slide-in animation
    - _Requirements: 7.3, 7.4, 7.6_

  - [ ] 6.3 Comment interactions
    - Upvote with bounce animation + haptic
    - Swipe-to-reply gesture → pre-fills @username
    - _Requirements: 7.5, 7.7_

- [ ] 7. Match Threads (Hero Feature)
  - [ ] 7.1 Match list screen
    - Upcoming/live matches for user's club
    - Match cards with teams, time/score, LIVE badge with pulse animation
    - _Requirements: 8.1_

  - [ ] 7.2 Live match thread screen
    - Score header: large scores with flip animation on goals
    - Phase indicator: LIVE with pulsing dot, HT, FT
    - Real-time comment stream (WebSocket/STOMP) with auto-scroll
    - "New comments" pill when user scrolls up (tap to jump to bottom)
    - _Requirements: 8.1, 8.2, 8.7, 8.8_

  - [ ] 7.3 Goal reaction overlay
    - Full-screen semi-transparent overlay triggered by GoalScoredEvent
    - 30-second countdown timer (ring animation)
    - Emoji/reaction picker with haptic feedback
    - Particle burst animation (Skia) on reaction submit
    - _Requirements: 8.3, 8.4_

  - [ ] 7.4 Player ratings UI
    - Expandable player list during/after match
    - Swipe or slider to rate 1-10 with haptic detents at each number
    - Already-rated players show your rating + average
    - _Requirements: 8.5_

  - [ ] 7.5 MOTM results display
    - Podium-style reveal: 3rd → 2nd → 1st with scale + glow animation
    - Player photo, name, average rating, total votes
    - Club-themed gold/silver/bronze accents
    - _Requirements: 8.6_

  - [ ] 7.6 Comment rate limit UX
    - When approaching limit, show gentle countdown toast
    - Disable input with "You can comment again in Xs" message
    - _Requirements: 8.9_

- [ ] 8. Profile & Follow
  - [ ] 8.1 Profile screen
    - Large avatar + club badge overlay
    - Stats row: posts, followers, following (animated counter on load)
    - Reputation level badge with progress bar to next level
    - Follow/Unfollow button with morph animation (+ → ✓)
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ] 8.2 Profile posts tab
    - Same PostCard rendering as feed, filtered to user's posts
    - Cursor-paginated, infinite scroll
    - _Requirements: 9.4_

  - [ ] 8.3 Edit profile screen
    - Avatar upload with crop (expo-image-manipulator)
    - Username edit with availability check
    - Theme preference (light/dark/system) toggle
    - Club change option (triggers theme transition)
    - _Requirements: 9.5_

- [ ] 9. Notifications & Activity
  - [ ] 9.1 Activity feed screen
    - Grouped items: mentions, replies, match alerts, reputation changes
    - Each item shows icon, title, preview (100 char max), timestamp
    - Pull-to-refresh, infinite scroll
    - _Requirements: 10.1_

  - [ ] 9.2 Unread badge + mark read
    - Tab bar badge with pulse animation for unread count
    - Auto-mark-read on screen focus (PUT /activity-feeds/read)
    - Swipe-to-dismiss individual items
    - _Requirements: 10.2, 10.3_

  - [ ] 9.3 Notification preferences screen
    - Toggle switches for match_alerts, goals, replies, mentions
    - Smooth animated on/off with haptic
    - _Requirements: 10.5_

  - [ ] 9.4 Push notification setup
    - Expo Notifications registration + FCM token
    - Deep link handling (tap notification → navigate to content)
    - _Requirements: 10.4_

- [ ] 10. Polish & Performance
  - [ ] 10.1 Splash screen + app loading
    - Animated splash with Fanzone logo → club theme fade-in
    - Font + initial data prefetch during splash
    - _Requirements: 11.1_

  - [ ] 10.2 Skeleton screens
    - Club-tinted shimmer placeholders for every loading state
    - Consistent height/spacing matching actual content layout
    - _Requirements: 2.10, 5.7_

  - [ ] 10.3 Error & empty states
    - Club-themed illustrations for errors and empty feeds
    - Retry buttons with animated bounce
    - _Requirements: 11.5, 5.8_

  - [ ] 10.4 Accessibility & reduced motion
    - All interactive elements have accessibilityLabel/Role
    - Respect `prefers-reduced-motion` (disable spring animations, use simple fades)
    - Minimum touch targets (44×44)
    - _Requirements: 11.6_

## Notes

- Club theming is the backbone: every component references `useClubTheme()` for colors, never hardcoded
- Animations should feel "alive" but not annoying — springs over beziers, subtle over dramatic
- The match thread is the showcase feature: it should feel like being in the crowd
- Haptics are critical: upvotes, ratings, goal reactions should have tactile feedback
- Offline-first for feed reads (TanStack Query cache), queue writes for retry
