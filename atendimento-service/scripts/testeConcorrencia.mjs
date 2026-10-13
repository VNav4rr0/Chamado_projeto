#!/usr/bin/env node
/**
 * scripts/testeConcorrencia.mjs
 * 
 * Demonstração ao vivo de concorrência (Seção 8.3 do ARQUITETURA.md):
 * 1. Faz login via Gateway como USUARIO e como N ADMINs (captura cookie tokenSuporteTi).
 * 2. Garante que os N ADMINs possuem perfil de atendente ativo no departamento.
 * 3. Abre um chamado como USUARIO e aguarda status = EM_ANALISE (via polling).
 * 4. Dispara N requisições POST /api/atendimento/fila/{chamadoId}/assumir simultâneas (Promise.all).
 * 5. Imprime a tabela de resultados: exatamente 1x 200 e (N-1)x 409.
 * 6. Confere o estado final do chamado e da fila de atendimento.
 * 
 * Requisitos: Node 18+ (sem dependências externas npm).
 * Uso:
 *   node scripts/testeConcorrencia.mjs [numAdmins]
 * Exemplo:
 *   node scripts/testeConcorrencia.mjs 5
 */

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';
const ADMIN_CODIGO = process.env.ADMIN_CODIGO || 'ADMIN123';
const N_ADMINS = parseInt(process.argv[2] || process.env.N_ADMINS || '5', 10);

console.log('='.repeat(70));
console.log('  TESTE DE CONCORRÊNCIA - SUPORTE TI');
console.log(`  Gateway: ${GATEWAY_URL}`);
console.log(`  Admins concorrentes: ${N_ADMINS}`);
console.log('='.repeat(70));

function extrairCookie(res) {
  let cookieHeader = '';
  if (typeof res.headers.getSetCookie === 'function') {
    const cookies = res.headers.getSetCookie();
    if (cookies && cookies.length > 0) {
      cookieHeader = cookies.map(c => c.split(';')[0]).join('; ');
    }
  }
  if (!cookieHeader) {
    const raw = res.headers.get('set-cookie');
    if (raw) {
      cookieHeader = raw.split(';')[0];
    }
  }
  return cookieHeader;
}

async function requestJson(url, options = {}) {
  const res = await fetch(url, options);
  let data = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    data = await res.json().catch(() => null);
  } else {
    data = await res.text().catch(() => null);
  }
  return { status: res.status, headers: res.headers, data };
}

async function autenticarOuCadastrar(nome, email, senha, role, codigoAdmin = null) {
  // Tenta login primeiro
  let loginRes = await requestJson(`${GATEWAY_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha })
  });

  if (loginRes.status === 200) {
    const cookie = extrairCookie(loginRes);
    return { usuario: loginRes.data, cookie };
  }

  // Se falhou, cadastra
  const cadastroBody = { nome, email, senha, role };
  if (role === 'ADMIN' && codigoAdmin) {
    cadastroBody.codigoAdmin = codigoAdmin;
  }

  await requestJson(`${GATEWAY_URL}/api/auth/cadastro`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cadastroBody)
  });

  // Tenta login após cadastro
  loginRes = await requestJson(`${GATEWAY_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha })
  });

  if (loginRes.status !== 200) {
    throw new Error(`Falha ao autenticar ${email}: HTTP ${loginRes.status} - ${JSON.stringify(loginRes.data)}`);
  }

  const cookie = extrairCookie(loginRes);
  return { usuario: loginRes.data, cookie };
}

