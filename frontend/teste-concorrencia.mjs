const BASE_URL_GATEWAY = process.env.BASE_URL_GATEWAY || 'http://localhost:8080';
const BASE_URL_CHAMADO = process.env.BASE_URL_CHAMADO || `${BASE_URL_GATEWAY}/api/chamados`;
const BASE_URL_ATENDIMENTO = process.env.BASE_URL_ATENDIMENTO || `${BASE_URL_GATEWAY}/api/tecnicos`;

const CATEGORIA_TESTE = process.env.CATEGORIA || 'REDE';
const CAPACIDADE_MAXIMA_REDE = Number(process.env.CAPACIDADE_MAXIMA || 6);
const QTD_REQUISICOES = Number(process.env.QTD_REQUISICOES || 15);

const POLL_TENTATIVAS = Number(process.env.POLL_TENTATIVAS || 20);
const POLL_INTERVALO_MS = Number(process.env.POLL_INTERVALO_MS || 500);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const counters = {
    chamados_em_atendimento: 0,
    chamados_aguardando_tecnico: 0,
    chamados_timeout_polling: 0,
    erro_inesperado: 0,
};

async function executarVU(vuId) {
    const clientId = `user-aluno-${vuId}`;

    const payload = {
        titulo: `Incidente de Concorrência VU-${vuId}`,
        descricao: `Teste de estresse de alocação concorrente na categoria ${CATEGORIA_TESTE}`,
        categoria: CATEGORIA_TESTE,
        prioridade: 'ALTA',
    };

    let chamadoId;

    try {
        // Ponto-chave: Requisição HTTP POST aceita com 202 Accepted (Outbox assíncrono)
        const res = await fetch(BASE_URL_CHAMADO, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Id': clientId,
            },
            body: JSON.stringify(payload),
        });

        if (res.status !== 200 && res.status !== 202) {
            counters.erro_inesperado++;
            return;
        }

        const data = await res.json();
        chamadoId = data.id;
    } catch {
        counters.erro_inesperado++;
        return;
    }

    // Ponto-chave: Polling aguardando alocação ou represamento na fila RabbitMQ
    let statusFinal = null;
    for (let i = 0; i < POLL_TENTATIVAS; i++) {
        await sleep(POLL_INTERVALO_MS);

        try {
            const resStatus = await fetch(`${BASE_URL_CHAMADO}/${chamadoId}`, {
                headers: { 'X-Usuario-Id': clientId },
            });

            if (!resStatus.ok) continue;

            const chamado = await resStatus.json();
            if (chamado.status === 'EM_ATENDIMENTO' || chamado.status === 'AGUARDANDO_TECNICO') {
                statusFinal = chamado.status;
                break;
            }
        } catch {
        }
    }

    if (statusFinal === 'EM_ATENDIMENTO') {
        counters.chamados_em_atendimento++;
    } else if (statusFinal === 'AGUARDANDO_TECNICO') {
        counters.chamados_aguardando_tecnico++;
    } else {
        counters.chamados_timeout_polling++;
    }
}

async function run() {
    console.log('\n============================================================');
    console.log('       INICIANDO TESTE DE CONCORRÊNCIA SIMULTÂNEA');
    console.log('============================================================');
    console.log(`URL Gateway / Chamados:     ${BASE_URL_CHAMADO}`);
    console.log(`Categoria sob teste:        ${CATEGORIA_TESTE}`);
    console.log(`Capacidade máxima esperada: ${CAPACIDADE_MAXIMA_REDE}`);
    console.log(`Requisições simultâneas:    ${QTD_REQUISICOES}`);
    console.log('------------------------------------------------------------');
    console.log('Disparando requisições em paralelo...');

    const start = Date.now();
    const promessas = [];
    for (let i = 1; i <= QTD_REQUISICOES; i++) {
        promessas.push(executarVU(i));
    }

    await Promise.all(promessas);
    const duration = ((Date.now() - start) / 1000).toFixed(2);

    console.log(`\nTodas as requisições concluídas em ${duration}s.`);
    console.log('\n============================================================');
    console.log('                  MÉTRICAS COLETADAS');
    console.log('============================================================');
    console.log(`  chamados_em_atendimento:     ${counters.chamados_em_atendimento} (Esperado: ${Math.min(CAPACIDADE_MAXIMA_REDE, QTD_REQUISICOES)})`);
    console.log(`  chamados_aguardando_tecnico: ${counters.chamados_aguardando_tecnico} (Esperado: ${Math.max(0, QTD_REQUISICOES - CAPACIDADE_MAXIMA_REDE)})`);
    console.log(`  chamados_timeout_polling:    ${counters.chamados_timeout_polling} (Esperado: 0)`);
    console.log(`  erro_inesperado:             ${counters.erro_inesperado} (Esperado: 0)`);
    console.log('============================================================\n');

    // Ponto-chave: Verificação de atomicidade de carga no banco
    try {
        const resCarga = await fetch(`${BASE_URL_ATENDIMENTO}/carga`, {
            headers: { 'X-Usuario-Id': 'admin-auditoria' },
        });

        if (resCarga.ok) {
            const painel = await resCarga.json();
            const tecnicosRede = (painel.tecnicos || []).filter((t) => t.especialidade === CATEGORIA_TESTE);

            console.log('Status da Equipe no Banco de Dados:');
            let cargaTotal = 0;
            let houveOverAllocation = false;

            tecnicosRede.forEach((t) => {
                console.log(`  • ${t.nome}: ${t.cargaAtual} / ${t.capacidadeMax} chamados`);
                cargaTotal += t.cargaAtual;
                if (t.cargaAtual > t.capacidadeMax) {
                    houveOverAllocation = true;
                }
            });

            console.log('------------------------------------------------------------');
            if (houveOverAllocation) {
                console.error('❌ FALHA CRÍTICA: Técnico ultrapassou capacidade máxima!');
            } else if (cargaTotal !== Math.min(CAPACIDADE_MAXIMA_REDE, QTD_REQUISICOES)) {
                console.warn(`⚠️ ATENÇÃO: Carga ocupada (${cargaTotal}) diferente do esperado.`);
            } else {
                console.log('✅ SUCESSO: Concorrência protegida com exatidão e sem over-allocation!');
            }
        }
    } catch (e) {
        console.log('Não foi possível consultar o endpoint de técnicos:', e.message);
    }
}

run();
