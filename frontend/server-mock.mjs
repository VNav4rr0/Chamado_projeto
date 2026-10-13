import http from 'http';

const PORT = process.env.PORT || 8080;

let tecnicos = [
    { id: 1, nome: 'Roberto Redes', especialidade: 'REDE', cargaAtual: 0, capacidadeMax: 2, ativo: true },
    { id: 2, nome: 'Renata Roteadores', especialidade: 'REDE', cargaAtual: 0, capacidadeMax: 4, ativo: true },
    { id: 3, nome: 'Hugo Hardware', especialidade: 'HARDWARE', cargaAtual: 0, capacidadeMax: 3, ativo: true },
    { id: 4, nome: 'Helena Hard', especialidade: 'HARDWARE', cargaAtual: 0, capacidadeMax: 5, ativo: true },
    { id: 5, nome: 'Sofia Software', especialidade: 'SOFTWARE', cargaAtual: 0, capacidadeMax: 4, ativo: true },
    { id: 6, nome: 'Samuel Sistemas', especialidade: 'SOFTWARE', cargaAtual: 0, capacidadeMax: 2, ativo: true },
    { id: 7, nome: 'Alice Acessos', especialidade: 'ACESSO', cargaAtual: 0, capacidadeMax: 3, ativo: true },
    { id: 8, nome: 'Arthur Autenticação', especialidade: 'ACESSO', cargaAtual: 0, capacidadeMax: 6, ativo: true },
];

const chamados = new Map();
let seqProtocolo = 100;

function parseJsonBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', (chunk) => (body += chunk));
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (err) {
                reject(err);
            }
        });
    });
}

const server = http.createServer(async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Usuario-Id, Authorization');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const url = new URL(req.url, `http://${req.headers.host}`);
    const pathname = url.pathname;

    // Ponto-chave: POST /api/chamados responde 202 Accepted imediatamente (Transactional Outbox)
    if (req.method === 'POST' && (pathname === '/api/chamados' || pathname === '/chamados')) {
        try {
            const dto = await parseJsonBody(req);
            const chamadoId = `ch-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
            const usuarioId = req.headers['x-usuario-id'] || 'usuario-anonimo';

            const novoChamado = {
                id: chamadoId,
                usuarioId,
                titulo: dto.titulo || 'Chamado sem título',
                descricao: dto.descricao || '',
                categoria: dto.categoria || 'REDE',
                prioridade: dto.prioridade || 'MEDIA',
                status: 'ABERTO',
                protocolo: null,
                tecnicoId: null,
                tecnicoNome: null,
                criadoEm: new Date().toISOString(),
            };

            chamados.set(chamadoId, novoChamado);

            // Ponto-chave: Desacoplamento assíncrono - processado após retorno HTTP via RabbitMQ
            setTimeout(() => {
                processarMensageriaRabbitMQ(chamadoId);
            }, 400);

            res.writeHead(202, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                id: chamadoId,
                status: 'ABERTO',
                mensagem: 'Chamado aceito para processamento assíncrono (HTTP 202 Accepted).',
            }));
        } catch (e) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ erro: 'Payload inválido: ' + e.message }));
        }
        return;
    }

    // Ponto-chave: Consulta de status assíncrono para polling de clientes
    if (req.method === 'GET' && (pathname.startsWith('/api/chamados/') || pathname.startsWith('/chamados/'))) {
        const id = pathname.split('/').pop();
        const chamado = chamados.get(id);

        if (!chamado) {
            res.writeHead(404, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ erro: 'Chamado não encontrado' }));
            return;
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(chamado));
        return;
    }

    // Ponto-chave: Auditoria de capacidade e ocupação concorrente da equipe
    if (req.method === 'GET' && (pathname === '/api/tecnicos/carga' || pathname === '/tecnicos/carga')) {
        const totalCarga = tecnicos.reduce((acc, t) => acc + t.cargaAtual, 0);
        const totalCapacidade = tecnicos.reduce((acc, t) => acc + t.capacidadeMax, 0);
        const percentualGeral = totalCapacidade > 0 ? Math.round((totalCarga / totalCapacidade) * 100) : 0;

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            tecnicos,
            totalCarga,
            totalCapacidade,
            percentualGeral,
        }));
        return;
    }

    if (req.method === 'POST' && pathname === '/api/reset') {
        tecnicos.forEach((t) => (t.cargaAtual = 0));
        chamados.clear();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ mensagem: 'Estado resetado com sucesso!' }));
        return;
    }

    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ erro: 'Rota não encontrada' }));
});

// Ponto-chave: Atribuição atômica por menor carga com proteção contra Over-Allocation
function processarMensageriaRabbitMQ(chamadoId) {
    const chamado = chamados.get(chamadoId);
    if (!chamado || chamado.status !== 'ABERTO') return;

    const disponiveis = tecnicos
        .filter((t) => t.ativo && t.especialidade === chamado.categoria)
        .sort((a, b) => a.cargaAtual - b.cargaAtual);

    const tecnicoAlocado = disponiveis.find((t) => t.cargaAtual < t.capacidadeMax);

    if (tecnicoAlocado) {
        tecnicoAlocado.cargaAtual += 1;
        seqProtocolo++;

        chamado.status = 'EM_ATENDIMENTO';
        chamado.tecnicoId = tecnicoAlocado.id;
        chamado.tecnicoNome = tecnicoAlocado.nome;
        chamado.protocolo = `CH-2026-${String(seqProtocolo).padStart(6, '0')}`;
    } else {
        // Ponto-chave: Represamento na fila quando capacidade está esgotada (AtribuicaoFalhouEvent)
        chamado.status = 'AGUARDANDO_TECNICO';
    }
}

server.listen(PORT, () => {
    console.log(`\n============================================================`);
    console.log(`  🚀 SERVIDOR CENTRAL DE CHAMADOS ATIVO NA PORTA ${PORT}`);
    console.log(`============================================================`);
    console.log(`Endpoints prontos para teste de concorrência:`);
    console.log(`  • POST http://localhost:${PORT}/api/chamados       (Abertura)`);
    console.log(`  • GET  http://localhost:${PORT}/api/chamados/:id   (Polling)`);
    console.log(`  • GET  http://localhost:${PORT}/api/tecnicos/carga (Auditoria)`);
    console.log(`  • POST http://localhost:${PORT}/api/reset          (Zerar dados)`);
    console.log(`============================================================\n`);
});
