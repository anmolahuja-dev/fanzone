import { ExpoConfig, ConfigContext } from 'expo/config';

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Fanzone',
  slug: 'fanzone',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'automatic',
  splash: {
    image: './assets/splash.png',
    resizeMode: 'contain',
    backgroundColor: '#0a0a0f',
  },
  scheme: 'fanzone',
  ios: {
    supportsTablet: false,
    bundleIdentifier: 'dev.fanzone.app',
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon.png',
      backgroundColor: '#0a0a0f',
    },
    package: 'dev.fanzone.app',
  },
  plugins: [
    'expo-router',
    'expo-font',
    'expo-haptics',
    'expo-image-picker',
  ],
  experiments: {
    typedRoutes: true,
  },
  extra: {
    apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:8081',
    wsBaseUrl: process.env.WS_BASE_URL || 'ws://localhost:8084',
  },
});
