import { create } from 'zustand';

interface User {
  id: string;
  email: string;
  username: string;
  favoriteClubId: string | null;
  emailVerified: boolean;
  reputationLevel: string;
  profilePictureUrl: string | null;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  setTokens: (access: string, refresh: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,
  isLoading: true,

  setTokens: (access, refresh) =>
    set({ accessToken: access, refreshToken: refresh, isAuthenticated: true }),

  setUser: (user) => set({ user }),

  logout: () =>
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
    }),

  setLoading: (loading) => set({ isLoading: loading }),
}));
