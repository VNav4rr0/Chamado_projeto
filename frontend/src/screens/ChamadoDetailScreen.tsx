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
  Portal,
  Dialog,
  TextInput,
  useTheme,
  ActivityIndicator,
  Card,
  Chip,
  Divider,
} from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useChamado } from '../context/ChamadoContext';
import { chamadoService } from '../services/chamadoService';
import { Chamado, STATUS_LABELS, STATUS_COLORS } from '../types/chamado';

interface ChamadoDetailScreenProps {
  chamadoId: string;
  onBack: () => void;
}

export const ChamadoDetailScreen: React.FC<ChamadoDetailScreenProps> = ({
  chamadoId,
  onBack,
}) => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { usuario, alterarStatus, showSnackbar } = useChamado();

  const [chamado, setChamado] = useState<Chamado | null>(null);
  const [loadingLocal, setLoadingLocal] = useState(true);

  // Modais de ação
  const [dialogResolver, setDialogResolver] = useState(false);
  const [horasTrabalhadas, setHorasTrabalhadas] = useState('2.5');
  const [custo, setCusto] = useState('150.00');

  const [dialogRejeitar, setDialogRejeitar] = useState(false);
  const [motivoRejeicao, setMotivoRejeicao] = useState('');

  const [executandoAcao, setExecutandoAcao] = useState(false);

  const carregarChamado = useCallback(async () => {
    try {
      setLoadingLocal(true);
      const c = await chamadoService.getChamadoById(chamadoId);
      setChamado(c);
    } catch (err: any) {
      showSnackbar(`Erro ao carregar detalhes: ${err.message}`);
    } finally {
      setLoadingLocal(false);
    }
  }, [chamadoId, showSnackbar]);

  useEffect(() => {
    carregarChamado();
  }, [carregarChamado]);

  const isDesktop = width >= 900;

  if (loadingLocal && !chamado) {
    return (
      <View style={[styles.centerContainer, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator size="large" color="#4f46e5" />
        <Text style={{ marginTop: 12 }}>Carregando dados do chamado...</Text>
      </View>
    );
  }

  if (!chamado) {
    return (
      <View style={[styles.centerContainer, { backgroundColor: theme.colors.background }]}>
        <MaterialCommunityIcons name="alert-circle-outline" size={48} color="#ef4444" />
        <Text variant="titleMedium" style={{ marginTop: 12 }}>Chamado não localizado</Text>
        <Button mode="contained" onPress={onBack} style={{ marginTop: 16 }}>
          Voltar para a lista
        </Button>
      </View>
    );
  }

  const statusInfo = STATUS_COLORS[chamado.status] || { bg: '#e2e8f0', text: '#334155', border: '#cbd5e1' };
  const statusLabel = STATUS_LABELS[chamado.status] || chamado.status;

  const isAdmin = usuario?.role === 'ADMIN';
  const isDono = usuario?.id === chamado.solicitanteId;

  // Ações
  const handleFecharChamado = async () => {
    setExecutandoAcao(true);
    try {
      const atualizado = await alterarStatus(chamado.id, { status: 'FECHADO' });
      setChamado(atualizado);
      showSnackbar('Chamado fechado com sucesso!');
    } catch (e) {
      console.error(e);
    } finally {
      setExecutandoAcao(false);
    }
  };

  const handleMudarStatus = async (novoStatus: any) => {
    setExecutandoAcao(true);
    try {
      const atualizado = await alterarStatus(chamado.id, { status: novoStatus });
      setChamado(atualizado);
      showSnackbar(`Status alterado para ${STATUS_LABELS[novoStatus as keyof typeof STATUS_LABELS] || novoStatus}`);
    } catch (e) {
      console.error(e);
    } finally {
      setExecutandoAcao(false);
    }
  };

  const handleConfirmarResolucao = async () => {
    const h = parseFloat(horasTrabalhadas);
    const c = parseFloat(custo);
    if (isNaN(h) || h < 0) {
      showSnackbar('Informe horas trabalhadas válidas.');
      return;
    }
    if (isNaN(c) || c < 0) {
      showSnackbar('Informe um custo financeiro válido.');
      return;
    }

    setExecutandoAcao(true);
    try {
      const atualizado = await alterarStatus(chamado.id, {
        status: 'RESOLVIDO',
        horasTrabalhadas: h,
        custo: c,
      });
      setChamado(atualizado);
      setDialogResolver(false);
      showSnackbar('Chamado resolvido com sucesso!');
    } catch (e) {
      console.error(e);
    } finally {
      setExecutandoAcao(false);
    }
  };

  const handleConfirmarRejeicao = async () => {
    if (!motivoRejeicao.trim()) {
      showSnackbar('Informe a justificativa de rejeição.');
      return;
    }

    setExecutandoAcao(true);
    try {
      const atualizado = await alterarStatus(chamado.id, {
        status: 'REJEITADO',
        motivo: motivoRejeicao.trim(),
      });
      setChamado(atualizado);
      setDialogRejeitar(false);
      showSnackbar('Chamado rejeitado.');
    } catch (e) {
      console.error(e);
    } finally {
      setExecutandoAcao(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={[styles.container, { backgroundColor: theme.colors.background }]}>
      <View style={[styles.mainLayout, isDesktop && styles.mainLayoutDesktop]}>
        {/* Painel Principal */}
        <View style={styles.leftColumn}>
          <Button
            mode="text"
            icon="arrow-left"
            onPress={onBack}
            style={{ alignSelf: 'flex-start', marginBottom: 12 }}
          >
            Voltar aos chamados
          </Button>

          <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
            <Card.Content>
              {/* Header do Chamado */}
              <View style={styles.ticketHeader}>
                <View>
                  <Text variant="titleMedium" style={{ fontWeight: '700', color: '#4f46e5' }}>
                    {chamado.protocolo}
                  </Text>
                  <Text variant="headlineSmall" style={{ fontWeight: '700', marginTop: 4 }}>
                    {chamado.titulo}
                  </Text>
                </View>
                <Chip
                  style={{ backgroundColor: statusInfo.bg, borderColor: statusInfo.border, borderWidth: 1 }}
                  textStyle={{ color: statusInfo.text, fontWeight: '700' }}
                >
                  {statusLabel}
                </Chip>
              </View>

              <Divider style={{ marginVertical: 16 }} />

              <Text variant="titleSmall" style={{ fontWeight: '700', marginBottom: 6 }}>
                Descrição do Problema
              </Text>
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, lineHeight: 22 }}>
                {chamado.descricao}
              </Text>

              {chamado.motivoRejeicao && (
                <View style={styles.rejeicaoBox}>
                  <MaterialCommunityIcons name="alert-circle" size={20} color="#b91c1c" />
                  <Text style={{ color: '#b91c1c', fontWeight: '600', marginLeft: 8 }}>
                    Motivo da Rejeição: {chamado.motivoRejeicao}
                  </Text>
                </View>
              )}

              {/* Informações de Conclusão */}
              {chamado.status === 'RESOLVIDO' && (
                <View style={styles.resolucaoBox}>
                  <Text style={{ fontWeight: '700', color: '#166534' }}>
                    Dados de Resolução do Atendimento:
                  </Text>
                  <Text style={{ color: '#166534', marginTop: 4 }}>
                    • Horas trabalhadas: {chamado.horasTrabalhadas}h
                  </Text>
                  <Text style={{ color: '#166534' }}>
                    • Custo registrado: R$ {chamado.custo ? chamado.custo.toFixed(2) : '0.00'}
                  </Text>
                </View>
              )}

              <Divider style={{ marginVertical: 20 }} />

              {/* Histórico / Linha do Tempo */}
              <Text variant="titleSmall" style={{ fontWeight: '700', marginBottom: 12 }}>
                Histórico de Transições
              </Text>
              {(!chamado.historico || chamado.historico.length === 0) ? (
                <Text style={{ color: '#94a3b8' }}>Nenhuma transição registrada.</Text>
              ) : (
                <View style={styles.timeline}>
                  {chamado.historico.map((h, idx) => (
                    <View key={idx} style={styles.timelineItem}>
                      <View style={styles.timelineDot} />
                      <View style={{ flex: 1, marginLeft: 12 }}>
                        <Text style={{ fontWeight: '700', fontSize: 13.5 }}>
                          {STATUS_LABELS[h.statusNovo] || h.statusNovo}
                        </Text>
                        <Text style={{ fontSize: 12, color: '#64748b' }}>
                          Por: {h.alteradoPor} em {new Date(h.alteradoEm).toLocaleString('pt-BR')}
                        </Text>
                        {h.motivo && (
                          <Text style={{ fontSize: 12, color: '#b91c1c', marginTop: 2 }}>
                            Motivo: {h.motivo}
                          </Text>
                        )}
                      </View>
                    </View>
                  ))}
                </View>
              )}
            </Card.Content>
          </Card>
        </View>

        {/* Coluna Lateral / Ações */}
        <View style={styles.rightColumn}>
          {/* Card de Metadados */}
          <Card style={[styles.card, { backgroundColor: theme.colors.surface, marginBottom: 16 }]}>
            <Card.Content>
              <Text variant="titleSmall" style={{ fontWeight: '700', marginBottom: 12 }}>
                Detalhes do Atendimento
              </Text>

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Categoria:</Text>
                <Text style={styles.metaValue}>{chamado.categoria}</Text>
              </View>

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Prioridade:</Text>
                <Text style={styles.metaValue}>{chamado.prioridade}</Text>
              </View>

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Departamento:</Text>
                <Text style={styles.metaValue}>{chamado.departamentoNome || 'Aguardando Triagem'}</Text>
              </View>

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Técnico Responsável:</Text>
                <Text style={styles.metaValue}>{chamado.atendenteNome || 'Não Atribuído'}</Text>
              </View>

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Solicitante:</Text>
                <Text style={styles.metaValue}>{chamado.solicitanteNome}</Text>
              </View>

              {chamado.prazoSla && (
                <View style={styles.metaRow}>
                  <Text style={styles.metaLabel}>Prazo SLA:</Text>
                  <Text style={[styles.metaValue, { color: '#b45309', fontWeight: '700' }]}>
                    {new Date(chamado.prazoSla).toLocaleString('pt-BR')}
                  </Text>
                </View>
              )}

              <View style={styles.metaRow}>
                <Text style={styles.metaLabel}>Criado em:</Text>
                <Text style={styles.metaValue}>
                  {new Date(chamado.criadoEm).toLocaleString('pt-BR')}
                </Text>
              </View>
            </Card.Content>
          </Card>

          {/* Card de Ações Disponíveis */}
          <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
            <Card.Content>
              <Text variant="titleSmall" style={{ fontWeight: '700', marginBottom: 12 }}>
                Ações do Chamado
              </Text>

              {/* Botão de Fechamento (Dono ou Admin quando RESOLVIDO) */}
              {chamado.status === 'RESOLVIDO' && (isDono || isAdmin) && (
                <Button
                  mode="contained"
                  buttonColor="#166534"
                  icon="check-all"
                  loading={executandoAcao}
                  onPress={handleFecharChamado}
                  style={{ marginBottom: 10 }}
                >
                  Confirmar e Fechar Chamado
                </Button>
              )}

              {/* Ações de Atendente / ADMIN */}
              {isAdmin && chamado.status === 'EM_ANDAMENTO' && (
                <>
                  <Button
                    mode="contained"
                    buttonColor="#4f46e5"
                    icon="check-circle-outline"
                    onPress={() => setDialogResolver(true)}
                    style={{ marginBottom: 10 }}
                  >
                    Resolver Chamado
                  </Button>
                  <Button
                    mode="outlined"
                    icon="pause-circle-outline"
                    loading={executandoAcao}
                    onPress={() => handleMudarStatus('AGUARDANDO_TERCEIRO')}
                    style={{ marginBottom: 10 }}
                  >
                    Aguardar Terceiro
                  </Button>
                  <Button
                    mode="outlined"
                    textColor="#dc2626"
                    icon="close-circle-outline"
                    onPress={() => setDialogRejeitar(true)}
                    style={{ borderColor: '#fca5a5' }}
                  >
                    Rejeitar Chamado
                  </Button>
                </>
              )}

              {isAdmin && chamado.status === 'AGUARDANDO_TERCEIRO' && (
                <Button
                  mode="contained"
                  buttonColor="#4f46e5"
                  icon="play-circle-outline"
                  loading={executandoAcao}
                  onPress={() => handleMudarStatus('EM_ANDAMENTO')}
                >
                  Retomar para Em Andamento
                </Button>
              )}

              {chamado.status === 'FECHADO' && (
                <Text style={{ color: '#64748b', fontStyle: 'italic', textAlign: 'center' }}>
                  Este chamado foi finalizado e encerrado.
                </Text>
              )}

              {chamado.status === 'REJEITADO' && (
                <Text style={{ color: '#dc2626', fontStyle: 'italic', textAlign: 'center' }}>
                  Chamado cancelado ou rejeitado.
                </Text>
              )}
            </Card.Content>
          </Card>
        </View>
      </View>

      {/* Diálogo de Resolução com Horas e Custo */}
      <Portal>
        <Dialog visible={dialogResolver} onDismiss={() => setDialogResolver(false)}>
          <Dialog.Title>Resolver Chamado</Dialog.Title>
          <Dialog.Content>
            <Text style={{ marginBottom: 12, color: theme.colors.onSurfaceVariant }}>
              Informe o total de horas gastas e o custo financeiro para registrar a solução:
            </Text>
            <TextInput
              label="Horas Trabalhadas"
              value={horasTrabalhadas}
              onChangeText={setHorasTrabalhadas}
              mode="outlined"
              keyboardType="numeric"
              style={{ marginBottom: 12 }}
            />
            <TextInput
              label="Custo Financeiro (R$)"
              value={custo}
              onChangeText={setCusto}
              mode="outlined"
              keyboardType="numeric"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogResolver(false)}>Cancelar</Button>
            <Button mode="contained" buttonColor="#4f46e5" onPress={handleConfirmarResolucao}>
              Salvar Resolução
            </Button>
          </Dialog.Actions>
        </Dialog>

        {/* Diálogo de Rejeição com Motivo */}
        <Dialog visible={dialogRejeitar} onDismiss={() => setDialogRejeitar(false)}>
          <Dialog.Title>Rejeitar Chamado</Dialog.Title>
          <Dialog.Content>
            <Text style={{ marginBottom: 12, color: theme.colors.onSurfaceVariant }}>
              Informe o motivo da rejeição ou impossibilidade técnica:
            </Text>
            <TextInput
              label="Motivo da Rejeição"
              value={motivoRejeicao}
              onChangeText={setMotivoRejeicao}
              mode="outlined"
              multiline
              numberOfLines={3}
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogRejeitar(false)}>Voltar</Button>
            <Button mode="contained" buttonColor="#dc2626" onPress={handleConfirmarRejeicao}>
              Confirmar Rejeição
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 24,
    flexGrow: 1,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  mainLayout: {
    flexDirection: 'column',
    gap: 24,
    maxWidth: 1100,
    alignSelf: 'center',
    width: '100%',
  },
  mainLayoutDesktop: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  leftColumn: {
    flex: 1,
  },
  rightColumn: {
    width: 340,
  },
  card: {
    borderRadius: 16,
  },
  ticketHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  rejeicaoBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fee2e2',
    padding: 12,
    borderRadius: 8,
    marginTop: 12,
  },
  resolucaoBox: {
    backgroundColor: '#f0fdf4',
    borderWidth: 1,
    borderColor: '#bbf7d0',
    padding: 12,
    borderRadius: 8,
    marginTop: 12,
  },
  timeline: {
    borderLeftWidth: 2,
    borderLeftColor: '#e2e8f0',
    marginLeft: 10,
    paddingLeft: 14,
    gap: 16,
  },
  timelineItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  timelineDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#4f46e5',
    marginLeft: -20,
    marginTop: 4,
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
  metaLabel: {
    color: '#64748b',
    fontSize: 13,
  },
  metaValue: {
    fontWeight: '600',
    fontSize: 13,
    maxWidth: 180,
    textAlign: 'right',
  },
});
