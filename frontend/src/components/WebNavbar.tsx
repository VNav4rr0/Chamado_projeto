import React from 'react';
import { View, StyleSheet, TouchableOpacity, useWindowDimensions } from 'react-native';
import { Text, useTheme, Button } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useChamado } from '../context/ChamadoContext';

export type WebTabKey = 'chamados' | 'novo' | 'fila' | 'gestao' | 'relatorio';

interface WebNavbarProps {
  activeTab: WebTabKey;
  onSelectTab: (tab: WebTabKey) => void;
  selectedChamadoId: string | null;
  onBackToChamados: () => void;
}

interface NavItem {
  key: WebTabKey;
  label: string;
  icon: string;
}

export const WebNavbar: React.FC<WebNavbarProps> = ({
  activeTab,
  onSelectTab,
  selectedChamadoId,
  onBackToChamados,
}) => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { isDarkMode, toggleTheme, usuario, logout } = useChamado();

  const isDesktop = width >= 860;
  const isAdmin = usuario?.role === 'ADMIN';

  const navItems: NavItem[] = isAdmin
    ? [
        { key: 'chamados', label: 'Painel Geral', icon: 'view-dashboard-outline' },
        { key: 'fila', label: 'Fila de Espera', icon: 'tray-full' },
        { key: 'gestao', label: 'Gestão Operacional', icon: 'domain' },
        { key: 'relatorio', label: 'Relatório Mensal', icon: 'chart-box-outline' },
      ]
    : [
        { key: 'chamados', label: 'Meus Chamados', icon: 'ticket-confirmation-outline' },
        { key: 'novo', label: 'Abrir Chamado', icon: 'plus-circle-outline' },
      ];

  return (
    <header
      style={{
        backgroundColor: theme.colors.surface,
        borderBottomWidth: 1,
        borderBottomColor: isDarkMode ? 'rgba(255,255,255,0.08)' : '#e2e8f0',
        position: 'sticky' as any,
        top: 0,
        zIndex: 100,
        boxShadow: isDarkMode
          ? '0 4px 20px rgba(0,0,0,0.4)'
          : '0 2px 12px rgba(15,23,42,0.04)',
      }}
    >
      <View style={styles.innerContainer}>
        {/* Brand */}
        <TouchableOpacity
          activeOpacity={0.8}
          onPress={onBackToChamados}
          style={styles.brandContainer}
        >
          <LinearGradient
            colors={['#4f46e5', '#6366f1']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.logoBadge}
          >
            <MaterialCommunityIcons name="headset" size={22} color="#ffffff" />
          </LinearGradient>

          <View style={styles.brandTitles}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              <Text variant="titleMedium" style={[styles.brandName, { color: theme.colors.onSurface }]}>
                Central de Suporte TI
              </Text>
              <View style={[styles.enterpriseBadge, isAdmin ? styles.badgeAdmin : styles.badgeUser]}>
                <Text style={styles.enterpriseText}>{usuario?.role || 'PORTAL'}</Text>
              </View>
            </View>
            <Text variant="bodySmall" style={styles.brandSubtitle}>
              Arquitetura de Microsserviços & Clean Arch
            </Text>
          </View>
        </TouchableOpacity>

        {/* Abas */}
        <View style={styles.navLinks}>
          {navItems.map((item) => {
            const isActive = activeTab === item.key && !selectedChamadoId;
            const activeColor = isDarkMode ? '#818cf8' : '#4f46e5';

            return (
              <TouchableOpacity
                key={item.key}
                activeOpacity={0.7}
                onPress={() => onSelectTab(item.key)}
                style={[
                  styles.navButton,
                  isActive && {
                    backgroundColor: isDarkMode ? 'rgba(99, 102, 241, 0.15)' : '#eef2ff',
                    borderColor: isDarkMode ? 'rgba(99, 102, 241, 0.3)' : '#c7d2fe',
                  },
                ]}
              >
                <MaterialCommunityIcons
                  name={item.icon as any}
                  size={18}
                  color={isActive ? activeColor : isDarkMode ? '#94a3b8' : '#64748b'}
                />
                <Text
                  style={[
                    styles.navButtonText,
                    {
                      color: isActive ? activeColor : isDarkMode ? '#cbd5e1' : '#475569',
                      fontWeight: isActive ? '700' : '500',
                    },
                  ]}
                >
                  {item.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Perfil & Tema & Logout */}
        <View style={styles.rightSection}>
          <TouchableOpacity
            onPress={toggleTheme}
            style={[styles.themeBtn, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.06)' : '#f1f5f9' }]}
          >
            <MaterialCommunityIcons
              name={isDarkMode ? 'weather-sunny' : 'weather-night'}
              size={18}
              color={isDarkMode ? '#fbbf24' : '#475569'}
            />
          </TouchableOpacity>

          {usuario && (
            <View style={styles.userProfile}>
              <View style={{ alignItems: 'flex-end' }}>
                <Text style={styles.userName}>{usuario.nome}</Text>
                <Text style={styles.userEmail}>{usuario.email}</Text>
              </View>
              <Button
                mode="text"
                textColor="#dc2626"
                icon="logout"
                onPress={logout}
                style={{ marginLeft: 4 }}
                compact
              >
                Sair
              </Button>
            </View>
          )}
        </View>
      </View>
    </header>
  );
};

const styles = StyleSheet.create({
  innerContainer: {
    maxWidth: 1400,
    width: '100%',
    alignSelf: 'center',
    paddingHorizontal: 20,
    paddingVertical: 10,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 12,
  },
  brandContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  logoBadge: {
    width: 40,
    height: 40,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  brandTitles: {
    justifyContent: 'center',
  },
  brandName: {
    fontWeight: '800',
    fontSize: 16,
  },
  brandSubtitle: {
    fontSize: 11,
    color: '#94a3b8',
  },
  enterpriseBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  badgeAdmin: {
    backgroundColor: '#fee2e2',
  },
  badgeUser: {
    backgroundColor: '#dbeafe',
  },
  enterpriseText: {
    fontSize: 10,
    fontWeight: '800',
    color: '#1e293b',
  },
  navLinks: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  navButtonText: {
    fontSize: 13,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  themeBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  userProfile: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderLeftWidth: 1,
    borderLeftColor: '#e2e8f0',
    paddingLeft: 12,
  },
  userName: {
    fontSize: 13,
    fontWeight: '700',
  },
  userEmail: {
    fontSize: 11,
    color: '#94a3b8',
  },
});
