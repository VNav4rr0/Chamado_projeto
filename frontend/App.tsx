import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Provider as PaperProvider, Snackbar, Text } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';

import { ChamadoProvider, useChamado } from './src/context/ChamadoContext';
import { lightTheme, darkTheme } from './src/theme/theme';
import { WebNavbar, WebTabKey } from './src/components/WebNavbar';
import { AuthScreen } from './src/screens/AuthScreen';
import { ChamadosListScreen } from './src/screens/ChamadosListScreen';
import { NovoChamadoScreen } from './src/screens/NovoChamadoScreen';
import { ChamadoDetailScreen } from './src/screens/ChamadoDetailScreen';
import { FilaAtendimentoScreen } from './src/screens/FilaAtendimentoScreen';
import { GestaoScreen } from './src/screens/GestaoScreen';
import { RelatorioScreen } from './src/screens/RelatorioScreen';
import { Chamado } from './src/types/chamado';

const MainApp: React.FC = () => {
  const { isDarkMode, snackbarMessage, dismissSnackbar, usuario } = useChamado();
  const theme = isDarkMode ? darkTheme : lightTheme;

  const [activeTab, setActiveTab] = useState<WebTabKey>('chamados');
  const [selectedChamadoId, setSelectedChamadoId] = useState<string | null>(null);

  const handleSelectChamado = (chamado: Chamado) => {
    setSelectedChamadoId(chamado.id);
  };

  const handleBackFromDetail = () => {
    setSelectedChamadoId(null);
  };

  const handleNovoChamadoSuccess = (chamadoId: string) => {
    setSelectedChamadoId(chamadoId);
    setActiveTab('chamados');
  };

  // Se o usuário não estiver logado, exibe a tela de Login / Cadastro
  if (!usuario) {
    return (
      <PaperProvider theme={theme}>
        <View style={[styles.rootContainer, { backgroundColor: theme.colors.background }]}>
          <StatusBar style={isDarkMode ? 'light' : 'dark'} />
          <AuthScreen />
          <Snackbar
            visible={!!snackbarMessage}
            onDismiss={dismissSnackbar}
            duration={4000}
            style={styles.webSnackbar}
          >
            <Text style={{ color: '#ffffff', fontWeight: '700', fontSize: 13.5 }}>
              {snackbarMessage}
            </Text>
          </Snackbar>
        </View>
      </PaperProvider>
    );
  }

  const renderCurrentScreen = () => {
    if (selectedChamadoId) {
      return (
        <ChamadoDetailScreen
          chamadoId={selectedChamadoId}
          onBack={handleBackFromDetail}
        />
      );
    }

    const isAdmin = usuario.role === 'ADMIN';

    switch (activeTab) {
      case 'chamados':
        return (
          <ChamadosListScreen
            onSelectChamado={handleSelectChamado}
            onOpenNovoChamado={() => setActiveTab('novo')}
          />
        );
      case 'novo':
        return (
          <NovoChamadoScreen
            onSuccess={handleNovoChamadoSuccess}
            onCancel={() => setActiveTab('chamados')}
          />
        );
      case 'fila':
        return isAdmin ? <FilaAtendimentoScreen /> : <ChamadosListScreen onSelectChamado={handleSelectChamado} onOpenNovoChamado={() => setActiveTab('novo')} />;
      case 'gestao':
        return isAdmin ? <GestaoScreen /> : <ChamadosListScreen onSelectChamado={handleSelectChamado} onOpenNovoChamado={() => setActiveTab('novo')} />;
      case 'relatorio':
        return isAdmin ? <RelatorioScreen /> : <ChamadosListScreen onSelectChamado={handleSelectChamado} onOpenNovoChamado={() => setActiveTab('novo')} />;
      default:
        return (
          <ChamadosListScreen
            onSelectChamado={handleSelectChamado}
            onOpenNovoChamado={() => setActiveTab('novo')}
          />
        );
    }
  };

  return (
    <PaperProvider theme={theme}>
      <View style={[styles.rootContainer, { backgroundColor: theme.colors.background }]}>
        <StatusBar style={isDarkMode ? 'light' : 'dark'} />

        <WebNavbar
          activeTab={activeTab}
          onSelectTab={(tab) => {
            setSelectedChamadoId(null);
            setActiveTab(tab);
          }}
          selectedChamadoId={selectedChamadoId}
          onBackToChamados={handleBackFromDetail}
        />

        <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          {renderCurrentScreen()}
        </main>

        <Snackbar
          visible={!!snackbarMessage}
          onDismiss={dismissSnackbar}
          duration={4000}
          action={{
            label: 'FECHAR',
            onPress: dismissSnackbar,
            textColor: '#818cf8',
          }}
          style={styles.webSnackbar}
        >
          <Text style={{ color: '#ffffff', fontWeight: '700', fontSize: 13.5 }}>
            {snackbarMessage}
          </Text>
        </Snackbar>
      </View>
    </PaperProvider>
  );
};

export default function App() {
  return (
    <SafeAreaProvider>
      <ChamadoProvider>
        <MainApp />
      </ChamadoProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  rootContainer: {
    flex: 1,
    minHeight: '100vh' as any,
  },
  webSnackbar: {
    backgroundColor: '#0f172a',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
    maxWidth: 480,
    alignSelf: 'flex-end',
    marginRight: 24,
    marginBottom: 24,
  },
});
