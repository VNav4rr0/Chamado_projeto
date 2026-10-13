import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, Card, useTheme, Button } from 'react-native-paper';
import { useChamado } from '../context/ChamadoContext';

export const SettingsScreen: React.FC = () => {
  const theme = useTheme();
  const { usuario, logout, isDarkMode, toggleTheme } = useChamado();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <Text variant="titleLarge" style={{ fontWeight: '700', marginBottom: 12 }}>
            Configurações da Sessão
          </Text>
          <Text style={{ marginBottom: 8 }}>
            Conexão: <Text style={{ fontWeight: '700' }}>Spring Cloud Gateway (http://localhost:8080/api)</Text>
          </Text>
          <Text style={{ marginBottom: 8 }}>
            Usuário: <Text style={{ fontWeight: '700' }}>{usuario?.nome} ({usuario?.email})</Text>
          </Text>
          <Text style={{ marginBottom: 16 }}>
            Perfil: <Text style={{ fontWeight: '700' }}>{usuario?.role}</Text>
          </Text>

          <Button mode="outlined" onPress={toggleTheme} style={{ marginBottom: 12 }}>
            Alternar para Tema {isDarkMode ? 'Claro' : 'Escuro'}
          </Button>

          <Button mode="contained" buttonColor="#dc2626" onPress={logout}>
            Sair da Conta (Logout)
          </Button>
        </Card.Content>
      </Card>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 24,
    flex: 1,
  },
  card: {
    maxWidth: 600,
    alignSelf: 'center',
    width: '100%',
    borderRadius: 16,
  },
});
