# AGENTS.md — Quantum Industries

Instruções para agentes de programação (incluindo Codex). Este arquivo complementa `CONTRIBUTING.md`; em caso de conflito, preserve as regras de contribuição e peça esclarecimento.

## Contexto confirmado
- Mod **Quantum Industries**: ciência, investigação, pesquisa individual e progressão industrial.
- Minecraft **26.1.2**, NeoForge **26.1.2.125**, **Java 25**, Gradle Wrapper **9.2.1**.
- Mod ID: `quantumindustries`; grupo: `br.com.quantumindustries`.
- Fontes Java principais em `src/main/java`, fontes específicas de cliente também em `src/client/java` (configuradas em `build.gradle`).
- Recursos em `src/main/resources` e `src/generated/resources`.
- Consulte `README.md` e `ROADMAP.md` para o estado atual do jogo e `docs/design/bancada-de-ensaio/README.md` para as regras da Bancada de Ensaio Empírico.

## Architecture navigation (Graphify)
- When the Graphify CLI is available, query the shared map with `graphify query "<specific question>" --graph graphify-out/graph.json`; also consult `graphify-out/GRAPH_REPORT.md`.
- If the CLI is unavailable, inspect the report and graph JSON as navigation aids, then verify the relevant source files.
- The map covers `src`; paths in query results are relative to `src/`. The 126 image assets were excluded from semantic extraction.
- Treat graph links, clusters, and hubs as navigation clues, not authority. Verify relevant conclusions in source; the report records dangling edges, self-loops, collapsed relationships, and incomplete semantic token accounting.

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
- Quando uma mudança alterar o comportamento do mod ou o estado do projeto, atualize as seções pertinentes do README.md e/ou ROADMAP.md no mesmo PR. Não altere documentos que não foram afetados.

## Referências técnicas entre mods — obrigatório para qualquer subsistema
- **Antes de implementar ou refatorar cabos, tubos, energia, fluidos, itens, máquinas, mecânica, redes, armazenamento, autocrafting, pesquisas, GUIs, renderização, performance ou multiplayer**, consulte a área correspondente de **`docs/ENGINEERING_REFERENCE_ATLAS.md`**. Use `docs/MOD_REFERENCES.md` como índice das fontes Create, Oritech, AE2 e Mekanism.
- O atlas separa **fatos verificados nos códigos externos**, **implementação já existente no Quantum Industries**, **propostas de melhoria** e **testes pendentes**. Não trate sugestões como bugs confirmados ou como requisitos obrigatórios; não reproduza arquitetura, APIs, assets ou licenças de terceiros sem justificativa.
- Para conduítes, mantenha coerentes **topologia, permissões por face (`AUTO/PULL/PUSH/NONE`), transferência real e aparência**. Teste as seis direções, bifurcações, gargalos, múltiplas fontes/destinos, ciclos, save/reload e bordas de chunks.
- Para energia, fluidos e itens, preserve **conservação de recursos, limites por trecho/tick, atomicidade quando aplicável, inserção parcial e ausência de duplicação**. Respeite o contrato atual de cabos sem armazenamento, as transações de fluido e os pacotes físicos dos tubos pneumáticos.
- Para mecânica, não substituir o modelo de **UM** pelo sistema de RPM/stress do Create sem decisão de gameplay. Para redes digitais/autocrafting da Era V, documente o design e valide um protótipo antes de criar frameworks genéricos.
- Verifique assinaturas nas dependências reais do projeto (**Minecraft 26.1.2 / NeoForge 26.1.2.125**); branches externos podem ser de outras versões. Execute testes de contrato e runtime pertinentes, e registre o que foi de fato validado.

