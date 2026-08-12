/**
 * Design tokens: spacing, typography, radii, shadows.
 * These are constant across all club themes.
 */

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const;

export const borderRadius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
} as const;

export const typography = {
  hero: { fontFamily: 'Oswald-Bold', fontSize: 48, lineHeight: 56 },
  h1: { fontFamily: 'Oswald-Bold', fontSize: 32, lineHeight: 40 },
  h2: { fontFamily: 'Inter-Bold', fontSize: 24, lineHeight: 32 },
  h3: { fontFamily: 'Inter-SemiBold', fontSize: 18, lineHeight: 26 },
  body: { fontFamily: 'Inter-Regular', fontSize: 16, lineHeight: 24 },
  bodySmall: { fontFamily: 'Inter-Regular', fontSize: 14, lineHeight: 20 },
  caption: { fontFamily: 'Inter-Regular', fontSize: 12, lineHeight: 16 },
  badge: { fontFamily: 'Oswald-Medium', fontSize: 14, lineHeight: 18 },
  score: { fontFamily: 'Oswald-Bold', fontSize: 64, lineHeight: 72 },
  tabLabel: { fontFamily: 'Inter-SemiBold', fontSize: 12, lineHeight: 16 },
} as const;

export const shadows = {
  card: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 8,
  },
  cardLifted: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
    elevation: 12,
  },
  glow: (color: string) => ({
    shadowColor: color,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
    elevation: 8,
  }),
} as const;

export const animation = {
  spring: { damping: 15, stiffness: 100 },
  springFast: { damping: 20, stiffness: 200 },
  springBouncy: { damping: 10, stiffness: 120 },
  duration: {
    fast: 200,
    normal: 300,
    slow: 500,
    themeTransition: 400,
  },
} as const;
