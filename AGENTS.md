# AGENTS.md — Quantum Industries

Instruções para agentes de programação (incluindo Codex). Este arquivo complementa `CONTRIBUTING.md`; em caso de conflito, preserve as regras de contribuição e peça esclarecimento.

## Contexto confirmado
- Mod **Quantum Industries**: ciência, investigação, pesquisa individual e progressão industrial.
- Minecraft **26.1.2**, NeoForge **26.1.2.125**, **Java 25**, Gradle Wrapper **9.2.1**.
- Mod ID: `quantumindustries`; grupo: `br.com.quantumindustries`.
- Fontes Java principais em `src/main/java`, fontes específicas de cliente também em `src/client/java` (configuradas em `build.gradle`).
- Recursos em `src/main/resources` e `src/generated/resources`.
- Consulte `README.md` e `ROADMAP.md` para o estado atual do jogo e `docs/design/bancada-de-ensaio/README.md` para as regras da Bancada de Ensaio Empírico.

## Antes de alterar código
1. Leia somente os arquivos relevantes à tarefa; comece por `README.md`, `CONTRIBUTING.md`, `ROADMAP.md` e a documentação específica quando necessário. Não varra o repositório inteiro sem motivo.
2. Verifique o estado do Git (`git status --short`) e preserve alterações de outras pessoas. Não sobrescreva trabalho alheio.
3. Identifique a implementação real antes de criar classes, pacotes ou abstrações. `docs/ARCHITECTURE.md` descreve também módulos **conceituais**, não necessariamente existentes.
4. Para APIs NeoForge/Minecraft, confira assinaturas na versão instalada, nas dependências ou na documentação correspondente; não invente métodos nem use exemplos de versões incompatíveis.
5. Para trabalho compartilhado, confirme a tarefa com a equipe e use uma branch `feat/*`, `fix/*` ou `docs/*`. Abra PR; não faça push direto na `main` sem autorização explícita.

## Regras de implementação
- Faça a **menor alteração correta** para atender à solicitação. Evite refatorações oportunistas, dependências novas e mudanças de versão.
- Preserve a separação cliente/servidor: o **servidor é autoridade** sobre inventários, custos, descobertas, pesquisas, permissões e desbloqueios.
- Mantenha descobertas e pesquisas persistentes **por jogador**. Compartilhar máquina não deve misturar progresso individual.
- Nunca confie em pacotes ou ações do cliente como prova de conclusão de pesquisa. Valide requisitos e operações no servidor.
- Preserve invariantes da Bancada de Ensaio: quatro métodos por material (inspeção, aquecimento, reação e prensa mecânica), totalizando 40 descobertas nos dez materiais, reserva/consumo de amostra, persistência ao fechar/reabrir, ferramentas reutilizáveis e devolução de itens ao quebrar, salvo mudança de design aprovada.
- Novos requisitos científicos afetam pesquisas futuras. Preserve estudos já concluídos e use WorldResearchData.canLearn/learn em todos os caminhos normais.
- Nomes de classes e métodos em inglês, seguindo convenções Java. Evite comentários redundantes e abstrações prematuras.
- Ao mudar itens, blocos ou interfaces, confira registros, recursos, modelos, traduções PT-BR/EN-US e receitas pertinentes.
- Não inclua credenciais, arquivos locais, caches Gradle, mundos de teste ou saídas de build no commit.
- Não altere licença nem redistribua assets de terceiros sem autorização.
- A cada implementação ou atualização do mod, revise o README.md e o ROADMAP.md no mesmo PR. Registre o que foi entregue e mantenha o trabalho pendente identificado.

## Verificação proporcional ao risco
- Testes de contratos e recursos: `python3 -m unittest discover -s tests -v`.
- Compilação: `./gradlew build` (requer JDK 25).
- Integração de bancada em servidor isolado: `./tools/verify-assay.sh`.
- Preview visual da bancada (Linux, Xvfb e cache configurados): `./tools/preview-assay.sh`.
- Execução manual: `./gradlew runClient`; servidor: `./gradlew runServer`.
- Comece com o teste mais específico. Execute verificações amplas quando o impacto justificar; testes de cliente/preview são mais caros e podem abrir processos gráficos.
- Nunca afirme que compilou, testou ou verificou visualmente se não executou o procedimento. Relate impedimentos de ambiente.

## Economia de contexto e franquia do Codex
- Primeiro responda internamente: quais arquivos e invariantes a tarefa afeta? Abra apenas esses caminhos.
- Prefira buscas direcionadas (`rg` com caminho ou padrão específico) a dumps completos de código, logs e diretórios.
- Para logs extensos, capture o erro e algumas linhas de contexto, não milhares de linhas.
- Não repita `./gradlew build` nem previews completos sem alteração relevante ou hipótese nova.
- Divida mudanças grandes em etapas verificáveis. Peça esclarecimento se a solicitação mudar regras de gameplay, escopo ou arquitetura.
- Não gere documentação longa automaticamente; atualize apenas o documento pertinente.
- Se uma tentativa falhar repetidamente, pare, resuma a causa provável e proponha próximo diagnóstico em vez de repetir comandos.

## Formato da entrega ao colaborador
Responda de forma objetiva:
1. **O que mudou** e por quê.
2. **Arquivos alterados**.
3. **Testes executados**, resultados e limitações.
4. **Riscos/pendências** e eventual necessidade de revisão de design.

Não faça commits, push, merge, force-push nem mudanças destrutivas sem autorização explícita na tarefa. Sempre revise o diff antes de propor integração.