## Direção artística e produção de assets (obrigatório)
- **Leia `docs/ART_DIRECTION.md` antes de criar ou alterar itens, blocos, modelos 3D, texturas ou animações.** Para assets das Eras I e II, siga também `docs/ERA1_VISUAL_STYLE_GUIDE.md` ou `docs/ERA2_VISUAL_STYLE_GUIDE.md`, respectivamente. `docs/MOD_REFERENCES.md` registra referências externas verificadas, mas não substitui as regras do projeto.
- **Confirme a era real** no `ROADMAP.md`, no catálogo de pesquisas/desbloqueios e na documentação existente; não classifique pelo nome, aparência ou tipo de energia. As cinco eras são as definidas no roadmap. **`wire_drawing_bench` pertence à Era II** conforme o guia atual, embora tenha acionamento manual/FE. Não invente paletas oficiais para as Eras III–V.
- Identidade visual: **Minecraft industrial detalhado**, com acabamento de alto nível, silhueta legível, pixel art nítida e detalhes mecânicos com propósito. Não simplifique por reflexo nem confunda complexidade com ruído, gradientes pintados, brilho futurista ou estética desconectada do Minecraft.
- Preserve os materiais, rampas de cor, atlas e proporções estabelecidos nos guias de cada era. **A grade de 16 unidades por bloco não exige textura 16×16**: respeite os sprites 16×16/32×32 e atlas 128×128 já utilizados. Verifique UVs e todos os consumidores de atlas compartilhados antes de modificá-los.
- Antes de modelar, localize o asset existente, sua fonte `.bbmodel`, modelo JSON/`.geo.json`, textura, renderizador, blockstate, modelos de item, animações, bones, pivôs, orientação, colisões e conexões relevantes. Prefira **JSON vanilla para geometria estática** e **GeckoLib quando houver necessidade real de animação**, respeitando a implementação existente.
- Imagens conceituais geradas ou fornecidas servem como **referência artística**, não como geometria exata. Adapte proporções à grade voxel, faça vistas úteis e preserve encaixes, mecânica e leitura em escala de jogo. Não copie assets ou código de Create, Oritech, AE2, Mekanism ou outros terceiros.
- Pipeline: **inspeção → diagnóstico visual → proposta → protótipo único no Blockbench/MCP → integração → QA in-game → aprovação**. Reaproveite o harness e o `minecraft-live` descritos abaixo. Não confunda screenshot do Blockbench com validação no Minecraft; registre antes/depois quando possível e declare o que realmente foi testado.
- **Sem padronização em lote sem aprovação do piloto.** Preserve IDs, receitas, progressão, registros, animações e contratos de teste. Para cada mudança visual, relate arquivos alterados, decisões de design, verificações executadas e pendências.

## Verificação proporcional ao risco
- Verificações da CI: `python3 -m unittest discover -s tests -p 'test_*.py' -v`, `bash ./gradlew --no-daemon verifyLegacyResearch`, `bash tools/verify-mechanical.sh` e `bash ./gradlew --no-daemon build` (requer JDK 25).
- Integração de bancada em servidor isolado: `./tools/verify-assay.sh`.
- Preview visual da bancada (Linux, Xvfb e cache configurados): `./tools/preview-assay.sh`.
- Execução manual: `./gradlew runClient`; servidor: `./gradlew runServer`.
- Comece com o teste mais específico. Execute verificações amplas quando o impacto justificar; testes de cliente/preview são mais caros e podem abrir processos gráficos.
- Nunca afirme que compilou, testou ou verificou visualmente se não executou o procedimento. Relate impedimentos de ambiente.

### Inspeção visual e QA in-game autônomo (MCP + Harness sem interação humana)
1. **`minecraft-live` MCP sem clique manual ("MCP Take Over"):**
   - `build.gradle` (`runs.client`) e `Jogar.sh` já passam `-Dmcp.remote_control=true` e `-Dmcp.background_input=true` (`MC_MCP_REMOTE_CONTROL=true`, `MC_MCP_BACKGROUND_INPUT=true`). Isso libera `enter_control_mode`, `screenshot`, `get_screen_buttons`, `enumerate_widgets`, `click_button_index`, `click` e `set_view_angle` na porta `19132` sem o usuário precisar clicar em *MCP Take Over* no menu de pausa.
   - **Limitações do bridge `minecraft-mcp-26.1.2-neoforge-v0.4.28.jar` no MC 26.1.2:** `execute_command`, `open_chat` e `press_key` falham por reflection (`performCommand` vs `performPrefixedCommand`), e `get_player_info` quebra o JSON em locale `pt_BR` por vírgula decimal (`String.format("%.1f")`).
2. **Harness autônomo de QA in-game (`tests/qa-preview.init.gradle`):**
   - Sobe o cliente NeoForge, entra direto no mundo isolado `build/qa-preview/saves/MachinesQA` (`--quickPlaySingleplayer MachinesQA`), instancia e testa as máquinas no servidor, move a câmera por estágios, captura screenshots nativas e fecha o cliente sozinho:
     ```bash
     ./gradlew --no-daemon --max-workers=2 --init-script tests/qa-preview.init.gradle runClient --console=plain > /tmp/quantum-qa-preview.log 2>&1
     ```
   - Fontes de QA ficam em `tests/qa-preview/br/com/quantumindustries/qa/MachinesQaSetup.java` e `MachinesQaClient.java` (fora do JAR de produção).
   - Saídas para inspeção direta pelo agente: `build/qa-preview/qa-report.txt`, screenshots em `build/qa-preview/screenshots/` (`01-machines-overview.png` a `06-mechanical-era1-closeup.png`) e log completo em `/tmp/quantum-qa-preview.log`.
3. **Blockbench MCP autônomo (`http://127.0.0.1:8787`):**
   - Se o Blockbench estiver fechado, inicie em background com `DISPLAY=:0 /mnt/dados/apps/blockbench/squashfs-root/AppRun` e aguarde ~4s para usar `get_status`, `new_project`, `import_texture`, `add_cubes`, `check_model`, `screenshot_views`, `save_project` e `export_model`.

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
