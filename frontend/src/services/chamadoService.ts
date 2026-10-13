import { api } from './api';
import {
  Chamado,
  CriarChamadoDTO,
  ChamadoCriadoResponse,
  AlterarStatusDTO,
  UsuarioLogado,
  ItemFila,
  Departamento,
  Atendente,
  Sla,
  RelatorioMensal,
  StatusChamado,
} from '../types/chamado';

class ChamadoService {
  // --- Autenticação ---
  public async login(email: string, senha: string): Promise<UsuarioLogado> {
    return api.post<UsuarioLogado>('/auth/login', { email, senha });
  }

  public async cadastro(dados: {
    nome: string;
    email: string;
    senha: string;
    role: 'USUARIO' | 'ADMIN';
    codigoAdmin?: string;
  }): Promise<UsuarioLogado> {
    return api.post<UsuarioLogado>('/auth/register', dados);
  }

  public async logout(): Promise<void> {
    await api.post('/auth/logout', {});
  }

  // --- Chamados ---
  public async getChamados(status?: StatusChamado): Promise<Chamado[]> {
    const query = status ? `?status=${status}` : '';
    const resultado = await api.get<any>(`/chamados${query}`);
    // O backend pode retornar lista direta ou página Spring Data { content: [...] }
    if (resultado && Array.isArray(resultado.content)) {
      return resultado.content;
    }
    return Array.isArray(resultado) ? resultado : [];
  }

  public async getChamadoById(id: string): Promise<Chamado> {
    return api.get<Chamado>(`/chamados/${id}`);
  }

  public async criarChamado(dto: CriarChamadoDTO): Promise<ChamadoCriadoResponse> {
    return api.post<ChamadoCriadoResponse>('/chamados', dto);
  }

  public async alterarStatus(id: string, dto: AlterarStatusDTO): Promise<Chamado> {
    return api.patch<Chamado>(`/chamados/${id}/status`, dto);
  }

  // --- Atendimento / Cadastros ---
  public async getCategorias(): Promise<string[]> {
    return api.get<string[]>('/atendimento/categorias');
  }

  public async getDepartamentos(): Promise<Departamento[]> {
    return api.get<Departamento[]>('/atendimento/departamentos');
  }

  public async criarDepartamento(dados: { nome: string; categorias: string[] }): Promise<Departamento> {
    return api.post<Departamento>('/atendimento/departamentos', dados);
  }

  public async getAtendentes(departamentoId?: string): Promise<Atendente[]> {
    const query = departamentoId ? `?departamentoId=${departamentoId}` : '';
    return api.get<Atendente[]>(`/atendimento/atendentes${query}`);
  }

  public async criarAtendente(dados: {
    id?: string;
    nome: string;
    departamentoId: string;
    capacidade?: number;
  }): Promise<Atendente> {
    return api.post<Atendente>('/atendimento/atendentes', dados);
  }

  public async getSlas(): Promise<Sla[]> {
    return api.get<Sla[]>('/atendimento/slas');
  }

  public async atualizarSla(prioridade: string, horas: number): Promise<Sla> {
    return api.put<Sla>(`/atendimento/slas/${prioridade}`, { horas });
  }

  // --- Fila de Atendimento ---
  public async getFila(departamentoId?: string, status?: string): Promise<ItemFila[]> {
    const params = new URLSearchParams();
    if (departamentoId) params.append('departamentoId', departamentoId);
    if (status) params.append('status', status);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return api.get<ItemFila[]>(`/atendimento/fila${qs}`);
  }

  public async assumirItemFila(chamadoId: string): Promise<ItemFila> {
    return api.post<ItemFila>(`/atendimento/fila/${chamadoId}/assumir`, {});
  }

  // --- Relatório Mensal ---
  public async getRelatorioMensal(ano: number, mes: number): Promise<RelatorioMensal> {
    return api.get<RelatorioMensal>(`/relatorio/mensal?ano=${ano}&mes=${mes}`);
  }
}

export const chamadoService = new ChamadoService();
