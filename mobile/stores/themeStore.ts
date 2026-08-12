import { create } from 'zustand';
import { CLUB_THEMES, ClubTheme, DEFAULT_CLUB_THEME } from '@/theme/clubs';

type ThemeMode = 'dark' | 'light' | 'system';

interface ThemeState {
  clubTheme: ClubTheme;
  mode: ThemeMode;
  setClub: (clubId: string) => void;
  setMode: (mode: ThemeMode) => void;
}

export const useThemeStore = create<ThemeState>((set) => ({
  clubTheme: DEFAULT_CLUB_THEME,
  mode: 'dark',

  setClub: (clubId: string) => {
    const theme = CLUB_THEMES[clubId] || DEFAULT_CLUB_THEME;
    set({ clubTheme: theme });
  },

  setMode: (mode: ThemeMode) => {
    set({ mode });
  },
}));
