# Design: Fanzone Mobile Frontend

## Architecture Overview

```
mobile/
├── app/                         # Expo Router file-based routes
│   ├── (auth)/                  # Auth group (login, register)
│   ├── (onboarding)/            # Onboarding flow
│   ├── (tabs)/                  # Main tab navigation
│   │   ├── _layout.tsx          # Tab bar with club-themed active indicator
│   │   ├── feed/                # Home feed (for-you, club, following)
│   │   ├── matches/             # Match threads list + live thread
│   │   ├── create/              # Post creation
│   │   ├── activity/            # Notifications / activity feed
│   │   └── profile/             # User profile
│   └── _layout.tsx              # Root layout (theme provider, auth guard)
├── components/
│   ├── ui/                      # Primitive UI components (Button, Card, Input, etc.)
│   ├── feed/                    # Feed-specific (PostCard, PollCard, etc.)
│   ├── match/                   # Match thread components
│   ├── common/                  # Shared (Avatar, Badge, Skeleton, etc.)
│   └── animations/              # Reusable animation wrappers
├── hooks/                       # Custom hooks (useClubTheme, useAuth, etc.)
├── services/                    # API client (axios/fetch + TanStack Query)
├── stores/                      # Zustand stores (auth, theme, preferences)
├── theme/                       # Club themes, typography, spacing constants
├── types/                       # TypeScript types/interfaces
└── utils/                       # Helpers (formatters, validators)
```

## Club Theming System

### Theme Structure

```typescript
interface ClubTheme {
  id: string;
  name: string;
  colors: {
    primary: string;       // Main club color (e.g., #DA291C for Liverpool)
    secondary: string;     // Secondary color (e.g., #00B2A9)
    primaryGlow: string;   // Primary with reduced opacity for glows
    gradient: [string, string]; // Gradient pair for headers/backgrounds
  };
  crestUrl: string;
}

interface AppTheme {
  mode: 'dark' | 'light';
  club: ClubTheme;
  colors: {
    background: string;      // #0a0a0f (dark) / #fafafa (light)
    surface: string;         // #1a1a2e (dark) / #ffffff (light)
    surfaceGlass: string;    // rgba(26,26,46,0.7) with backdrop blur
    text: string;
    textSecondary: string;
    border: string;
    accent: string;          // = club.colors.primary
    accentSecondary: string; // = club.colors.secondary
    error: string;
    success: string;
  };
  spacing: typeof spacing;
  typography: typeof typography;
  borderRadius: typeof borderRadius;
}
```

### Club Theme Examples

```typescript
const CLUB_THEMES: Record<string, ClubTheme> = {
  liverpool: {
    id: 'liverpool',
    name: 'Liverpool FC',
    colors: {
      primary: '#C8102E',
      secondary: '#F6EB61',
      primaryGlow: 'rgba(200,16,46,0.15)',
      gradient: ['#C8102E', '#8B0000'],
    },
    crestUrl: '/clubs/liverpool.png',
  },
  barcelona: {
    id: 'barcelona',
    name: 'FC Barcelona',
    colors: {
      primary: '#004D98',
      secondary: '#A50044',
      primaryGlow: 'rgba(0,77,152,0.15)',
      gradient: ['#004D98', '#A50044'],
    },
    crestUrl: '/clubs/barcelona.png',
  },
  // ... 18 more clubs
};
```

### Theme Application

- `ThemeProvider` wraps entire app, provides `useClubTheme()` hook
- On club selection (onboarding or settings), fetches `GET /api/v1/clubs/{clubId}/theme`
- Stores in Zustand `themeStore` (persisted to AsyncStorage)
- Animated theme transitions via Reanimated shared values (color interpolation over 400ms spring)
- Glassmorphism cards use `surfaceGlass` with `@react-native-community/blur` or Skia shader

## Navigation Structure

