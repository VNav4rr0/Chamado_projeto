import React, { useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  useWindowDimensions,
} from 'react-native';
import {
  Text,
  TextInput,
  useTheme,
  HelperText,
  ActivityIndicator,
  Button,
  Card,
  Chip,
} from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useChamado } from '../context/ChamadoContext';
import { chamadoService } from '../services/chamadoService';
import { PrioridadeChamado, SLA_HORAS_PADRAO } from '../types/chamado';

interface NovoChamadoScreenProps {
  onSuccess: (chamadoId: string) => void;
  onCancel: () => void;
}

const PRIORIDADES: PrioridadeChamado[] = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];

export const NovoChamadoScreen: React.FC<NovoChamadoScreenProps> = ({
  onSuccess,
  onCancel,
}) => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { criarChamado, loading, isDarkMode } = useChamado();

  const [categorias, setCategorias] = useState<string[]>([]);
  const [loadingCategorias, setLoadingCategorias] = useState(true);

  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [categoria, setCategoria] = useState<string>('');
  const [prioridade, setPrioridade] = useState<PrioridadeChamado>('MEDIA');
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const cats = await chamadoService.getCategorias();
        setCategorias(cats);
        if (cats.length > 0) {
          setCategoria(cats[0]);
        }
      } catch (err) {
        console.warn('Erro ao carregar categorias:', err);
        const fallback = ['Infraestrutura', 'Redes', 'Software', 'AcessoContas', 'Hardware'];
        setCategorias(fallback);
        setCategoria(fallback[0]);
      } finally {
        setLoadingCategorias(false);
      }
    })();
  }, []);

  const isDesktop = width >= 900;
  const isTituloValid = titulo.trim().length >= 5;
  const isDescricaoValid = descricao.trim().length >= 10;
  const canSubmit = isTituloValid && isDescricaoValid && !!categoria && !loading;

  const handleSubmit = async () => {
    setTouched(true);
    if (!canSubmit) return;

    try {
      const res = await criarChamado({
        titulo: titulo.trim(),
        descricao: descricao.trim(),
        categoria,
        prioridade,
      });
      onSuccess(res.id);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={[
          styles.container,
          { backgroundColor: theme.colors.background },
        ]}
      >
        <View style={[styles.mainLayout, isDesktop && styles.mainLayoutDesktop]}>
          {/* Formulário Principal */}
          <Card style={[styles.formCard, { backgroundColor: theme.colors.surface }]}>
            <Card.Content style={styles.cardContent}>
              <View style={styles.header}>
                <LinearGradient
                  colors={['#4f46e5', '#6366f1']}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.headerBadge}
                >
                  <MaterialCommunityIcons name="tag-plus" size={24} color="#ffffff" />
                </LinearGradient>
                <View>
                  <Text variant="titleLarge" style={{ fontWeight: '700', color: theme.colors.onSurface }}>
                    Abertura de Chamado Técnico
                  </Text>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    O chamado será triado automaticamente para o departamento especialista
                  </Text>
                </View>
              </View>

              {/* Categorias dinâmicas */}
              <Text variant="titleSmall" style={[styles.sectionTitle, { color: theme.colors.onSurface }]}>
                1. Categoria do Problema
              </Text>
              {loadingCategorias ? (
                <ActivityIndicator size="small" style={{ marginVertical: 12 }} />
              ) : (
                <View style={styles.chipsContainer}>
                  {categorias.map((cat) => {
                    const isSelected = categoria === cat;
                    return (
                      <Chip
                        key={cat}
                        selected={isSelected}
                        onPress={() => setCategoria(cat)}
                        style={[
                          styles.chip,
                          isSelected && { backgroundColor: isDarkMode ? '#3730a3' : '#e0e7ff' },
                        ]}
                        textStyle={isSelected ? { color: '#4f46e5', fontWeight: '700' } : undefined}
                        icon="tag-outline"
                      >
                        {cat}
                      </Chip>
                    );
                  })}
                </View>
              )}

              {/* Prioridades */}
              <Text variant="titleSmall" style={[styles.sectionTitle, { color: theme.colors.onSurface }]}>
                2. Nível de Prioridade (SLA estimado)
              </Text>
              <View style={styles.prioridadesGrid}>
                {PRIORIDADES.map((p) => {
                  const isSelected = prioridade === p;
                  const horas = SLA_HORAS_PADRAO[p];
                  return (
                    <TouchableOpacity
                      key={p}
                      onPress={() => setPrioridade(p)}
                      style={[
                        styles.prioridadeCard,
                        {
                          borderColor: isSelected ? '#4f46e5' : isDarkMode ? 'rgba(255,255,255,0.1)' : '#e2e8f0',
                          backgroundColor: isSelected
                            ? isDarkMode ? 'rgba(99,102,241,0.2)' : '#eef2ff'
                            : isDarkMode ? 'rgba(255,255,255,0.03)' : '#f8fafc',
                        },
                      ]}
                    >
                      <Text style={[styles.prioLabel, isSelected && { color: '#4f46e5', fontWeight: '700' }]}>
                        {p}
                      </Text>
                      <Text style={styles.prioSla}>Até {horas}h</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>

              {/* Título */}
              <Text variant="titleSmall" style={[styles.sectionTitle, { color: theme.colors.onSurface }]}>
                3. Informações da Solicitação
              </Text>
              <TextInput
                label="Título Resumido"
                value={titulo}
                onChangeText={setTitulo}
                mode="outlined"
                placeholder="Ex: Falha na conexão com o servidor de banco de dados"
                style={styles.input}
                error={touched && !isTituloValid}
              />
              {touched && !isTituloValid && (
                <HelperText type="error">O título deve conter pelo menos 5 caracteres.</HelperText>
              )}

              <TextInput
                label="Descrição Detalhada do Problema"
                value={descricao}
                onChangeText={setDescricao}
                mode="outlined"
                multiline
                numberOfLines={4}
                placeholder="Explique os sintomas observados, mensagens de erro e o impacto..."
                style={[styles.input, { height: 110 }]}
                error={touched && !isDescricaoValid}
              />
              {touched && !isDescricaoValid && (
                <HelperText type="error">A descrição deve ter ao menos 10 caracteres.</HelperText>
              )}

              {/* Botões de Ação */}
              <View style={styles.actionsContainer}>
                <Button mode="outlined" onPress={onCancel} style={styles.cancelBtn}>
                  Cancelar
                </Button>
                <Button
                  mode="contained"
                  onPress={handleSubmit}
                  loading={loading}
                  disabled={!canSubmit}
                  buttonColor="#4f46e5"
                  style={styles.submitBtn}
                >
                  Registrar Chamado
                </Button>
              </View>
            </Card.Content>
          </Card>

          {/* Coluna Lateral Informativa */}
          {isDesktop && (
            <View style={styles.sideColumn}>
              <Card style={[styles.infoCard, { backgroundColor: theme.colors.surface }]}>
                <Card.Content>
                  <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 12 }}>
                    Fluxo Orientado a Eventos
                  </Text>
                  <View style={styles.infoStep}>
                    <MaterialCommunityIcons name="numeric-1-circle" size={22} color="#4f46e5" />
                    <Text variant="bodySmall" style={{ flex: 1, marginLeft: 8 }}>
                      O chamado é registrado no banco e gravado no Transactional Outbox.
                    </Text>
                  </View>
                  <View style={styles.infoStep}>
                    <MaterialCommunityIcons name="numeric-2-circle" size={22} color="#4f46e5" />
                    <Text variant="bodySmall" style={{ flex: 1, marginLeft: 8 }}>
                      O Outbox Relay publica o evento <Text style={{ fontWeight: '700' }}>chamado.criado</Text> no RabbitMQ.
                    </Text>
                  </View>
                  <View style={styles.infoStep}>
                    <MaterialCommunityIcons name="numeric-3-circle" size={22} color="#4f46e5" />
                    <Text variant="bodySmall" style={{ flex: 1, marginLeft: 8 }}>
                      O serviço de atendimento tria a categoria para o departamento e define o prazo de SLA.
                    </Text>
                  </View>
                  <View style={styles.infoStep}>
                    <MaterialCommunityIcons name="numeric-4-circle" size={22} color="#4f46e5" />
                    <Text variant="bodySmall" style={{ flex: 1, marginLeft: 8 }}>
                      O chamado passa para <Text style={{ fontWeight: '700', color: '#b45309' }}>EM ANÁLISE / FILA</Text> e fica disponível para atendimento.
                    </Text>
                  </View>
                </Card.Content>
              </Card>
            </View>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 24,
    flexGrow: 1,
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
  formCard: {
    flex: 1,
    borderRadius: 16,
  },
  cardContent: {
    padding: 24,
  },
  sideColumn: {
    width: 320,
  },
  infoCard: {
    borderRadius: 16,
  },
  infoStep: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    marginBottom: 24,
  },
  headerBadge: {
    width: 48,
    height: 48,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sectionTitle: {
    fontWeight: '700',
    marginTop: 16,
    marginBottom: 10,
  },
  chipsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12,
  },
  chip: {
    borderRadius: 8,
  },
  prioridadesGrid: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 16,
    flexWrap: 'wrap',
  },
  prioridadeCard: {
    flex: 1,
    minWidth: 80,
    borderWidth: 1.5,
    borderRadius: 10,
    padding: 10,
    alignItems: 'center',
  },
  prioLabel: {
    fontWeight: '600',
    fontSize: 13,
  },
  prioSla: {
    fontSize: 11,
    color: '#64748b',
    marginTop: 2,
  },
  input: {
    marginBottom: 8,
  },
  actionsContainer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
    marginTop: 24,
  },
  cancelBtn: {
    borderRadius: 8,
  },
  submitBtn: {
    borderRadius: 8,
  },
});
