# Requirements: Fanzone Mobile Frontend

## 1. Platform & Tech Stack

1.1 React Native with Expo SDK 51+ (managed workflow)
1.2 TypeScript strict mode throughout
1.3 Expo Router (file-based navigation) with shared element transitions
1.4 React Native Reanimated 3 for all animations (60fps)
1.5 React Native Skia for custom gradients, club crest effects, particle animations
1.6 Shopify FlashList for all scrolling lists (feed, comments, match threads)
1.7 Moti for micro-interactions (upvote bounce, badge pulse, slide-ins)
1.8 Zustand for global state management (auth, theme, user)
1.9 TanStack Query (React Query) for server state, caching, optimistic updates
1.10 Expo Haptics for tactile feedback on interactions

## 2. Club-Specific Theming

2.1 Each club has a unique color palette (primary, secondary) loaded from backend
2.2 Theme applies globally once user selects their club during onboarding
2.3 Dark-first base: deep background (#0a0a0f) with club accent colors as subtle gradients
2.4 Glassmorphism cards: frosted glass effect with club color tint (blur + opacity)
2.5 Club gradient appears in: header bars, card borders, active tab indicators, loading skeletons
2.6 Smooth animated transition when user changes club (colors morph over 400ms spring)
2.7 Typography: Inter (body), Oswald/Bebas Neue condensed bold (scores, headers, match data)
2.8 Support light/dark/system theme preference with club colors adapting to both modes
2.9 Club badge/crest used as accent icon in headers and empty states
2.10 All loading states use club-tinted skeleton shimmer (not generic gray)

## 3. Onboarding Flow

3.1 Welcome screen with animated Fanzone logo reveal
3.2 Club selection: grid of club crests with search, selecting reveals club colors with explosion animation
3.3 Player favorites: carousel of squad players (optional, 1-5)
3.4 Interest selection: pill/chip selector (matchday, transfers, tactics, memes, news)
3.5 Smooth page transitions between steps (shared element for selected club crest)
3.6 Skip option for optional steps (players)
3.7 Final confirmation: full-screen club theme takeover animation before entering app

## 4. Authentication

4.1 Register/login screens with club-themed gradient background
4.2 Social login buttons (Google, Apple) styled with system appearance
4.3 Smooth form validation with inline error animations (shake + red highlight)
4.4 Biometric login (Face ID / fingerprint) after first successful login
4.5 Token refresh handled transparently (interceptor pattern)
4.6 Session expiry: gentle slide-down notification, not abrupt logout

## 5. Home Feed

5.1 Three tabs: For You / Club / Following with smooth tab bar animation
5.2 Pull-to-refresh with club-colored spinner animation
5.3 Feed cards with glassmorphism effect, club accent border glow
5.4 Card types: text post, image post (full-bleed), poll (interactive bars), match analysis (expandable)
5.5 Infinite scroll via cursor pagination (TanStack Query infinite queries)
5.6 Upvote animation: heart/fire icon with spring bounce + haptic feedback
5.7 Skeleton loading: club-colored shimmer placeholders during fetch
5.8 Empty state: club-themed illustration with encouraging message
5.9 Parallax header with club gradient that compresses on scroll
5.10 Card press animation: slight scale down (0.98) + shadow lift on touch

## 6. Post Creation

6.1 Bottom sheet or full-screen modal with type selector (text, image, poll, analysis)
6.2 Rich text input with character counter (club-colored progress ring)
6.3 Image picker with preview + crop
6.4 Poll builder: add 2-4 options with animated option slots
6.5 Post button with loading spinner, success checkmark animation
6.6 Optimistic update: card appears immediately in feed with pending indicator

## 7. Comments & Replies

7.1 Bottom sheet (swipe-to-expand) for comment threads
7.2 Threaded comments with indentation + collapse/expand animation
7.3 Reply input slides up from bottom with club-themed focus indicator
7.4 @mention autocomplete dropdown with user avatar + username
7.5 Upvote micro-animation (same as post upvote)
7.6 New comment slides in from bottom with fade animation
7.7 Swipe-to-reply gesture on comment cards

## 8. Match Threads (Hero Feature)

8.1 Live match header: score display with animated update (flip/counter animation on goals)
8.2 Real-time comment stream: comments flow in from bottom, auto-scroll with "new comments" pill
8.3 Goal reaction overlay: full-screen prompt (30s countdown) with reaction emoji picker + haptics
8.4 Stadium ambience: subtle dark gradient background with pulse on events
8.5 Player rating cards: swipe-to-rate (1-10) with star/number selector, haptic detents
8.6 MOTM display: podium-style reveal animation with top-3 players
8.7 Match phase indicator: animated badge (LIVE pulse, HT static, FT fade)
8.8 WebSocket connection with reconnect + optimistic message display
8.9 Rate limit feedback: gentle countdown toast when approaching limit
8.10 Typing indicator / presence (number of users watching)

## 9. Profile & Follow

9.1 Profile header: large avatar + club badge overlay, stats (posts, followers, following)
9.2 Reputation badge display with level name and animated progress bar to next level
9.3 Follow button with state animation (+ → Following with checkmark morph)
9.4 Profile posts feed (same card style as home feed)
9.5 Edit profile: avatar upload with crop, username change

## 10. Notifications & Activity Feed

10.1 Activity feed screen with categorized items (mentions, replies, match alerts, reputation)
10.2 Unread badge on tab bar icon with pulse animation
10.3 Swipe-to-mark-read on individual items
10.4 Push notification handling (deep links to relevant content)
10.5 Notification preferences: toggle switches with smooth on/off animation

## 11. Performance & Polish

11.1 App launch: < 2 seconds to interactive content
11.2 All list scrolling: 60fps minimum (FlashList + proper memo)
11.3 Gesture-driven: back swipe, pull-to-refresh, swipe actions all fluid
11.4 Offline support: cached feed content, queue actions for sync
11.5 Error states: club-themed error illustrations, retry buttons with bounce
11.6 Accessibility: proper labels, contrast ratios, reduced-motion support
