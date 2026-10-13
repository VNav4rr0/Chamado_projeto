import React from 'react';
import { View, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useChamado } from '../context/ChamadoContext';

interface HeaderBarProps {
  title: string;
  subtitle?: string;
  showBack?: boolean;
  onBack?: () => void;
}

export const HeaderBar: React.FC<HeaderBarProps> = ({ title, subtitle, showBack, onBack }) => {
  const theme = useTheme();
  const { isDarkMode, toggleTheme } = useChamado();

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: theme.colors.surface,
          borderBottomColor: isDarkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.05)',
        },
      ]}
    >
      <View style={styles.leftRow}>
        {showBack && onBack ? (
          <TouchableOpacity activeOpacity={0.7} onPress={onBack} style={styles.backButton}>
            <MaterialCommunityIcons
              name="arrow-left"
              size={22}
              color={theme.colors.onSurface}
            />
          </TouchableOpacity>
        ) : (
          <LinearGradient
            colors={['#4f46e5', '#6366f1']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.logoBadge}
          >
            <MaterialCommunityIcons name="headset" size={20} color="#ffffff" />
          </LinearGradient>
        )}

        <View style={styles.titles}>
          <Text
            variant="titleMedium"
            style={[styles.mainTitle, { color: theme.colors.onSurface }]}
          >
            {title}
          </Text>
          {subtitle && (
            <Text
              variant="bodySmall"
              style={[styles.subTitle, { color: theme.colors.onSurfaceVariant }]}
            >
              {subtitle}
            </Text>
          )}
        </View>
      </View>

      <View style={styles.rightRow}>
        {/* Badge Pill de Ambiente */}
        <View
          style={[
            styles.envPill,
            {
              backgroundColor: isDarkMode ? 'rgba(16, 185, 129, 0.18)' : '#ecfdf5',
              borderColor: isDarkMode ? 'rgba(52, 211, 153, 0.3)' : '#a7f3d0',
            },
          ]}
        >
          <View
            style={[
              styles.dot,
              { backgroundColor: '#10b981' },
            ]}
          />
          <Text
            variant="labelSmall"
            style={[
              styles.envText,
              { color: '#059669' },
            ]}
          >
            GATEWAY ATIVO
          </Text>
        </View>

        {/* Botão de Tema */}
        <TouchableOpacity
          activeOpacity={0.7}
          onPress={toggleTheme}
          style={[
            styles.iconButton,
            {
              backgroundColor: isDarkMode
                ? 'rgba(255,255,255,0.06)'
                : 'rgba(0,0,0,0.04)',
            },
          ]}
        >
          <MaterialCommunityIcons
            name={isDarkMode ? 'weather-sunny' : 'weather-night'}
            size={20}
            color={isDarkMode ? '#fbbf24' : '#475569'}
          />
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.03,
        shadowRadius: 8,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  leftRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  logoBadge: {
    width: 38,
    height: 38,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
    shadowColor: '#4f46e5',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.25,
    shadowRadius: 5,
    elevation: 3,
  },
  backButton: {
    width: 38,
    height: 38,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    backgroundColor: 'rgba(0,0,0,0.04)',
  },
  titles: {
    flex: 1,
  },
  mainTitle: {
    fontWeight: '800',
    letterSpacing: -0.3,
    fontSize: 17,
  },
  subTitle: {
    fontSize: 11,
    fontWeight: '500',
    marginTop: 1,
  },
  rightRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  envPill: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 20,
    borderWidth: 1,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 5,
  },
  envText: {
    fontWeight: '800',
    fontSize: 9.5,
    letterSpacing: 0.4,
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
