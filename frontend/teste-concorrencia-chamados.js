import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL_GATEWAY = __ENV.BASE_URL_GATEWAY || 'http://localhost:8080';
const BASE_URL_CHAMADO = __ENV.BASE_URL_CHAMADO || `${BASE_URL_GATEWAY}/api/chamados`;
const BASE_URL_ATENDIMENTO = __ENV.BASE_URL_ATENDIMENTO || `${BASE_URL_GATEWAY}/api/tecnicos`;

const CATEGORIA_TESTE = __ENV.CATEGORIA || 'REDE';
const CAPACIDADE_MAXIMA_REDE = Number(__ENV.CAPACIDADE_MAXIMA || 6);
const QTD_REQUISICOES = Number(__ENV.QTD_REQUISICOES || 15);

const POLL_TENTATIVAS = Number(__ENV.POLL_TENTATIVAS || 20);
const POLL_INTERVALO = Number(__ENV.POLL_INTERVALO || 0.5);

const chamadosEmAtendimento = new Counter('chamados_em_atendimento');
const chamadosAguardandoTecnico = new Counter('chamados_aguardando_tecnico');
const timeoutPolling = new Counter('chamados_timeout_polling');
const erroInesperado = new Counter('erro_inesperado');

export const options = {
    scenarios: {
        abertura_simultanea: {
            executor: 'shared-iterations',
            vus: QTD_REQUISICOES,
            iterations: QTD_REQUISICOES,
            maxDuration: '60s',
        },
    },
};

export default function () {
    const clientId = `user-aluno-${__VU}`;

    const payload = JSON.stringify({
        titulo: `Incidente de Concorrência VU-${__VU}`,
        descricao: `Teste de estresse de alocação concorrente na categoria ${CATEGORIA_TESTE}`,
        categoria: CATEGORIA_TESTE,
        prioridade: 'ALTA',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Usuario-Id': clientId,
        },
    };

    // Ponto-chave: Envio assíncrono com padrão Transactional Outbox (espera-se HTTP 202)
    const resAbertura = http.post(BASE_URL_CHAMADO, payload, params);

    const ok = check(resAbertura, {
        'chamado aceito (200 ou 202)': (r) => r.status === 200 || r.status === 202,
    });

    if (!ok) {
        erroInesperado.add(1);
        return;
    }

    let chamadoId;
    try {
        chamadoId = JSON.parse(resAbertura.body).id;
    } catch {
        erroInesperado.add(1);
        return;
    }

    // Ponto-chave: Polling para aguardar o consumo do evento no RabbitMQ
    let statusFinal = null;
    for (let i = 0; i < POLL_TENTATIVAS; i++) {
        sleep(POLL_INTERVALO);

        const resStatus = http.get(`${BASE_URL_CHAMADO}/${chamadoId}`, params);
        if (resStatus.status !== 200) continue;

        try {
            const chamado = JSON.parse(resStatus.body);
            if (chamado.status === 'EM_ATENDIMENTO' || chamado.status === 'AGUARDANDO_TECNICO') {
                statusFinal = chamado.status;
                break;
            }
        } catch {
        }
    }

    if (statusFinal === 'EM_ATENDIMENTO') {
        chamadosEmAtendimento.add(1);
    } else if (statusFinal === 'AGUARDANDO_TECNICO') {
        chamadosAguardandoTecnico.add(1);
    } else {
        timeoutPolling.add(1);
    }
}

// Ponto-chave: Validação de consistência do banco e proteção contra Over-Allocation
export function teardown() {
    const resCarga = http.get(`${BASE_URL_ATENDIMENTO}/carga`, {
        headers: { 'Content-Type': 'application/json', 'X-Usuario-Id': 'admin-auditoria' },
    });

    console.log('\n============================================================');
    console.log('         RELATÓRIO DO TESTE DE CONCORRÊNCIA');
    console.log('============================================================');
    console.log(`Categoria testada:           ${CATEGORIA_TESTE}`);
    console.log(`Capacidade máxima de vagas:  ${CAPACIDADE_MAXIMA_REDE}`);
    console.log(`Total de chamados enviados:  ${QTD_REQUISICOES}`);
    console.log('------------------------------------------------------------');

    if (resCarga.status === 200) {
        try {
            const painel = JSON.parse(resCarga.body);
            const tecnicosRede = (painel.tecnicos || []).filter((t) => t.especialidade === CATEGORIA_TESTE);

            let cargaOcupadaTotal = 0;
            let houveOverAllocation = false;

            console.log('Status dos Técnicos de ' + CATEGORIA_TESTE + ':');
            tecnicosRede.forEach((t) => {
                console.log(`  • ${t.nome}: ${t.cargaAtual} / ${t.capacidadeMax} chamados`);
                cargaOcupadaTotal += t.cargaAtual;
                if (t.cargaAtual > t.capacidadeMax) {
                    houveOverAllocation = true;
                }
            });

            console.log('------------------------------------------------------------');
            console.log(`Carga total ocupada no banco: ${cargaOcupadaTotal} / ${CAPACIDADE_MAXIMA_REDE}`);
            console.log('============================================================');
            console.log('MÉTRICAS ESPERADAS NO K6:');
            console.log(`  chamados_em_atendimento   → deve ser igual a ${Math.min(CAPACIDADE_MAXIMA_REDE, QTD_REQUISICOES)}`);
            console.log(`  chamados_aguardando_tecnico → deve ser igual a ${Math.max(0, QTD_REQUISICOES - CAPACIDADE_MAXIMA_REDE)}`);
            console.log('  chamados_timeout_polling   → deve ser 0');
            console.log('  erro_inesperado            → deve ser 0');
            console.log('============================================================\n');

            if (houveOverAllocation) {
                console.error('❌ FALHA CRÍTICA DE CONCORRÊNCIA: Um técnico ultrapassou a capacidade máxima! (Race Condition detectada).');
            } else if (cargaOcupadaTotal !== Math.min(CAPACIDADE_MAXIMA_REDE, QTD_REQUISICOES)) {
                console.error(`⚠️ ATENÇÃO: Carga final ocupada (${cargaOcupadaTotal}) não bate com o esperado.`);
            } else {
                console.log('✅ SUCESSO: A concorrência foi protegida perfeitamente e os excedentes foram represados sem over-allocation!');
            }
        } catch (e) {
            console.log('Erro ao ler resposta de técnicos:', e.message);
        }
    }
}
