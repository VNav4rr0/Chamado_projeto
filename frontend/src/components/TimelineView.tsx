import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { HistoricoChamado, STATUS_LABELS, STATUS_COLORS } from '../types/chamado';

interface TimelineViewProps {
  historico: HistoricoChamado[];
}

export const TimelineView: React.FC<TimelineViewProps> = ({ historico }) => {
  const theme = useTheme();

  if (!historico || historico.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text variant="bodySmall" style={{ color: theme.colors.outline }}>
          Nenhum evento registrado.
        </Text>
      </View>
    );
  }

  const sorted = [...historico].sort(
    (a, b) => new Date(a.alteradoEm).getTime() - new Date(b.alteradoEm).getTime()
  );

  return (
    <View style={styles.container}>
      {sorted.map((item, index) => {
        const isLast = index === sorted.length - 1;
        const statusConfig = STATUS_COLORS[item.statusNovo] || { bg: '#e0e7ff', text: '#3730a3', border: '#c7d2fe' };
        const statusLabel = STATUS_LABELS[item.statusNovo] || item.statusNovo;
        const dataFormatada = new Date(item.alteradoEm).toLocaleString('pt-BR');

        return (
          <View key={index} style={styles.timelineRow}>
            <View style={styles.nodeColumn}>
              <View
                style={[
                  styles.nodeCircle,
                  { backgroundColor: statusConfig.bg, borderColor: statusConfig.border },
                ]}
              />
              {!isLast && <View style={styles.nodeLine} />}
            </View>

            <View style={styles.contentColumn}>
              <View style={styles.statusBadge}>
                <Text style={[styles.badgeText, { color: statusConfig.text }]}>
                  {statusLabel}
                </Text>
              </View>

              <Text style={styles.timeText}>{dataFormatada}</Text>
              <Text style={styles.actorText}>Por: {item.alteradoPor}</Text>

              {item.motivo && (
                <Text style={styles.motivoText}>Motivo: {item.motivo}</Text>
              )}
            </View>
          </View>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: 8,
  },
  emptyContainer: {
    padding: 16,
    alignItems: 'center',
  },
  timelineRow: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  nodeColumn: {
    alignItems: 'center',
    width: 24,
    marginRight: 12,
  },
  nodeCircle: {
    width: 12,
    height: 12,
    borderRadius: 6,
    borderWidth: 2,
    marginTop: 4,
  },
  nodeLine: {
    width: 2,
    flex: 1,
    backgroundColor: '#cbd5e1',
    marginTop: 4,
  },
  contentColumn: {
    flex: 1,
  },
  statusBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
    marginBottom: 4,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  timeText: {
    fontSize: 11,
    color: '#64748b',
  },
  actorText: {
    fontSize: 12,
    color: '#475569',
    marginTop: 2,
  },
  motivoText: {
    fontSize: 12,
    color: '#dc2626',
    marginTop: 2,
  },
});