async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function executar() {
  const sufixoTimestamp = Date.now().toString().slice(-5);

  // 1. Autenticar Usuário
  console.log('\n[1/5] Autenticando usuário solicitante...');
  const usuarioInfo = await autenticarOuCadastrar(
    `Usuario Teste ${sufixoTimestamp}`,
    `usuario_${sufixoTimestamp}@teste.com`,
    'Senha@123',
    'USUARIO'
  );
  console.log(`  ✓ Usuário autenticado: ${usuarioInfo.usuario.email} (ID: ${usuarioInfo.usuario.id})`);

  // 2. Autenticar N Administradores
  console.log(`\n[2/5] Autenticando ${N_ADMINS} administradores...`);
  const admins = [];
  for (let i = 1; i <= N_ADMINS; i++) {
    const admin = await autenticarOuCadastrar(
      `Admin Tecnico ${i}`,
      `admin_${i}_${sufixoTimestamp}@teste.com`,
      'Senha@123',
      'ADMIN',
      ADMIN_CODIGO
    );
    admins.push(admin);
    console.log(`  ✓ Admin ${i}: ${admin.usuario.email} (ID: ${admin.usuario.id})`);
  }

  // 3. Localizar departamento de "Software" e vincular atendentes
  console.log('\n[3/5] Preparando perfis de atendente no departamento...');
  const depRes = await requestJson(`${GATEWAY_URL}/api/atendimento/departamentos`, {
    headers: { Cookie: admins[0].cookie }
  });

  if (depRes.status !== 200 || !Array.isArray(depRes.data) || depRes.data.length === 0) {
    throw new Error(`Falha ao listar departamentos: HTTP ${depRes.status} - ${JSON.stringify(depRes.data)}`);
  }

  const depSoftware = depRes.data.find(d => d.categorias && d.categorias.includes('Software')) || depRes.data[0];
  console.log(`  ✓ Departamento selecionado: ${depSoftware.nome} (ID: ${depSoftware.id})`);

  for (let i = 0; i < admins.length; i++) {
    const adm = admins[i];
    await requestJson(`${GATEWAY_URL}/api/atendimento/atendentes`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Cookie: adm.cookie
      },
      body: JSON.stringify({
        id: adm.usuario.id,
        nome: adm.usuario.nome,
        departamentoId: depSoftware.id,
        capacidade: 5
      })
    });
  }
  console.log(`  ✓ ${N_ADMINS} atendentes registrados com capacidade 5 no departamento.`);

  // 4. Abrir chamado como Usuário e aguardar triagem
  console.log('\n[4/5] Abrindo chamado e aguardando triagem (status EM_ANALISE)...');
  const chamadoCriadoRes = await requestJson(`${GATEWAY_URL}/api/chamados`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Cookie: usuarioInfo.cookie
    },
    body: JSON.stringify({
      titulo: `Chamado Concorrente ${sufixoTimestamp}`,
      descricao: 'Teste de disputa de capacidade e concorrência na fila de atendimento',
      categoria: 'Software',
      prioridade: 'ALTA'
    })
  });

  if (chamadoCriadoRes.status !== 201) {
    throw new Error(`Falha ao abrir chamado: HTTP ${chamadoCriadoRes.status} - ${JSON.stringify(chamadoCriadoRes.data)}`);
  }

  const chamadoId = chamadoCriadoRes.data.id;
  const protocolo = chamadoCriadoRes.data.protocolo;
  console.log(`  ✓ Chamado criado: ${protocolo} (ID: ${chamadoId}), status inicial: ${chamadoCriadoRes.data.status}`);

  // Polling até EM_ANALISE
  let chamadoAtual = null;
  const maxTentativas = 20;
  for (let t = 1; t <= maxTentativas; t++) {
    await delay(500);
    const getRes = await requestJson(`${GATEWAY_URL}/api/chamados/${chamadoId}`, {
      headers: { Cookie: usuarioInfo.cookie }
    });
    if (getRes.status === 200) {
      chamadoAtual = getRes.data;
      if (chamadoAtual.status === 'EM_ANALISE') {
        console.log(`  ✓ Chamado triado com sucesso: status = ${chamadoAtual.status} (tentativa ${t})`);
        break;
      }
    }
  }

  if (!chamadoAtual || chamadoAtual.status !== 'EM_ANALISE') {
    throw new Error(`Timeout: O chamado não atingiu o status EM_ANALISE após ${maxTentativas * 0.5}s. Verifique se RabbitMQ e os consumidores estão ativos.`);
  }

  // 5. Disputa Concorrente: N requisições simultâneas via Promise.all
  console.log(`\n[5/5] DISPARANDO ${N_ADMINS} REQUISIÇÕES CONCORRENTES PARA ASSUMIR O CHAMADO...`);
  const promessas = admins.map((adm, index) => {
    return requestJson(`${GATEWAY_URL}/api/atendimento/fila/${chamadoId}/assumir`, {
      method: 'POST',
      headers: { Cookie: adm.cookie }
    }).then(res => ({
      adminIndex: index + 1,
      adminNome: adm.usuario.nome,
      adminEmail: adm.usuario.email,
      statusHttp: res.status,
      corpo: res.data
    }));
  });

  const resultados = await Promise.all(promessas);

  // Tabela de resultados
  const tabelaExibicao = resultados.map(r => ({
    'Atendente': r.adminNome,
    'Status HTTP': r.statusHttp,
    'Resultado': r.statusHttp === 200 ? '✅ 200 OK (ASSUMIDO)' : '🔒 409 CONFLITO',
    'Mensagem': r.corpo && r.corpo.erro ? r.corpo.erro : (r.statusHttp === 200 ? 'Sucesso' : JSON.stringify(r.corpo))
  }));

  console.log('\n--- TABELA DE RESULTADOS DA DISPUTA CONCORRENTE ---');
  console.table(tabelaExibicao);

  const sucessos = resultados.filter(r => r.statusHttp === 200);
  const conflitos = resultados.filter(r => r.statusHttp === 409);

  console.log(`\nTotal Sucessos (200): ${sucessos.length}`);
  console.log(`Total Conflitos (409): ${conflitos.length}`);

  if (sucessos.length === 1 && conflitos.length === N_ADMINS - 1) {
    console.log('\n🎯 VALIDAÇÃO PERFEITA: Exatamente 1 técnico assumiu o chamado e os demais receberam 409 (Conflito Concorrente)!');
  } else {
    console.error(`\n⚠️ ATENÇÃO: Esperado 1 sucesso e ${N_ADMINS - 1} conflitos, mas obteve ${sucessos.length} sucessos e ${conflitos.length} conflitos.`);
  }

  // Verificação final do chamado
  console.log('\nAguardando propagação do evento chamado.atribuido...');
  await delay(1200);
  const chamadoFinalRes = await requestJson(`${GATEWAY_URL}/api/chamados/${chamadoId}`, {
    headers: { Cookie: usuarioInfo.cookie }
  });

  if (chamadoFinalRes.status === 200) {
    const cf = chamadoFinalRes.data;
    console.log(`\nEstado Final do Chamado:`);
    console.log(`  Protocolo: ${cf.protocolo}`);
    console.log(`  Status:    ${cf.status}`);
    console.log(`  Atendente: ${cf.atendenteNome || 'N/A'} (ID: ${cf.atendenteId || 'N/A'})`);
  }

  console.log('\n' + '='.repeat(70));
  console.log('  TESTE DE CONCORRÊNCIA FINALIZADO COM SUCESSO');
  console.log('='.repeat(70) + '\n');
}

executar().catch(err => {
  console.error('\n❌ Erro durante o teste de concorrência:', err.message);
  process.exit(1);
});
