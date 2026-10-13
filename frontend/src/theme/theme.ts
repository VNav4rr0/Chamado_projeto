import { MD3LightTheme, MD3DarkTheme } from 'react-native-paper';

export const modernPalette = {
  primary: '#4f46e5', // Indigo vibrante e moderno
  primaryLight: '#6366f1',
  primaryDark: '#3730a3',
  primaryGradient: ['#4f46e5', '#6366f1'] as const,
  accent: '#06b6d4', // Cyan neon
  success: '#10b981', // Emerald
  successLight: '#d1fae5',
  warning: '#f59e0b', // Amber
  warningLight: '#fef3c7',
  danger: '#ef4444', // Rose / Red
  dangerLight: '#fee2e2',
  info: '#0284c7', // Sky
  infoLight: '#e0f2fe',
  
  // Categorias
  rede: '#0284c7',
  redeGradient: ['#0284c7', '#38bdf8'] as const,
  hardware: '#ea580c',
  hardwareGradient: ['#ea580c', '#fb923c'] as const,
  software: '#7c3aed',
  softwareGradient: ['#7c3aed', '#a855f7'] as const,
  acesso: '#059669',
  acessoGradient: ['#059669', '#34d399'] as const,
};

export const lightTheme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: modernPalette.primary,
    onPrimary: '#ffffff',
    primaryContainer: '#eef2ff',
    onPrimaryContainer: '#312e81',
    secondary: '#64748b',
    secondaryContainer: '#f1f5f9',
    onSecondaryContainer: '#0f172a',
    tertiary: '#06b6d4',
    background: '#f8fafc', // Slate 50
    surface: '#ffffff',
    surfaceVariant: '#f1f5f9',
    onSurface: '#0f172a',
    onSurfaceVariant: '#475569',
    outline: '#cbd5e1',
    outlineVariant: '#e2e8f0',
    elevation: {
      ...MD3LightTheme.colors.elevation,
      level1: '#ffffff',
      level2: '#f8fafc',
      level3: '#f1f5f9',
    },
  },
  roundness: 16,
};

export const darkTheme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    primary: '#818cf8',
    onPrimary: '#0f172a',
    primaryContainer: '#1e1b4b',
    onPrimaryContainer: '#c7d2fe',
    secondary: '#94a3b8',
    secondaryContainer: '#1e293b',
    onSecondaryContainer: '#f8fafc',
    tertiary: '#22d3ee',
    background: '#090d16', // Deep Dark Navy
    surface: '#131b2e', // Card surface
    surfaceVariant: '#1e293b',
    onSurface: '#f8fafc',
    onSurfaceVariant: '#94a3b8',
    outline: '#334155',
    outlineVariant: '#1e293b',
    elevation: {
      ...MD3DarkTheme.colors.elevation,
      level1: '#131b2e',
      level2: '#1e293b',
      level3: '#334155',
    },
  },
  roundness: 16,
};
