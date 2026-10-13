import React, { useState, useMemo } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  useWindowDimensions,
} from 'react-native';
import {
  Text,
  Searchbar,
  useTheme,
  ActivityIndicator,
  Button,
  Card,
  Chip,
} from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useChamado } from '../context/ChamadoContext';
import { Chamado, StatusChamado, STATUS_LABELS, STATUS_COLORS } from '../types/chamado';

interface ChamadosListScreenProps {
  onSelectChamado: (chamado: Chamado) => void;
  onOpenNovoChamado: () => void;
}

type FilterStatus = 'TODOS' | StatusChamado;

const FILTRO_STATUS_LISTA: { key: FilterStatus; label: string }[] = [
  { key: 'TODOS', label: 'Todos' },
  { key: 'CRIADO', label: 'Criados' },
  { key: 'EM_ANALISE', label: 'Em Triagem / Fila' },
  { key: 'EM_ANDAMENTO', label: 'Em Andamento' },
  { key: 'RESOLVIDO', label: 'Resolvidos' },
  { key: 'FECHADO', label: 'Fechados' },
];

export const ChamadosListScreen: React.FC<ChamadosListScreenProps> = ({
  onSelectChamado,
  onOpenNovoChamado,
}) => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { chamados, loading, refreshing, carregarChamados, isDarkMode, usuario } = useChamado();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<FilterStatus>('TODOS');

  const isDesktop = width >= 992;

  const metrics = useMemo(() => {
    const total = chamados.length;
    const emAndamento = chamados.filter((c) => c.status === 'EM_ANDAMENTO').length;
    const emFila = chamados.filter((c) => c.status === 'CRIADO' || c.status === 'EM_ANALISE').length;
    const concluidos = chamados.filter((c) => c.status === 'RESOLVIDO' || c.status === 'FECHADO').length;
    return { total, emAndamento, emFila, concluidos };
  }, [chamados]);

  const filteredChamados = useMemo(() => {
    return chamados.filter((item) => {
      const matchStatus = selectedStatus === 'TODOS' || item.status === selectedStatus;
      const q = searchQuery.toLowerCase().trim();
      const matchQuery =
        !q ||
        (item.protocolo && item.protocolo.toLowerCase().includes(q)) ||
        (item.titulo && item.titulo.toLowerCase().includes(q)) ||
        (item.categoria && item.categoria.toLowerCase().includes(q)) ||
        (item.atendenteNome && item.atendenteNome.toLowerCase().includes(q));
      return matchStatus && matchQuery;
    });
  }, [chamados, selectedStatus, searchQuery]);

  return (
    <ScrollView
      contentContainerStyle={[styles.container, { backgroundColor: theme.colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => carregarChamados()} />}
    >
      <View style={styles.contentWrapper}>
        {/* Top Header */}
        <View style={styles.topHeader}>
          <View>
            <Text variant="headlineSmall" style={{ fontWeight: '700', color: theme.colors.onSurface }}>
              {usuario?.role === 'ADMIN' ? 'Painel de Chamados Corporativos' : 'Meus Chamados'}
            </Text>
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              Acompanhe suas solicitações com triagem automatizada e controle de SLA
            </Text>
          </View>

          <View style={styles.headerButtons}>
            <Button
              mode="outlined"
              icon="refresh"
              onPress={() => carregarChamados()}
              loading={refreshing}
              style={{ borderRadius: 8 }}
            >
              Atualizar
            </Button>
            {usuario?.role === 'USUARIO' && (
              <Button
                mode="contained"
                icon="plus"
                buttonColor="#4f46e5"
                onPress={onOpenNovoChamado}
                style={{ borderRadius: 8 }}
              >
                Abrir Chamado
              </Button>
            )}
          </View>
        </View>

        {/* KPIs */}
        <View style={styles.kpisGrid}>
          <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={styles.kpiContent}>
              <View>
                <Text style={styles.kpiNumber}>{metrics.total}</Text>
                <Text style={styles.kpiLabel}>Total Geral</Text>
              </View>
              <MaterialCommunityIcons name="ticket-outline" size={28} color="#4f46e5" />
            </Card.Content>
          </Card>

          <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={styles.kpiContent}>
              <View>
                <Text style={[styles.kpiNumber, { color: '#b45309' }]}>{metrics.emFila}</Text>
                <Text style={styles.kpiLabel}>Em Triagem / Fila</Text>
              </View>
              <MaterialCommunityIcons name="clock-outline" size={28} color="#b45309" />
            </Card.Content>
          </Card>

          <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={styles.kpiContent}>
              <View>
                <Text style={[styles.kpiNumber, { color: '#1e40af' }]}>{metrics.emAndamento}</Text>
                <Text style={styles.kpiLabel}>Em Andamento</Text>
              </View>
              <MaterialCommunityIcons name="progress-wrench" size={28} color="#1e40af" />
            </Card.Content>
          </Card>

          <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={styles.kpiContent}>
              <View>
                <Text style={[styles.kpiNumber, { color: '#166534' }]}>{metrics.concluidos}</Text>
                <Text style={styles.kpiLabel}>Concluídos</Text>
              </View>
              <MaterialCommunityIcons name="check-circle-outline" size={28} color="#166534" />
            </Card.Content>
          </Card>
        </View>

        {/* Barra de Filtro e Busca */}
        <View style={styles.filterSection}>
          <Searchbar
            placeholder="Buscar por protocolo, título ou atendente..."
            value={searchQuery}
            onChangeText={setSearchQuery}
            style={[styles.searchbar, { backgroundColor: theme.colors.surface }]}
          />

          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.statusChips}>
            {FILTRO_STATUS_LISTA.map((f) => {
              const isSelected = selectedStatus === f.key;
              return (
                <Chip
                  key={f.key}
                  selected={isSelected}
                  onPress={() => setSelectedStatus(f.key)}
                  style={[styles.statusChip, isSelected && { backgroundColor: isDarkMode ? '#3730a3' : '#e0e7ff' }]}
                  textStyle={isSelected ? { color: '#4f46e5', fontWeight: '700' } : undefined}
                >
                  {f.label}
                </Chip>
              );
            })}
          </ScrollView>
        </View>

        {/* Lista de Chamados */}
        {loading && chamados.length === 0 ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#4f46e5" />
            <Text style={{ marginTop: 12 }}>Carregando chamados...</Text>
          </View>
        ) : filteredChamados.length === 0 ? (
          <Card style={[styles.emptyCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={{ alignItems: 'center', padding: 32 }}>
              <MaterialCommunityIcons name="ticket-outline" size={48} color="#94a3b8" />
              <Text variant="titleMedium" style={{ marginTop: 12, fontWeight: '700' }}>
                Nenhum chamado encontrado
              </Text>
              <Text variant="bodySmall" style={{ color: '#64748b', marginTop: 4, textAlign: 'center' }}>
                {searchQuery || selectedStatus !== 'TODOS'
                  ? 'Tente ajustar os filtros ou a busca digitada.'
                  : 'Nenhum chamado registrado no momento.'}
              </Text>
              {usuario?.role === 'USUARIO' && (
                <Button mode="contained" buttonColor="#4f46e5" onPress={onOpenNovoChamado} style={{ marginTop: 16 }}>
                  Abrir Primeiro Chamado
                </Button>
              )}
            </Card.Content>
          </Card>
        ) : (
          <View style={[styles.grid, isDesktop && styles.gridDesktop]}>
            {filteredChamados.map((item) => {
              const statusInfo = STATUS_COLORS[item.status] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
              const statusLabel = STATUS_LABELS[item.status] || item.status;

              return (
                <TouchableOpacity
                  key={item.id}
                  activeOpacity={0.8}
                  onPress={() => onSelectChamado(item)}
                  style={[styles.ticketItemWrapper, isDesktop && styles.ticketItemWrapperDesktop]}
                >
                  <Card style={[styles.ticketCard, { backgroundColor: theme.colors.surface }]}>
                    <Card.Content>
                      <View style={styles.ticketTopRow}>
                        <Text style={styles.protocolText}>{item.protocolo || 'SEM PROTOCOLO'}</Text>
                        <Chip
                          style={{ backgroundColor: statusInfo.bg, borderColor: statusInfo.border, borderWidth: 1 }}
                          textStyle={{ color: statusInfo.text, fontWeight: '700', fontSize: 11 }}
                        >
                          {statusLabel}
                        </Chip>
                      </View>

                      <Text variant="titleMedium" style={styles.ticketTitle} numberOfLines={2}>
                        {item.titulo}
                      </Text>

                      <Text variant="bodySmall" style={styles.ticketDesc} numberOfLines={2}>
                        {item.descricao}
                      </Text>

                      <View style={styles.ticketFooter}>
                        <View style={styles.badgeRow}>
                          <Chip icon="tag" style={styles.metaChip} textStyle={{ fontSize: 11 }}>
                            {item.categoria}
                          </Chip>
                          <Chip icon="alert-decagram" style={styles.metaChip} textStyle={{ fontSize: 11 }}>
                            {item.prioridade}
                          </Chip>
                        </View>

                        {item.atendenteNome ? (
                          <Text style={styles.atendenteText}>Técnico: {item.atendenteNome}</Text>
                        ) : (
                          <Text style={[styles.atendenteText, { color: '#94a3b8' }]}>Aguardando Atendente</Text>
                        )}
                      </View>
                    </Card.Content>
                  </Card>
                </TouchableOpacity>
              );
            })}
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
    maxWidth: 1200,
    width: '100%',
    alignSelf: 'center',
  },
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
    flexWrap: 'wrap',
    gap: 12,
  },
  headerButtons: {
    flexDirection: 'row',
    gap: 10,
  },
  kpisGrid: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
    flexWrap: 'wrap',
  },
  kpiCard: {
    flex: 1,
    minWidth: 160,
    borderRadius: 12,
  },
  kpiContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 14,
  },
  kpiNumber: {
    fontSize: 22,
    fontWeight: '800',
  },
  kpiLabel: {
    fontSize: 12,
    color: '#64748b',
    marginTop: 2,
  },
  filterSection: {
    marginBottom: 20,
    gap: 12,
  },
  searchbar: {
    borderRadius: 12,
    elevation: 1,
  },
  statusChips: {
    flexDirection: 'row',
    gap: 8,
    paddingVertical: 4,
  },
  statusChip: {
    borderRadius: 8,
  },
  loadingContainer: {
    padding: 40,
    alignItems: 'center',
  },
  emptyCard: {
    borderRadius: 16,
    marginTop: 12,
  },
  grid: {
    flexDirection: 'column',
    gap: 14,
  },
  gridDesktop: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  ticketItemWrapper: {
    width: '100%',
  },
  ticketItemWrapperDesktop: {
    width: '49%',
  },
  ticketCard: {
    borderRadius: 14,
    elevation: 2,
  },
  ticketTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  protocolText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#4f46e5',
  },
  ticketTitle: {
    fontWeight: '700',
    marginBottom: 6,
  },
  ticketDesc: {
    color: '#64748b',
    marginBottom: 12,
  },
  ticketFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.06)',
    paddingTop: 10,
    flexWrap: 'wrap',
    gap: 8,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
  },
  metaChip: {
    height: 28,
  },
  atendenteText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#475569',
  },
});
