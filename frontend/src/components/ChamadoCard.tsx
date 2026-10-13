import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { Text, Card, Chip, useTheme } from 'react-native-paper';
import { Chamado, STATUS_LABELS, STATUS_COLORS } from '../types/chamado';

interface ChamadoCardProps {
  chamado: Chamado;
  onPress: (chamado: Chamado) => void;
}

export const ChamadoCard: React.FC<ChamadoCardProps> = ({ chamado, onPress }) => {
  const theme = useTheme();
  const statusInfo = STATUS_COLORS[chamado.status] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
  const statusLabel = STATUS_LABELS[chamado.status] || chamado.status;

  return (
    <TouchableOpacity activeOpacity={0.8} onPress={() => onPress(chamado)}>
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <View style={styles.topRow}>
            <Text style={styles.protocol}>{chamado.protocolo}</Text>
            <Chip
              style={{ backgroundColor: statusInfo.bg, borderColor: statusInfo.border, borderWidth: 1 }}
              textStyle={{ color: statusInfo.text, fontWeight: '700', fontSize: 11 }}
            >
              {statusLabel}
            </Chip>
          </View>

          <Text variant="titleMedium" style={{ fontWeight: '700', marginVertical: 6 }}>
            {chamado.titulo}
          </Text>

          <Text variant="bodySmall" numberOfLines={2} style={{ color: '#64748b', marginBottom: 12 }}>
            {chamado.descricao}
          </Text>

          <View style={styles.footer}>
            <Text style={{ fontSize: 12, color: '#64748b' }}>
              Categoria: <Text style={{ fontWeight: '600', color: theme.colors.onSurface }}>{chamado.categoria}</Text>
            </Text>
            <Text style={{ fontSize: 12, color: '#64748b' }}>
              Atendente: <Text style={{ fontWeight: '600', color: theme.colors.onSurface }}>{chamado.atendenteNome || 'Fila'}</Text>
            </Text>
          </View>
        </Card.Content>
      </Card>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    borderRadius: 14,
    marginBottom: 12,
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  protocol: {
    fontWeight: '800',
    color: '#4f46e5',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.06)',
    paddingTop: 8,
  },
});