```
Root Stack
├── (auth)            — Login, Register (no tab bar)
├── (onboarding)      — Club select, Players, Interests (no tab bar)
└── (tabs)            — Main app
    ├── Feed          — Home feed with 3 sub-tabs (ForYou, Club, Following)
    ├── Matches       — Match list → Match Thread detail
    ├── Create (+)    — Post creation modal (center tab, accent button)
    ├── Activity      — Notification feed
    └── Profile       — Current user profile → Edit → Settings
```

## API Client Design

```typescript
// services/api.ts
const api = axios.create({
  baseURL: Config.API_BASE_URL,
});

// Token interceptor
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto-refresh on 401
api.interceptors.response.use(null, async (error) => {
  if (error.response?.status === 401) {
    const newToken = await refreshToken();
    // retry original request
  }
  return Promise.reject(error);
});
```

### TanStack Query Hooks

```typescript
// hooks/useFeed.ts
export const useFeed = (tab: 'for-you' | 'club' | 'following') => {
  return useInfiniteQuery({
    queryKey: ['feed', tab],
    queryFn: ({ pageParam }) => api.get(`/feeds/${tab}`, { params: { cursor: pageParam, size: 20 } }),
    getNextPageParam: (lastPage) => lastPage.data.nextCursor,
    staleTime: 60_000, // 1 min cache
  });
};
```

## Animation Patterns

### Feed Card Enter
```typescript
// Spring-based enter animation for feed cards
const entering = FadeInDown.springify().damping(15).stiffness(100);
```

### Upvote Bounce
```typescript
// Moti-based upvote animation
<MotiPressable
  animate={({ pressed }) => ({
    scale: pressed ? 1.3 : 1,
  })}
  transition={{ type: 'spring', damping: 10 }}
/>
```

### Club Theme Transition
```typescript
// Reanimated color interpolation
const backgroundColor = useAnimatedStyle(() => ({
  backgroundColor: interpolateColor(
    progress.value,
    [0, 1],
    [oldColor, newColor]
  ),
}));
```

### Goal Celebration
```typescript
// Skia particle burst on goal event
<Canvas>
  <Circle cx={center.x} cy={center.y} r={radius}>
    <RadialGradient colors={[club.primary, 'transparent']} />
  </Circle>
  {particles.map(p => <Circle key={p.id} ... />)}
</Canvas>
```

## WebSocket Design (Match Threads)

```typescript
// services/websocket.ts
class MatchThreadSocket {
  private client: Client; // STOMP client

  connect(matchId: string) {
    this.client = new Client({
      brokerURL: `${Config.WS_URL}/ws`,
      onConnect: () => {
        this.client.subscribe(`/topic/match-threads/${matchId}`, (msg) => {
          const comment = JSON.parse(msg.body);
          // Add to local state via Zustand
          useMatchStore.getState().addComment(comment);
        });
      },
      reconnectDelay: 3000,
    });
    this.client.activate();
  }
}
```

## State Management (Zustand)

```typescript
// stores/authStore.ts
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

// stores/themeStore.ts
interface ThemeState {
  clubTheme: ClubTheme | null;
  mode: 'dark' | 'light' | 'system';
  setClub: (clubId: string) => void;
  setMode: (mode: 'dark' | 'light' | 'system') => void;
}
```

## Key Design Tokens

```typescript
const spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48 };

const borderRadius = { sm: 8, md: 12, lg: 16, xl: 24, full: 9999 };

const typography = {
  hero: { fontFamily: 'Oswald-Bold', fontSize: 48 },        // Match scores
  h1: { fontFamily: 'Oswald-Bold', fontSize: 32 },          // Section headers
  h2: { fontFamily: 'Inter-Bold', fontSize: 24 },
  h3: { fontFamily: 'Inter-SemiBold', fontSize: 18 },
  body: { fontFamily: 'Inter-Regular', fontSize: 16 },
  caption: { fontFamily: 'Inter-Regular', fontSize: 12 },
  badge: { fontFamily: 'Oswald-Medium', fontSize: 14 },     // Reputation badges
};

const shadows = {
  card: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 12 },
  glow: (color: string) => ({ shadowColor: color, shadowOffset: { width: 0, height: 0 }, shadowOpacity: 0.4, shadowRadius: 16 }),
};
```
