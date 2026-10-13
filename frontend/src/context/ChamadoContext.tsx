import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  Chamado,
  CriarChamadoDTO,
  AlterarStatusDTO,
  UsuarioLogado,
  StatusChamado,
} from '../types/chamado';
import { chamadoService } from '../services/chamadoService';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface ChamadoContextData {
  usuario: UsuarioLogado | null;
  chamados: Chamado[];
  loading: boolean;
  refreshing: boolean;
  isDarkMode: boolean;
  snackbarMessage: string | null;
  login: (email: string, senha: string) => Promise<void>;
  cadastro: (dados: {
    nome: string;
    email: string;
    senha: string;
    role: 'USUARIO' | 'ADMIN';
    codigoAdmin?: string;
  }) => Promise<void>;
  logout: () => Promise<void>;
  carregarChamados: (status?: StatusChamado) => Promise<void>;
  criarChamado: (dto: CriarChamadoDTO) => Promise<{ id: string; protocolo: string }>;
  alterarStatus: (id: string, dto: AlterarStatusDTO) => Promise<Chamado>;
  toggleTheme: () => void;
  dismissSnackbar: () => void;
  showSnackbar: (msg: string) => void;
}

const ChamadoContext = createContext<ChamadoContextData>({} as ChamadoContextData);

export const ChamadoProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(null);
  const [chamados, setChamados] = useState<Chamado[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [isDarkMode, setIsDarkMode] = useState<boolean>(false);
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  const showSnackbar = useCallback((msg: string) => {
    setSnackbarMessage(msg);
  }, []);

  const dismissSnackbar = useCallback(() => {
    setSnackbarMessage(null);
  }, []);

  const carregarChamados = useCallback(async (filtroStatus?: StatusChamado) => {
    if (!usuario) return;
    setRefreshing(true);
    try {
      const lista = await chamadoService.getChamados(filtroStatus);
      setChamados(lista);
    } catch (error: any) {
      if (error?.status === 401) {
        setUsuario(null);
        await AsyncStorage.removeItem('@usuario_sessao');
        showSnackbar('Sessão expirada. Faça login novamente.');
      } else {
        console.warn('Erro ao carregar chamados:', error);
        showSnackbar(`Erro ao buscar chamados: ${error.message || 'Falha de conexão'}`);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [usuario, showSnackbar]);

  // Carrega configurações e sessão do usuário salvas
  useEffect(() => {
    (async () => {
      try {
        const [savedTheme, savedUser] = await Promise.all([
          AsyncStorage.getItem('@chamados_theme'),
          AsyncStorage.getItem('@usuario_sessao'),
        ]);

        if (savedTheme !== null) {
          setIsDarkMode(savedTheme === 'dark');
        }

        if (savedUser) {
          try {
            const userObj = JSON.parse(savedUser);
            setUsuario(userObj);
          } catch {
            await AsyncStorage.removeItem('@usuario_sessao');
          }
        }
      } catch (e) {
        console.warn('Erro ao restaurar dados locais:', e);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // Recarrega chamados quando usuário se autentica
  useEffect(() => {
    if (usuario) {
      carregarChamados();
    } else {
      setChamados([]);
    }
  }, [usuario, carregarChamados]);

  const login = async (email: string, senha: string) => {
    setLoading(true);
    try {
      const user = await chamadoService.login(email, senha);
      setUsuario(user);
      await AsyncStorage.setItem('@usuario_sessao', JSON.stringify(user));
      showSnackbar(`Bem-vindo(a), ${user.nome}!`);
    } catch (err: any) {
      showSnackbar(`Falha no login: ${err.message || 'Verifique suas credenciais'}`);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const cadastro = async (dados: {
    nome: string;
    email: string;
    senha: string;
    role: 'USUARIO' | 'ADMIN';
    codigoAdmin?: string;
  }) => {
    setLoading(true);
    try {
      const user = await chamadoService.cadastro(dados);
      setUsuario(user);
      await AsyncStorage.setItem('@usuario_sessao', JSON.stringify(user));
      showSnackbar(`Conta criada com sucesso! Bem-vindo(a), ${user.nome}!`);
    } catch (err: any) {
      showSnackbar(`Falha no cadastro: ${err.message || 'Erro ao registrar conta'}`);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await chamadoService.logout().catch(() => {});
    } finally {
      setUsuario(null);
      setChamados([]);
      await AsyncStorage.removeItem('@usuario_sessao');
      showSnackbar('Você saiu da sua conta.');
      setLoading(false);
    }
  };

  const toggleTheme = () => {
    const nextVal = !isDarkMode;
    setIsDarkMode(nextVal);
    AsyncStorage.setItem('@chamados_theme', nextVal ? 'dark' : 'light');
  };

  const criarChamado = async (dto: CriarChamadoDTO) => {
    setLoading(true);
    try {
      const res = await chamadoService.criarChamado(dto);
      await carregarChamados();
      showSnackbar(`✅ Chamado ${res.protocolo} aberto com sucesso!`);
      return res;
    } catch (err: any) {
      showSnackbar(`❌ Falha ao abrir chamado: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const alterarStatus = async (id: string, dto: AlterarStatusDTO) => {
    setLoading(true);
    try {
      const chamadoAtualizado = await chamadoService.alterarStatus(id, dto);
      await carregarChamados();
      showSnackbar(`Chamado atualizado para "${chamadoAtualizado.status}"`);
      return chamadoAtualizado;
    } catch (err: any) {
      showSnackbar(`❌ Erro ao atualizar chamado: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return (
    <ChamadoContext.Provider
      value={{
        usuario,
        chamados,
        loading,
        refreshing,
        isDarkMode,
        snackbarMessage,
        login,
        cadastro,
        logout,
        carregarChamados,
        criarChamado,
        alterarStatus,
        toggleTheme,
        dismissSnackbar,
        showSnackbar,
      }}
    >
      {children}
    </ChamadoContext.Provider>
  );
};

export const useChamado = () => useContext(ChamadoContext);
