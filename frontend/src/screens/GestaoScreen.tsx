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
  Portal,
  Dialog,
  Divider,
  SegmentedButtons,
} from 'react-native-paper';
import { chamadoService } from '../services/chamadoService';
import { useChamado } from '../context/ChamadoContext';
import { Departamento, Atendente, Sla } from '../types/chamado';

export const GestaoScreen: React.FC = () => {
  const theme = useTheme();
  const { showSnackbar } = useChamado();

  const [secao, setSecao] = useState<'departamentos' | 'atendentes' | 'sla'>('departamentos');
  const [loading, setLoading] = useState(true);

  // Estados dos dados
  const [departamentos, setDepartamentos] = useState<Departamento[]>([]);
  const [atendentes, setAtendentes] = useState<Atendente[]>([]);
  const [slas, setSlas] = useState<Sla[]>([]);

  // Diálogos de criação
  const [dialogDep, setDialogDep] = useState(false);
  const [novoDepNome, setNovoDepNome] = useState('');
  const [novoDepCats, setNovoDepCats] = useState('');

  const [dialogAtendente, setDialogAtendente] = useState(false);
  const [novoAtendenteNome, setNovoAtendenteNome] = useState('');
  const [novoAtendenteDepId, setNovoAtendenteDepId] = useState('');
  const [novoAtendenteCapacidade, setNovoAtendenteCapacidade] = useState('5');

  const [dialogSla, setDialogSla] = useState(false);
  const [slaSelecionado, setSlaSelecionado] = useState<Sla | null>(null);
  const [novoHorasSla, setNovoHorasSla] = useState('24');

  const carregarTudo = useCallback(async () => {
    try {
      setLoading(true);
      const [deps, atends, listaSla] = await Promise.all([
        chamadoService.getDepartamentos(),
        chamadoService.getAtendentes(),
        chamadoService.getSlas(),
      ]);
      setDepartamentos(deps);
      setAtendentes(atends);
      setSlas(listaSla);
      if (deps.length > 0 && !novoAtendenteDepId) {
        setNovoAtendenteDepId(deps[0].id);
      }
    } catch (err: any) {
      showSnackbar(`Erro ao carregar dados de gestão: ${err.message}`);
    } finally {
      setLoading(false);
    }
  }, [novoAtendenteDepId, showSnackbar]);

  useEffect(() => {
    carregarTudo();
  }, [carregarTudo]);

  const handleCriarDepartamento = async () => {
    if (!novoDepNome.trim() || !novoDepCats.trim()) {
      showSnackbar('Preencha o nome e as categorias separadas por vírgula.');
      return;
    }
    try {
      const categorias = novoDepCats.split(',').map((c) => c.trim()).filter(Boolean);
      await chamadoService.criarDepartamento({ nome: novoDepNome.trim(), categorias });
      showSnackbar('Departamento criado com sucesso!');
      setDialogDep(false);
      setNovoDepNome('');
      setNovoDepCats('');
      await carregarTudo();
    } catch (e: any) {
      showSnackbar(`Erro: ${e.message}`);
    }
  };

  const handleCriarAtendente = async () => {
    if (!novoAtendenteNome.trim() || !novoAtendenteDepId) {
      showSnackbar('Preencha o nome e selecione o departamento.');
      return;
    }
    try {
      await chamadoService.criarAtendente({
        nome: novoAtendenteNome.trim(),
        departamentoId: novoAtendenteDepId,
        capacidade: parseInt(novoAtendenteCapacidade, 10) || 5,
      });
      showSnackbar('Atendente cadastrado com sucesso!');
      setDialogAtendente(false);
      setNovoAtendenteNome('');
      await carregarTudo();
    } catch (e: any) {
      showSnackbar(`Erro: ${e.message}`);
    }
  };

  const handleSalvarSla = async () => {
    if (!slaSelecionado) return;
    try {
      const h = parseInt(novoHorasSla, 10);
      if (isNaN(h) || h <= 0) {
        showSnackbar('Informe um número válido de horas.');
        return;
      }
      await chamadoService.atualizarSla(slaSelecionado.prioridade, h);
      showSnackbar(`SLA da prioridade ${slaSelecionado.prioridade} atualizado!`);
      setDialogSla(false);
      await carregarTudo();
    } catch (e: any) {
      showSnackbar(`Erro: ${e.message}`);
    }
  };

  return (
    <ScrollView contentContainerStyle={[styles.container, { backgroundColor: theme.colors.background }]}>
      <View style={styles.contentWrapper}>
        <View style={styles.header}>
          <Text variant="headlineSmall" style={{ fontWeight: '700' }}>
            Gestão Operacional (Departamentos, Atendentes e SLA)
          </Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            Configure o roteamento automático de categorias, a capacidade de atendimento e os prazos
          </Text>
        </View>

        <SegmentedButtons
          value={secao}
          onValueChange={(val) => setSecao(val as any)}
          buttons={[
            { value: 'departamentos', label: 'Departamentos' },
            { value: 'atendentes', label: 'Atendentes' },
            { value: 'sla', label: 'Acordo de SLA' },
          ]}
          style={{ marginBottom: 20 }}
        />

        {loading ? (
          <ActivityIndicator size="large" color="#4f46e5" style={{ padding: 40 }} />
        ) : secao === 'departamentos' ? (
          <View>
            <View style={styles.sectionHeader}>
              <Text variant="titleMedium" style={{ fontWeight: '700' }}>
                Departamentos Cadastrados ({departamentos.length})
              </Text>
              <Button mode="contained" buttonColor="#4f46e5" icon="plus" onPress={() => setDialogDep(true)}>
                Novo Departamento
              </Button>
            </View>

            <View style={styles.gridCards}>
              {departamentos.map((dep) => (
                <Card key={dep.id} style={[styles.card, { backgroundColor: theme.colors.surface }]}>
                  <Card.Content>
                    <Text variant="titleMedium" style={{ fontWeight: '700' }}>
                      {dep.nome}
                    </Text>
                    <Text style={{ color: '#64748b', fontSize: 13, marginTop: 4 }}>
                      Categorias Atendidas:
                    </Text>
                    <Text style={{ fontWeight: '600', color: '#4f46e5', marginTop: 2 }}>
                      {dep.categorias.join(', ')}
                    </Text>
                    <Text style={{ fontSize: 12, color: dep.ativo ? '#166534' : '#dc2626', marginTop: 8 }}>
                      • {dep.ativo ? 'Ativo no sistema' : 'Inativo'}
                    </Text>
                  </Card.Content>
                </Card>
              ))}
            </View>
          </View>
        ) : secao === 'atendentes' ? (
          <View>
            <View style={styles.sectionHeader}>
              <Text variant="titleMedium" style={{ fontWeight: '700' }}>
                Técnicos & Atendentes Registrados ({atendentes.length})
              </Text>
              <Button mode="contained" buttonColor="#4f46e5" icon="plus" onPress={() => setDialogAtendente(true)}>
                Novo Atendente
              </Button>
            </View>

            <View style={styles.gridCards}>
              {atendentes.map((at) => (
                <Card key={at.id} style={[styles.card, { backgroundColor: theme.colors.surface }]}>
                  <Card.Content>
                    <Text variant="titleMedium" style={{ fontWeight: '700' }}>
                      {at.nome}
                    </Text>
                    <Text style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                      Chamados Ativos: <Text style={{ fontWeight: '700' }}>{at.chamadosAtivos} / {at.capacidade}</Text>
                    </Text>
                    <View style={styles.progressBarBg}>
                      <View
                        style={[
                          styles.progressBarFill,
                          {
                            width: `${Math.min(100, (at.chamadosAtivos / at.capacidade) * 100)}%`,
                            backgroundColor: at.chamadosAtivos >= at.capacidade ? '#dc2626' : '#4f46e5',
                          },
                        ]}
                      />
                    </View>
                    <Text style={{ fontSize: 12, color: at.ativo ? '#166534' : '#dc2626', marginTop: 8 }}>
                      • {at.ativo ? 'Disponível para atribuição' : 'Perfil inativo'}
                    </Text>
                  </Card.Content>
                </Card>
              ))}
            </View>
          </View>
        ) : (
          <View>
            <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 12 }}>
              Configuração de SLA por Nível de Criticidade
            </Text>
            <View style={styles.gridCards}>
              {slas.map((s) => (
                <Card key={s.prioridade} style={[styles.card, { backgroundColor: theme.colors.surface }]}>
                  <Card.Content>
                    <Text variant="titleMedium" style={{ fontWeight: '700' }}>
                      Prioridade {s.prioridade}
                    </Text>
                    <Text style={{ fontSize: 24, fontWeight: '800', color: '#4f46e5', marginVertical: 8 }}>
                      {s.horas} horas
                    </Text>
                    <Button
                      mode="outlined"
                      onPress={() => {
                        setSlaSelecionado(s);
                        setNovoHorasSla(String(s.horas));
                        setDialogSla(true);
                      }}
                    >
                      Alterar Prazo
                    </Button>
                  </Card.Content>
                </Card>
              ))}
            </View>
          </View>
        )}
      </View>

      {/* Diálogos */}
      <Portal>
        <Dialog visible={dialogDep} onDismiss={() => setDialogDep(false)}>
          <Dialog.Title>Novo Departamento</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label="Nome do Departamento"
              value={novoDepNome}
              onChangeText={setNovoDepNome}
              mode="outlined"
              style={{ marginBottom: 12 }}
            />
            <TextInput
              label="Categorias (separadas por vírgula)"
              value={novoDepCats}
              onChangeText={setNovoDepCats}
              placeholder="Ex: Redes, Switches, VPN"
              mode="outlined"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogDep(false)}>Cancelar</Button>
            <Button mode="contained" buttonColor="#4f46e5" onPress={handleCriarDepartamento}>
              Salvar
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={dialogAtendente} onDismiss={() => setDialogAtendente(false)}>
          <Dialog.Title>Cadastrar Atendente</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label="Nome do Atendente"
              value={novoAtendenteNome}
              onChangeText={setNovoAtendenteNome}
              mode="outlined"
              style={{ marginBottom: 12 }}
            />
            <TextInput
              label="Capacidade Simultânea"
              value={novoAtendenteCapacidade}
              onChangeText={setNovoAtendenteCapacidade}
              keyboardType="numeric"
              mode="outlined"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogAtendente(false)}>Cancelar</Button>
            <Button mode="contained" buttonColor="#4f46e5" onPress={handleCriarAtendente}>
              Salvar
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={dialogSla} onDismiss={() => setDialogSla(false)}>
          <Dialog.Title>Alterar SLA ({slaSelecionado?.prioridade})</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label="Prazo Máximo (em horas)"
              value={novoHorasSla}
              onChangeText={setNovoHorasSla}
              keyboardType="numeric"
              mode="outlined"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogSla(false)}>Cancelar</Button>
            <Button mode="contained" buttonColor="#4f46e5" onPress={handleSalvarSla}>
              Salvar
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
  contentWrapper: {
    maxWidth: 1100,
    width: '100%',
    alignSelf: 'center',
  },
  header: {
    marginBottom: 20,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  gridCards: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 14,
  },
  card: {
    flex: 1,
    minWidth: 300,
    borderRadius: 14,
  },
  progressBarBg: {
    height: 8,
    backgroundColor: '#e2e8f0',
    borderRadius: 4,
    marginTop: 8,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 4,
  },
});
