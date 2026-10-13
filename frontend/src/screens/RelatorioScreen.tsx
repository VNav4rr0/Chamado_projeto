import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
} from 'react-native';
import {
  Text,
  Button,
  Card,
  TextInput,
  useTheme,
  ActivityIndicator,
  Divider,
} from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { chamadoService } from '../services/chamadoService';
import { useChamado } from '../context/ChamadoContext';
import { RelatorioMensal } from '../types/chamado';

export const RelatorioScreen: React.FC = () => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { showSnackbar } = useChamado();

  const dataAtual = new Date();
  const [ano, setAno] = useState(String(dataAtual.getFullYear()));
  const [mes, setMes] = useState(String(dataAtual.getMonth() + 1));

  const [relatorio, setRelatorio] = useState<RelatorioMensal | null>(null);
  const [loading, setLoading] = useState(true);

  const carregarRelatorio = useCallback(async () => {
    const a = parseInt(ano, 10);
    const m = parseInt(mes, 10);
    if (isNaN(a) || isNaN(m) || m < 1 || m > 12) {
      showSnackbar('Ano ou mês inválido.');
      return;
    }

    try {
      setLoading(true);
      const res = await chamadoService.getRelatorioMensal(a, m);
      setRelatorio(res);
    } catch (err: any) {
      showSnackbar(`Erro ao buscar relatório: ${err.message}`);
    } finally {
      setLoading(false);
    }
  }, [ano, mes, showSnackbar]);

  useEffect(() => {
    carregarRelatorio();
  }, [carregarRelatorio]);

  const isDesktop = width >= 900;

  return (
    <ScrollView contentContainerStyle={[styles.container, { backgroundColor: theme.colors.background }]}>
      <View style={styles.contentWrapper}>
        {/* Cabeçalho */}
        <View style={styles.header}>
          <View>
            <Text variant="headlineSmall" style={{ fontWeight: '700' }}>
              Relatório Gerencial de Atendimento
            </Text>
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              Consolidado mensal de desempenho técnico, horas trabalhadas, custo e SLA (Fuso America/Sao_Paulo)
            </Text>
          </View>

          {/* Filtro Ano e Mês */}
          <View style={styles.filtroRow}>
            <TextInput
              label="Mês (1-12)"
              value={mes}
              onChangeText={setMes}
              keyboardType="numeric"
              mode="outlined"
              style={{ width: 95 }}
            />
            <TextInput
              label="Ano"
              value={ano}
              onChangeText={setAno}
              keyboardType="numeric"
              mode="outlined"
              style={{ width: 110 }}
            />
            <Button
              mode="contained"
              buttonColor="#4f46e5"
              icon="magnify"
              onPress={carregarRelatorio}
              style={{ height: 48, justifyContent: 'center', borderRadius: 8 }}
            >
              Consultar
            </Button>
          </View>
        </View>

        {loading ? (
          <ActivityIndicator size="large" color="#4f46e5" style={{ padding: 40 }} />
        ) : !relatorio ? (
          <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={{ alignItems: 'center', padding: 32 }}>
              <Text>Nenhum dado retornado para o período selecionado.</Text>
            </Card.Content>
          </Card>
        ) : (
          <View>
            {/* Cards de Métricas Principais */}
            <View style={styles.kpiGrid}>
              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>Chamados Concluídos</Text>
                  <Text style={[styles.kpiValue, { color: '#4f46e5' }]}>
                    {relatorio.totalConcluidos}
                  </Text>
                  <Text style={styles.kpiSub}>No mês {relatorio.mes}/{relatorio.ano}</Text>
                </Card.Content>
              </Card>

              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>Cumprimento de SLA</Text>
                  <Text style={[styles.kpiValue, { color: '#166534' }]}>
                    {relatorio.percentualSlaCumprido.toFixed(1)}%
                  </Text>
                  <Text style={styles.kpiSub}>Dentro do prazo</Text>
                </Card.Content>
              </Card>

              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>Tempo Médio Atendimento</Text>
                  <Text style={[styles.kpiValue, { color: '#b45309' }]}>
                    {relatorio.tempoMedioAtendimentoHoras.toFixed(1)}h
                  </Text>
                  <Text style={styles.kpiSub}>Criação até resolução</Text>
                </Card.Content>
              </Card>

              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>1ª Resposta Média</Text>
                  <Text style={[styles.kpiValue, { color: '#1e40af' }]}>
                    {relatorio.tempoMedioPrimeiraRespostaHoras.toFixed(1)}h
                  </Text>
                  <Text style={styles.kpiSub}>Até atribuição</Text>
                </Card.Content>
              </Card>

              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>Total Horas Trabalhadas</Text>
                  <Text style={[styles.kpiValue, { color: '#334155' }]}>
                    {relatorio.totalHorasTrabalhadas.toFixed(1)}h
                  </Text>
                  <Text style={styles.kpiSub}>Apontamento técnico</Text>
                </Card.Content>
              </Card>

              <Card style={[styles.kpiCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text style={styles.kpiLabel}>Custo Financeiro Total</Text>
                  <Text style={[styles.kpiValue, { color: '#047857' }]}>
                    R$ {relatorio.custoTotal.toFixed(2)}
                  </Text>
                  <Text style={styles.kpiSub}>Mão de obra e peças</Text>
                </Card.Content>
              </Card>
            </View>

            {/* Agrupamentos */}
            <View style={[styles.agrupamentosRow, isDesktop && styles.agrupamentosRowDesktop]}>
              {/* Por Categoria */}
              <Card style={[styles.grupoCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 12 }}>
                    Por Categoria
                  </Text>
                  {Object.entries(relatorio.porCategoria || {}).map(([cat, total]) => (
                    <View key={cat} style={styles.linhaGrupo}>
                      <Text style={{ fontWeight: '600' }}>{cat}</Text>
                      <Text style={{ fontWeight: '700', color: '#4f46e5' }}>{total}</Text>
                    </View>
                  ))}
                  {Object.keys(relatorio.porCategoria || {}).length === 0 && (
                    <Text style={{ color: '#94a3b8' }}>Sem registros.</Text>
                  )}
                </Card.Content>
              </Card>

              {/* Por Prioridade */}
              <Card style={[styles.grupoCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 12 }}>
                    Por Prioridade
                  </Text>
                  {Object.entries(relatorio.porPrioridade || {}).map(([prio, total]) => (
                    <View key={prio} style={styles.linhaGrupo}>
                      <Text style={{ fontWeight: '600' }}>{prio}</Text>
                      <Text style={{ fontWeight: '700', color: '#b45309' }}>{total}</Text>
                    </View>
                  ))}
                  {Object.keys(relatorio.porPrioridade || {}).length === 0 && (
                    <Text style={{ color: '#94a3b8' }}>Sem registros.</Text>
                  )}
                </Card.Content>
              </Card>

              {/* Por Atendente */}
              <Card style={[styles.grupoCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 12 }}>
                    Por Técnico / Atendente
                  </Text>
                  {Object.entries(relatorio.porAtendente || {}).map(([at, total]) => (
                    <View key={at} style={styles.linhaGrupo}>
                      <Text style={{ fontWeight: '600' }}>{at}</Text>
                      <Text style={{ fontWeight: '700', color: '#166534' }}>{total}</Text>
                    </View>
                  ))}
                  {Object.keys(relatorio.porAtendente || {}).length === 0 && (
                    <Text style={{ color: '#94a3b8' }}>Sem registros.</Text>
                  )}
                </Card.Content>
              </Card>
            </View>
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
    gap: 16,
  },
  filtroRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  card: {
    borderRadius: 14,
  },
  kpiGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 20,
  },
  kpiCard: {
    flex: 1,
    minWidth: 160,
    borderRadius: 12,
  },
  kpiLabel: {
    fontSize: 12,
    color: '#64748b',
    fontWeight: '600',
  },
  kpiValue: {
    fontSize: 24,
    fontWeight: '800',
    marginVertical: 4,
  },
  kpiSub: {
    fontSize: 11,
    color: '#94a3b8',
  },
  agrupamentosRow: {
    flexDirection: 'column',
    gap: 14,
  },
  agrupamentosRowDesktop: {
    flexDirection: 'row',
  },
  grupoCard: {
    flex: 1,
    borderRadius: 14,
  },
  linhaGrupo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
});
