import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  RefreshControl,
  useWindowDimensions,
} from 'react-native';
import {
  Text,
  Button,
  Card,
  Chip,
  ActivityIndicator,
  useTheme,
  SegmentedButtons,
} from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { chamadoService } from '../services/chamadoService';
import { useChamado } from '../context/ChamadoContext';
import { ItemFila, Departamento } from '../types/chamado';

export const FilaAtendimentoScreen: React.FC = () => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { showSnackbar } = useChamado();

  const [itens, setItens] = useState<ItemFila[]>([]);
  const [departamentos, setDepartamentos] = useState<Departamento[]>([]);
  const [filtroDep, setFiltroDep] = useState<string>('TODOS');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [assumindoId, setAssumindoId] = useState<string | null>(null);

  const carregarFila = useCallback(async () => {
    try {
      setRefreshing(true);
      const [depList, filaList] = await Promise.all([
        chamadoService.getDepartamentos(),
        chamadoService.getFila(filtroDep === 'TODOS' ? undefined : filtroDep, 'NA_FILA'),
      ]);
      setDepartamentos(depList);
      setItens(filaList);
    } catch (err: any) {
      showSnackbar(`Erro ao carregar fila: ${err.message}`);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [filtroDep, showSnackbar]);

  useEffect(() => {
    carregarFila();
  }, [carregarFila]);

  const handleAssumir = async (chamadoId: string) => {
    setAssumindoId(chamadoId);
    try {
      await chamadoService.assumirItemFila(chamadoId);
      showSnackbar('✅ Chamado assumido com sucesso! Atribuído a você.');
      await carregarFila();
    } catch (err: any) {
      if (err?.status === 409 || (err.message && err.message.toLowerCase().includes('assumido'))) {
        showSnackbar('🔒 Chamado já assumido por outro técnico');
      } else {
        showSnackbar(`Erro ao assumir: ${err.message}`);
      }
      await carregarFila();
    } finally {
      setAssumindoId(null);
    }
  };

  const isDesktop = width >= 900;

  return (
    <ScrollView
      contentContainerStyle={[styles.container, { backgroundColor: theme.colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={carregarFila} />}
    >
      <View style={styles.contentWrapper}>
        <View style={styles.header}>
          <View>
            <Text variant="headlineSmall" style={{ fontWeight: '700' }}>
              Fila de Atendimento por Especialidade
            </Text>
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              Chamados triados aguardando reivindicação técnica com proteção de concorrência atômica
            </Text>
          </View>
          <Button mode="outlined" icon="refresh" onPress={carregarFila} loading={refreshing}>
            Atualizar Fila
          </Button>
        </View>

        {/* Filtro por Departamento */}
        {departamentos.length > 0 && (
          <View style={styles.depFilters}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
              <Chip
                selected={filtroDep === 'TODOS'}
                onPress={() => setFiltroDep('TODOS')}
                style={styles.chip}
              >
                Todos Departamentos
              </Chip>
              {departamentos.map((d) => (
                <Chip
                  key={d.id}
                  selected={filtroDep === d.id}
                  onPress={() => setFiltroDep(d.id)}
                  style={styles.chip}
                >
                  {d.nome}
                </Chip>
              ))}
            </ScrollView>
          </View>
        )}

        {loading ? (
          <View style={styles.centerBox}>
            <ActivityIndicator size="large" color="#4f46e5" />
            <Text style={{ marginTop: 12 }}>Carregando itens da fila...</Text>
          </View>
        ) : itens.length === 0 ? (
          <Card style={[styles.cardEmpty, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={{ alignItems: 'center', padding: 32 }}>
              <MaterialCommunityIcons name="check-decagram-outline" size={48} color="#166534" />
              <Text variant="titleMedium" style={{ fontWeight: '700', marginTop: 12 }}>
                Fila de Espera Vazia
              </Text>
              <Text style={{ color: '#64748b', textAlign: 'center', marginTop: 4 }}>
                Todos os chamados deste departamento foram assumidos ou concluídos.
              </Text>
            </Card.Content>
          </Card>
        ) : (
          <View style={[styles.grid, isDesktop && styles.gridDesktop]}>
            {itens.map((item) => (
              <Card
                key={item.id}
                style={[styles.itemCard, { backgroundColor: theme.colors.surface }]}
              >
                <Card.Content>
                  <View style={styles.itemHeader}>
                    <Text style={styles.protoText}>{item.protocolo}</Text>
                    <Chip
                      style={{ backgroundColor: '#fef3c7' }}
                      textStyle={{ color: '#92400e', fontWeight: '700', fontSize: 11 }}
                    >
                      NA FILA
                    </Chip>
                  </View>

                  <View style={styles.itemMeta}>
                    <Text style={{ fontSize: 13, color: '#64748b' }}>
                      Categoria: <Text style={{ fontWeight: '700', color: theme.colors.onSurface }}>{item.categoria}</Text>
                    </Text>
                    <Text style={{ fontSize: 13, color: '#64748b' }}>
                      Prioridade: <Text style={{ fontWeight: '700', color: '#b45309' }}>{item.prioridade}</Text>
                    </Text>
                    {item.prazoSla && (
                      <Text style={{ fontSize: 12, color: '#dc2626', marginTop: 4 }}>
                        Prazo SLA: {new Date(item.prazoSla).toLocaleString('pt-BR')}
                      </Text>
                    )}
                  </View>

                  <Button
                    mode="contained"
                    buttonColor="#4f46e5"
                    icon="hand-back-right-outline"
                    loading={assumindoId === item.chamadoId}
                    disabled={assumindoId !== null}
                    onPress={() => handleAssumir(item.chamadoId)}
                    style={{ marginTop: 16, borderRadius: 8 }}
                  >
                    Assumir Chamado
                  </Button>
                </Card.Content>
              </Card>
            ))}
          </View>
        )}
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 24,
    flexGrow: 1,
  },
  contentWrapper: {
    maxWidth: 1100,
    width: '100%',
    alignSelf: 'center',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
    flexWrap: 'wrap',
    gap: 12,
  },
  depFilters: {
    marginBottom: 16,
  },
  chip: {
    borderRadius: 8,
  },
  centerBox: {
    padding: 40,
    alignItems: 'center',
  },
  cardEmpty: {
    borderRadius: 16,
    marginTop: 16,
  },
  grid: {
    flexDirection: 'column',
    gap: 14,
  },
  gridDesktop: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  itemCard: {
    borderRadius: 14,
    minWidth: 320,
    flex: 1,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  protoText: {
    fontWeight: '800',
    color: '#4f46e5',
    fontSize: 14,
  },
  itemMeta: {
    marginTop: 6,
    gap: 4,
  },
});
