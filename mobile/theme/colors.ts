/**
 * Base color palette. These combine with club colors to form the full theme.
 * Club primary/secondary are injected via ThemeProvider.
 */

import { ClubTheme } from './clubs';

export interface ColorPalette {
  // Base surfaces
  background: string;
  surface: string;
  surfaceElevated: string;
  surfaceGlass: string;

  // Text
  text: string;
  textSecondary: string;
  textTertiary: string;
  textInverse: string;

  // Club-driven accents (injected from club theme)
  accent: string;
  accentSecondary: string;
  accentGlow: string;
  accentGradient: [string, string];

  // Borders
  border: string;
  borderFocus: string;

  // Semantic
  error: string;
  errorBg: string;
  success: string;
  successBg: string;
  warning: string;

  // Special
  skeleton: string;
  overlay: string;
}

export function buildDarkPalette(club: ClubTheme): ColorPalette {
  return {
    background: '#0a0a0f',
    surface: '#1a1a2e',
    surfaceElevated: '#242440',
    surfaceGlass: 'rgba(26,26,46,0.75)',

    text: '#FFFFFF',
    textSecondary: '#A0A0B0',
    textTertiary: '#6B6B80',
    textInverse: '#0a0a0f',

    accent: club.colors.primary,
    accentSecondary: club.colors.secondary,
    accentGlow: club.colors.primaryGlow,
    accentGradient: club.colors.gradient,

    border: 'rgba(255,255,255,0.08)',
    borderFocus: club.colors.primary,

    error: '#FF4757',
    errorBg: 'rgba(255,71,87,0.1)',
    success: '#2ED573',
    successBg: 'rgba(46,213,115,0.1)',
    warning: '#FFA502',

    skeleton: 'rgba(255,255,255,0.05)',
    overlay: 'rgba(0,0,0,0.6)',
  };
}

export function buildLightPalette(club: ClubTheme): ColorPalette {
  return {
    background: '#F8F9FA',
    surface: '#FFFFFF',
    surfaceElevated: '#FFFFFF',
    surfaceGlass: 'rgba(255,255,255,0.85)',

    text: '#1A1A2E',
    textSecondary: '#6B6B80',
    textTertiary: '#A0A0B0',
    textInverse: '#FFFFFF',

    accent: club.colors.primary,
    accentSecondary: club.colors.secondary,
    accentGlow: club.colors.primaryGlow,
    accentGradient: club.colors.gradient,

    border: 'rgba(0,0,0,0.08)',
    borderFocus: club.colors.primary,

    error: '#E74C3C',
    errorBg: 'rgba(231,76,60,0.08)',
    success: '#27AE60',
    successBg: 'rgba(39,174,96,0.08)',
    warning: '#F39C12',

    skeleton: 'rgba(0,0,0,0.04)',
    overlay: 'rgba(0,0,0,0.4)',
  };
}
