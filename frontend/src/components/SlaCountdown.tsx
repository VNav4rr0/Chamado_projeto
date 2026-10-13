import React, { useState, useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';

interface SlaCountdownProps {
  slaPrazo: string | null;
  criadoEm: string;
  status: string;
}

export const SlaCountdown: React.FC<SlaCountdownProps> = ({ slaPrazo, criadoEm, status }) => {
  const theme = useTheme();
  const [timeLeft, setTimeLeft] = useState<{
    text: string;
    progress: number;
    isExpired: boolean;
    color: string;
    bgColor: string;
  }>({
    text: 'Calculando prazo...',
    progress: 1,
    isExpired: false,
    color: '#10b981',
    bgColor: '#ecfdf5',
  });

  useEffect(() => {
    if (!slaPrazo || status === 'RESOLVIDO' || status === 'CANCELADO') {
      return;
    }

    // Ponto-chave: Cálculo dinâmico do tempo restante e transição de cores conforme urgência do SLA
    const calculate = () => {
      const start = new Date(criadoEm).getTime();
      const end = new Date(slaPrazo).getTime();
      const now = new Date().getTime();

      const totalDuration = Math.max(end - start, 1);
      const remaining = end - now;

      if (remaining <= 0) {
        const passedMin = Math.abs(Math.round(remaining / (1000 * 60)));
        const passedH = Math.floor(passedMin / 60);
        const remM = passedMin % 60;
        const text = passedH > 0 ? `Expirado há ${passedH}h ${remM}m` : `Expirado há ${passedMin}m`;

        setTimeLeft({
          text,
          progress: 0,
          isExpired: true,
          color: '#ef4444',
          bgColor: '#fef2f2',
        });
        return;
      }

      const progress = Math.max(0, Math.min(1, remaining / totalDuration));
      const remMinutes = Math.floor(remaining / (1000 * 60));
      const hours = Math.floor(remMinutes / 60);
      const mins = remMinutes % 60;

      let color = '#10b981';
      let bgColor = '#ecfdf5';
      if (progress < 0.25 || hours < 1) {
        color = '#ef4444';
        bgColor = '#fef2f2';
      } else if (progress < 0.5) {
        color = '#f59e0b';
        bgColor = '#fffbeb';
      }

      const text = hours > 0 ? `${hours}h ${mins}m restantes` : `${mins}m restantes`;

      setTimeLeft({
        text,
        progress,
        isExpired: false,
        color,
        bgColor,
      });
    };

    calculate();
    const interval = setInterval(calculate, 30000);
    return () => clearInterval(interval);
  }, [slaPrazo, criadoEm, status]);

  if (!slaPrazo) {
    if (status === 'AGUARDANDO_TECNICO') {
      return (
        <View style={[styles.statusBanner, { backgroundColor: '#fffbeb', borderColor: '#fde68a' }]}>
          <MaterialCommunityIcons name="timer-sand" size={15} color="#d97706" />
          <Text variant="labelSmall" style={{ color: '#b45309', marginLeft: 6, fontWeight: '700' }}>
            SLA em espera • Aguardando disponibilidade de técnico
          </Text>
        </View>
      );
    }
    return (
      <View style={[styles.statusBanner, { backgroundColor: '#eff6ff', borderColor: '#bfdbfe' }]}>
        <MaterialCommunityIcons name="cog-sync" size={15} color="#2563eb" />
        <Text variant="labelSmall" style={{ color: '#1d4ed8', marginLeft: 6, fontWeight: '600' }}>
          Calculando prazo pelo RabbitMQ...
        </Text>
      </View>
    );
  }

  if (status === 'RESOLVIDO') {
    return (
      <View style={[styles.statusBanner, { backgroundColor: '#ecfdf5', borderColor: '#a7f3d0' }]}>
        <MaterialCommunityIcons name="check-decagram" size={15} color="#059669" />
        <Text variant="labelSmall" style={{ color: '#047857', marginLeft: 6, fontWeight: '700' }}>
          Chamado resolvido dentro da meta de SLA
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.headerRow}>
        <View style={styles.iconLabelRow}>
          <View style={[styles.pulseDot, { backgroundColor: timeLeft.color }]} />
          <Text variant="labelSmall" style={{ color: timeLeft.color, fontWeight: '800' }}>
            {timeLeft.isExpired ? 'SLA VIOLADO' : 'TEMPO RESTANTE'}
          </Text>
        </View>
        <Text variant="labelMedium" style={{ fontWeight: '800', color: timeLeft.color }}>
          {timeLeft.text}
        </Text>
      </View>

      <View style={[styles.track, { backgroundColor: theme.colors.surfaceVariant }]}>
        <View
          style={[
            styles.fill,
            {
              width: `${Math.round(timeLeft.progress * 100)}%`,
              backgroundColor: timeLeft.color,
            },
          ]}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 4,
  },
  statusBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  iconLabelRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  pulseDot: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
    marginRight: 6,
  },
  track: {
    height: 5,
    borderRadius: 3,
    overflow: 'hidden',
    width: '100%',
  },
  fill: {
    height: '100%',
    borderRadius: 3,
  },
});
