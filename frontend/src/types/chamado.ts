export type PrioridadeChamado = 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAIXA';

export type StatusChamado = 
  | 'CRIADO'
  | 'EM_ANALISE'
  | 'EM_ANDAMENTO'
  | 'AGUARDANDO_TERCEIRO'
  | 'RESOLVIDO'
  | 'FECHADO'
  | 'REJEITADO';

export const STATUS_LABELS: Record<StatusChamado, string> = {
  CRIADO: 'Criado',
  EM_ANALISE: 'Em Triagem / Fila',
  EM_ANDAMENTO: 'Em Andamento',
  AGUARDANDO_TERCEIRO: 'Aguardando Terceiro',
  RESOLVIDO: 'Resolvido',
  FECHADO: 'Fechado',
  REJEITADO: 'Rejeitado',
};

export const STATUS_COLORS: Record<StatusChamado, { bg: string; text: string; border: string }> = {
  CRIADO: { bg: '#e0e7ff', text: '#3730a3', border: '#c7d2fe' },
  EM_ANALISE: { bg: '#fef3c7', text: '#92400e', border: '#fde68a' },
  EM_ANDAMENTO: { bg: '#dbeafe', text: '#1e40af', border: '#bfdbfe' },
  AGUARDANDO_TERCEIRO: { bg: '#ffedd5', text: '#9a3412', border: '#fed7aa' },
  RESOLVIDO: { bg: '#dcfce7', text: '#166534', border: '#bbf7d0' },
  FECHADO: { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' },
  REJEITADO: { bg: '#fee2e2', text: '#991b1b', border: '#fecaca' },
};

export interface HistoricoChamado {
  statusAnterior: StatusChamado | null;
  statusNovo: StatusChamado;
  alteradoPor: string;
  alteradoEm: string;
  motivo?: string;
  horasTrabalhadas?: number;
  custo?: number;
}

export interface Chamado {
  id: string;
  protocolo: string;
  solicitanteId: string;
  solicitanteNome: string;
  solicitanteEmail: string;
  titulo: string;
  descricao: string;
  categoria: string;
  prioridade: PrioridadeChamado;
  status: StatusChamado;
  departamentoId?: string;
  departamentoNome?: string;
  atendenteId?: string;
  atendenteNome?: string;
  prazoSla?: string;
  criadoEm: string;
  atualizadoEm: string;
  resolvidoEm?: string;
  fechadoEm?: string;
  horasTrabalhadas?: number;
  custo?: number;
  motivoRejeicao?: string;
  historico: HistoricoChamado[];
}

export interface CriarChamadoDTO {
  titulo: string;
  descricao: string;
  categoria: string;
  prioridade: PrioridadeChamado;
}

export interface ChamadoCriadoResponse {
  id: string;
  protocolo: string;
  status: StatusChamado;
  criadoEm: string;
}

export interface AlterarStatusDTO {
  status: StatusChamado;
  horasTrabalhadas?: number;
  custo?: number;
  motivo?: string;
}

export interface UsuarioLogado {
  id: string;
  nome: string;
  email: string;
  role: 'USUARIO' | 'ADMIN';
}

export interface ItemFila {
  id: string;
  chamadoId: string;
  protocolo: string;
  categoria: string;
  prioridade: PrioridadeChamado;
  departamentoId: string;
  status: 'NA_FILA' | 'ATRIBUIDO' | 'CONCLUIDO' | 'REJEITADO';
  atendenteId?: string;
  prazoSla?: string;
  criadoEm: string;
  atribuidoEm?: string;
  concluidoEm?: string;
  motivoRejeicao?: string;
}

export interface Departamento {
  id: string;
  nome: string;
  categorias: string[];
  ativo: boolean;
}

export interface Atendente {
  id: string;
  nome: string;
  departamentoId: string;
  capacidade: number;
  chamadosAtivos: number;
  ativo: boolean;
}

export interface Sla {
  prioridade: string;
  horas: number;
}

export interface RelatorioMensal {
  ano: number;
  mes: number;
  totalConcluidos: number;
  tempoMedioAtendimentoHoras: number;
  tempoMedioPrimeiraRespostaHoras: number;
  percentualSlaCumprido: number;
  totalHorasTrabalhadas: number;
  custoTotal: number;
  porCategoria: Record<string, number>;
  porPrioridade: Record<string, number>;
  porAtendente: Record<string, number>;
}

export const SLA_HORAS_PADRAO: Record<PrioridadeChamado, number> = {
  CRITICA: 2,
  ALTA: 8,
  MEDIA: 24,
  BAIXA: 72,
};
