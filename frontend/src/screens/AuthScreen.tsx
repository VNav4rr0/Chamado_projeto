import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, useWindowDimensions } from 'react-native';
import { Text, TextInput, Button, Card, useTheme, SegmentedButtons, HelperText } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useChamado } from '../context/ChamadoContext';

export const AuthScreen: React.FC = () => {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const { login, cadastro, isDarkMode } = useChamado();

  const [modo, setModo] = useState<'login' | 'cadastro'>('login');
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  // Campos do formulário
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [role, setRole] = useState<'USUARIO' | 'ADMIN'>('USUARIO');
  const [codigoAdmin, setCodigoAdmin] = useState('');

  const handleSubmeter = async () => {
    setErro(null);
    if (!email.trim() || !senha.trim()) {
      setErro('Preencha e-mail e senha.');
      return;
    }

    setLoading(true);
    try {
      if (modo === 'login') {
        await login(email.trim(), senha);
      } else {
        if (!nome.trim()) {
          setErro('Informe o seu nome.');
          setLoading(false);
          return;
        }
        if (role === 'ADMIN' && !codigoAdmin.trim()) {
          setErro('Informe o código de administrador para o perfil Admin.');
          setLoading(false);
          return;
        }
        await cadastro({
          nome: nome.trim(),
          email: email.trim(),
          senha,
          role,
          codigoAdmin: role === 'ADMIN' ? codigoAdmin.trim() : undefined,
        });
      }
    } catch (err: any) {
      setErro(err.message || 'Ocorreu um erro ao processar. Verifique os dados digitados.');
    } finally {
      setLoading(false);
    }
  };

  const isDesktop = width >= 768;

  return (
    <ScrollView contentContainerStyle={styles.scrollContainer}>
      <View style={[styles.cardWrapper, { maxWidth: isDesktop ? 480 : '100%' }]}>
        <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
          <Card.Content style={styles.cardContent}>
            {/* Header Brand */}
            <View style={styles.header}>
              <LinearGradient
                colors={['#4f46e5', '#6366f1']}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.logoBadge}
              >
                <MaterialCommunityIcons name="headset" size={28} color="#ffffff" />
              </LinearGradient>
              <Text variant="headlineSmall" style={[styles.title, { color: theme.colors.onSurface }]}>
                Central de Chamados TI
              </Text>
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, textAlign: 'center' }}>
                {modo === 'login'
                  ? 'Acesse o portal com suas credenciais'
                  : 'Crie sua conta para solicitar ou atender chamados'}
              </Text>
            </View>

            {/* Alternador Login / Cadastro */}
            <SegmentedButtons
              value={modo}
              onValueChange={(val) => {
                setModo(val as any);
                setErro(null);
              }}
              buttons={[
                { value: 'login', label: 'Entrar' },
                { value: 'cadastro', label: 'Criar Conta' },
              ]}
              style={styles.segmented}
            />

            {/* Mensagem de Erro */}
            {erro && (
              <View style={styles.errorBox}>
                <MaterialCommunityIcons name="alert-circle-outline" size={20} color="#dc2626" />
                <Text style={styles.errorText}>{erro}</Text>
              </View>
            )}

            {/* Formulário */}
            {modo === 'cadastro' && (
              <TextInput
                label="Nome Completo"
                value={nome}
                onChangeText={setNome}
                mode="outlined"
                style={styles.input}
                left={<TextInput.Icon icon="account-outline" />}
              />
            )}

            <TextInput
              label="E-mail"
              value={email}
              onChangeText={setEmail}
              mode="outlined"
              keyboardType="email-address"
              autoCapitalize="none"
              style={styles.input}
              left={<TextInput.Icon icon="email-outline" />}
            />

            <TextInput
              label="Senha"
              value={senha}
              onChangeText={setSenha}
              mode="outlined"
              secureTextEntry
              style={styles.input}
              left={<TextInput.Icon icon="lock-outline" />}
            />

            {modo === 'cadastro' && (
              <View style={styles.roleContainer}>
                <Text variant="labelLarge" style={{ marginBottom: 6, color: theme.colors.onSurface }}>
                  Tipo de Acesso:
                </Text>
                <SegmentedButtons
                  value={role}
                  onValueChange={(v) => setRole(v as any)}
                  buttons={[
                    { value: 'USUARIO', label: 'Usuário (Solicitante)' },
                    { value: 'ADMIN', label: 'Admin (Técnico)' },
                  ]}
                  style={{ marginBottom: 12 }}
                />

                {role === 'ADMIN' && (
                  <>
                    <TextInput
                      label="Código de Administrador"
                      value={codigoAdmin}
                      onChangeText={setCodigoAdmin}
                      mode="outlined"
                      placeholder="Ex: ADMIN123"
                      style={styles.input}
                      left={<TextInput.Icon icon="shield-key-outline" />}
                    />
                    <HelperText type="info">
                      Obrigatório para registrar conta com privilégios de Admin/Técnico
                    </HelperText>
                  </>
                )}
              </View>
            )}

            <Button
              mode="contained"
              onPress={handleSubmeter}
              loading={loading}
              disabled={loading}
              style={styles.submitButton}
              contentStyle={{ height: 48 }}
              buttonColor="#4f46e5"
            >
              {modo === 'login' ? 'Entrar no Sistema' : 'Finalizar Cadastro'}
            </Button>
          </Card.Content>
        </Card>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  scrollContainer: {
    flexGrow: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  cardWrapper: {
    width: '100%',
  },
  card: {
    borderRadius: 16,
    elevation: 4,
  },
  cardContent: {
    padding: 28,
  },
  header: {
    alignItems: 'center',
    marginBottom: 20,
  },
  logoBadge: {
    width: 56,
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  title: {
    fontWeight: '700',
    marginBottom: 4,
    textAlign: 'center',
  },
  segmented: {
    marginBottom: 20,
  },
  input: {
    marginBottom: 14,
  },
  roleContainer: {
    marginTop: 6,
    marginBottom: 6,
  },
  submitButton: {
    marginTop: 12,
    borderRadius: 10,
  },
  errorBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fee2e2',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    gap: 8,
  },
  errorText: {
    color: '#991b1b',
    fontSize: 13,
    flex: 1,
  },
});
