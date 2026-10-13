// mockService.ts - Desativado em favor da API real do Spring Cloud Gateway
export const mockService = {
  getChamados: async () => [],
  getChamadoById: async () => null,
  criarChamado: async () => ({ id: '', status: '', mensagem: '' }),
  resolverChamado: async () => ({} as any),
  getPainelCarga: async () => ({ tecnicos: [], totalCarga: 0, totalCapacidade: 0, percentualGeral: 0 }),
  resetData: async () => {},
  subscribe: () => () => {},
};
