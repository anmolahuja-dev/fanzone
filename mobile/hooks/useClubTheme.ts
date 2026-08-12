import { useMemo } from 'react';
import { useColorScheme } from 'react-native';
import { useThemeStore } from '@/stores/themeStore';
import { buildDarkPalette, buildLightPalette, ColorPalette } from '@/theme/colors';
import { ClubTheme } from '@/theme/clubs';
import { spacing, borderRadius, typography, shadows, animation } from '@/theme/tokens';

export interface AppTheme {
  mode: 'dark' | 'light';
  club: ClubTheme;
  colors: ColorPalette;
  spacing: typeof spacing;
  borderRadius: typeof borderRadius;
  typography: typeof typography;
  shadows: typeof shadows;
  animation: typeof animation;
}

/**
 * Hook to access the full app theme including club-specific colors.
 * Resolves 'system' mode using device appearance.
 */
export function useClubTheme(): AppTheme {
  const systemScheme = useColorScheme();
  const { clubTheme, mode } = useThemeStore();

  const resolvedMode = mode === 'system'
    ? (systemScheme === 'light' ? 'light' : 'dark')
    : mode;

  const colors = useMemo(
    () => resolvedMode === 'dark' ? buildDarkPalette(clubTheme) : buildLightPalette(clubTheme),
    [resolvedMode, clubTheme]
  );

  return useMemo(() => ({
    mode: resolvedMode,
    club: clubTheme,
    colors,
    spacing,
    borderRadius,
    typography,
    shadows,
    animation,
  }), [resolvedMode, clubTheme, colors]);
}
