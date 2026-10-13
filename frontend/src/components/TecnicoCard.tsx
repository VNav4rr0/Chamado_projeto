import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, useTheme, Card } from 'react-native-paper';
import { Atendente } from '../types/chamado';

interface TecnicoCardProps {
  tecnico: Atendente;
}

export const TecnicoCard: React.FC<TecnicoCardProps> = ({ tecnico }) => {
  const theme = useTheme();
  const percentual = tecnico.capacidade > 0 ? (tecnico.chamadosAtivos / tecnico.capacidade) * 100 : 0;

  return (
    <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
      <Card.Content>
        <Text variant="titleMedium" style={{ fontWeight: '700' }}>
          {tecnico.nome}
        </Text>
        <Text style={{ color: '#64748b', fontSize: 13, marginTop: 4 }}>
          Carga: {tecnico.chamadosAtivos} de {tecnico.capacidade} chamados
        </Text>
        <View style={styles.barBg}>
          <View
            style={[
              styles.barFill,
              {
                width: `${Math.min(100, percentual)}%`,
                backgroundColor: percentual >= 100 ? '#dc2626' : '#4f46e5',
              },
            ]}
          />
        </View>
      </Card.Content>
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    borderRadius: 14,
    marginBottom: 12,
  },
  barBg: {
    height: 8,
    backgroundColor: '#e2e8f0',
    borderRadius: 4,
    marginTop: 8,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    borderRadius: 4,
  },
});
