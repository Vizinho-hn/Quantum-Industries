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
0. Leia este `AGENTS.md` antes de cada tarefa de alteração. Ao confirmar um aprendizado novo e reutilizável sobre o projeto, registre aqui uma orientação curta ou um link para a documentação específica. Preserve o conteúdo dos colaboradores e evite hipóteses, logs de sessão e instruções duplicadas.
1. Leia somente os arquivos relevantes à tarefa; comece por `README.md`, `CONTRIBUTING.md`, `ROADMAP.md` e a documentação específica quando necessário. Não varra o repositório inteiro sem motivo.
2. Verifique o estado do Git (`git status --short`) e preserve alterações de outras pessoas. Não sobrescreva trabalho alheio.
3. Identifique a implementação real antes de criar classes, pacotes ou abstrações. `docs/ARCHITECTURE.md` descreve também módulos **conceituais**, não necessariamente existentes.
4. Para APIs NeoForge/Minecraft, confira assinaturas na versão instalada, nas dependências ou na documentação correspondente; não invente métodos nem use exemplos de versões incompatíveis.
5. Para trabalho compartilhado, confirme a tarefa com a equipe e use uma branch `feat/*`, `fix/*` ou `docs/*`. Abra PR; não faça push direto na `main` sem autorização explícita.

## Regras de implementação
- Serraria Elétrica: reutiliza `MechanicalProcesses` para espécies/variantes, com rendimento elétrico de 8 tábuas. Menu expõe o `SimpleContainer` real (0–3 entrada, 4–7 saída). Preserve progresso/receita/slot reservado durante NBT load; callbacks de alteração do inventário não devem invalidar a reserva enquanto o próprio save está sendo restaurado. O limite de recebimento FE é acumulado por tick, não por chamada.

- Serraria: automação usa o UUID persistido `automation_operator` e valida a pesquisa por jogador em `canInsertAutomated`; extraia apenas `output`. Máquinas antigas sem vínculo exigem interação autorizada. O HUD usa os estoques e estados do update tag do servidor.
- A autorização persistente dos processadores e a reivindicação de trabalho cinético por tick são descritas em `docs/ERA2_STABILIZATION.md`; preservar recuperação de itens legados, reautorização e registros científicos individuais.
- Cabos de cobre são condutores sem buffer FE: `energy()` legado representa fluxo. Não some esse valor como reserva; os snapshots de cabos separam reserva/capacidade dos equipamentos conectados (`load`/`capacity`) de fluxo/limite local (`localCapacity`/`localLoad`).
- Inspeção hidráulica: `hydraulicContributions()` reutiliza as amostras em cache das rodas, em unidades locais de acionamento QKS após normalização/saturação. Use o protocolo detalhado sob demanda; preserve o codec básico de `InspectionSnapshot`. Veja `docs/HYDRAULIC_INSPECTION.md`.
- Transporte para fundidoras: use `canInsertIngredient` para consultar capacidade sem mutação; nunca simule inserção alterando inventários. A busca dos tubos é limitada e respeita lados e chunks carregados. A devolução de excedentes só usa um inventário na origem de uma conexão Puxar.
- Interfaces de processamento: `MachineProcessingMenu` compartilha os slots reais das Block Entities da Fundidora Elétrica e do Britador. Fechar uma tela deve manter entradas, saída e progresso na máquina. Em MC 26.1.2, o desenho dessas telas usa `extractContents(GuiGraphicsExtractor, int, int, float)`; confira o padrão em `MachineProcessingScreen` antes de usar assinaturas de versões antigas.
- Conexões: `ConnectionMode.AUTO` é o identificador persistido do modo exibido como **Entrada e saída**. Ele permite tráfego nos dois sentidos e participa do ciclo da chave. Preserve o valor salvo e as permissões ao alterar nomes ou incluir novos modos.
- Faça a **menor alteração correta** para atender à solicitação. Evite refatorações oportunistas, dependências novas e mudanças de versão.
- Preserve a separação cliente/servidor: o **servidor é autoridade** sobre inventários, custos, descobertas, pesquisas, permissões e desbloqueios.
- Mantenha descobertas e pesquisas persistentes **por jogador**. Compartilhar máquina não deve misturar progresso individual.
- Nunca confie em pacotes ou ações do cliente como prova de conclusão de pesquisa. Valide requisitos e operações no servidor.
- Preserve invariantes da Bancada de Ensaio: quatro métodos por material (inspeção, aquecimento, reação e prensa mecânica), totalizando 52 descobertas nos treze materiais, reserva/consumo de amostra, persistência ao fechar/reabrir, ferramentas reutilizáveis e devolução de itens ao quebrar, salvo mudança de design aprovada.
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

## Quantum Kinetic System — PRs 1–3 (implementação)
- Para qualquer alteração de física de rotação, eixos, rodas-d'água, engrenagens ou renderer cinético, **consulte `docs/KINETIC_ENGINE_ARCHITECTURE.md`**.
- Para geometria de portas, altura dos eixos e encaixes de transmissão em qualquer era, siga `docs/MECHANICAL_CONNECTION_STANDARD.md`; a topologia QKS por si só não confirma alinhamento físico.
- O PR 1 integra `KineticDynamics` à rede mecânica via `KineticNetworkManager`, com dinâmica por componente, snapshots e sincronização de chunks. O PR 2 acrescenta engrenagens, volante, polias/correias e rodas 3×3/5×5; o PR 3 entrega telemetria dos óculos, capítulos opcionais de física no Códice e `tools/verify-qks.sh`. A aceitação dedicada do PR 3 passou com `bash tools/verify-qks.sh`: fases write/read, persistência após reinício e isolamento de classes nas duas JVMs. O preview cliente verificou o HUD normal, os diagnósticos com Shift e a paginação do Códice.
- O renderer mecânico nativo é `client/kinetic/QksBlockEntityRenderer`; `ClientKineticTracker.init()` deve instalar o sink antes da entrada no mundo. O diretório legado dos assets não implica dependência da API GeckoLib. O PR 1 foi verificado em preview do cliente e suítes de servidor, incluindo serraria/dínamo, sem API GeckoLib no subsistema mecânico. Preserve essas regressões em mudanças futuras; veja `docs/adr/0001-qks-native-renderer.md`.
- A suíte QKS é opt-in (`tests/qks.init.gradle`, `tests/qks-runtime/`), usa dois servidores reais sobre um mundo isolado e audita classes carregadas. Preserve os 14 campos originais de `InspectionSnapshot` antes dos 11 campos QKS; valide o codec com `RegistryFriendlyByteBuf` e registries reais.
- Não importar RPM/torque físico em SI como se as unidades UM já fossem N·m, não duplicar energia, não bloquear pesquisas por conteúdo opcional do livro, e não trocar modelos existentes por placeholders.

## Direção artística e produção de assets (obrigatório)
- **Leia `docs/ART_DIRECTION.md` antes de criar ou alterar itens, blocos, modelos 3D, texturas ou animações.** Para assets das Eras I e II, siga também `docs/ERA1_VISUAL_STYLE_GUIDE.md` ou `docs/ERA2_VISUAL_STYLE_GUIDE.md`, respectivamente. `docs/MOD_REFERENCES.md` registra referências externas verificadas, mas não substitui as regras do projeto.
- **Confirme a era real** no `ROADMAP.md`, no catálogo de pesquisas/desbloqueios e na documentação existente; não classifique pelo nome, aparência ou tipo de energia. As cinco eras são as definidas no roadmap. **`wire_drawing_bench` pertence à Era II** conforme o guia atual, embora tenha acionamento manual/FE. Não invente paletas oficiais para as Eras III–V.
- Identidade visual: **Minecraft industrial detalhado**, com acabamento de alto nível, silhueta legível, pixel art nítida e detalhes mecânicos com propósito. Não simplifique por reflexo nem confunda complexidade com ruído, gradientes pintados, brilho futurista ou estética desconectada do Minecraft.
- Preserve os materiais, rampas de cor, atlas e proporções estabelecidos nos guias de cada era. **A grade de 16 unidades por bloco não exige textura 16×16**: respeite os sprites 16×16/32×32 e atlas 128×128 já utilizados. Verifique UVs e todos os consumidores de atlas compartilhados antes de modificá-los.
- Antes de modelar, localize o asset existente, sua fonte `.bbmodel`, modelo JSON/`.geo.json`, textura, renderizador, blockstate, modelos de item, animações, bones, pivôs, orientação, colisões e conexões relevantes. Prefira **JSON vanilla para geometria estática** e **GeckoLib quando houver necessidade real de animação**, respeitando a implementação existente.
- Imagens conceituais geradas ou fornecidas servem como **referência artística**, não como geometria exata. Adapte proporções à grade voxel, faça vistas úteis e preserve encaixes, mecânica e leitura em escala de jogo. Não copie assets ou código de Create, Oritech, AE2, Mekanism ou outros terceiros.
- Pipeline: **inspeção → diagnóstico visual → proposta → protótipo único no Blockbench/MCP → integração → QA in-game → aprovação**. Reaproveite o harness e o `minecraft-live` descritos abaixo. Não confunda screenshot do Blockbench com validação no Minecraft; registre antes/depois quando possível e declare o que realmente foi testado.
- **Sem padronização em lote sem aprovação do piloto.** Preserve IDs, receitas, progressão, registros, animações e contratos de teste. Para cada mudança visual, relate arquivos alterados, decisões de design, verificações executadas e pendências.

## Fluxo reutilizável de modelagem e refinamento — Blockbench e Blender

Estas orientações consolidam os guias visuais e as correções disponíveis nas conversas do projeto. Uma proposta ainda em avaliação não constitui um novo padrão aprovado. Consulte o histórico acessível quando existir; não afirme lembrar conversas indisponíveis.

### 1. Interpretar a referência e definir o aceite
- Antes de modelar, anote três grupos: **obrigatório** (pedido explícito e características que identificam a referência aprovada), **adaptável** (perspectiva, iluminação, fundo e detalhes decorativos) e **contrato funcional** (portas, movimento, colisão e regras existentes). Não transforme decoração em funcionalidade por conta própria.
- Compare silhueta, proporção entre caixa e peças, aberturas, espessura dos reforços, projeção dos eixos e visibilidade do mecanismo. Repetir apenas os mesmos materiais não reproduz a composição aprovada. Se o usuário repetir a referência após uma entrega, identifique as diferenças concretas e corrija-as antes de propor outra interpretação.
- Uma referência conceitual pode ter perspectiva ou encaixes impossíveis. Explique a incompatibilidade e a adaptação mínima; peça esclarecimento quando a solução mudar uma característica obrigatória ou o comportamento. Não use essa ressalva para simplificar arbitrariamente a aparência pedida.

### 2. Confirmar medidas e contratos antes do detalhe
- Localize fonte editável, exports, atlas, variantes, renderer e animações reais. Registre dimensões em unidades do projeto (Minecraft: 16 unidades por bloco), limites permitidos, centros das portas, orientações, peças fixas/móveis, hierarquia, pivôs e eixos locais/de mundo.
- Faça primeiro um volume simples com os principais encaixes. Confira vistas frontal, lateral e superior; verifique o pivô com uma rotação de teste. Diferencie eixo de conexão, eixo excluído e orientação visual; não presuma que um campo chamado AXIS representa o eixo de rotação.
- Confirme que peças móveis cabem no percurso inteiro. Se houver saliências além do bloco, confira seleção, colisão, blocos vizinhos e conexões antes de aceitá-las. Não altere física, IDs, receitas ou progressão para acomodar uma mudança apenas visual.

### 3. Construir um piloto fiel ao estilo
- Siga a era confirmada e as paletas dos guias existentes. Prefira pixel art nítida, rampas controladas e detalhes com propósito. Reduza ruído gratuito e texturas fragmentadas; preserve volume, contraste entre materiais e complexidade útil.
- Priorize a silhueta e os elementos obrigatórios, depois mecanismos, materiais e acabamento. Não acrescente luzes, peças, materiais ou ornamentos sem justificativa no pedido ou no padrão aprovado.
- Confira se o próprio modelo mantém aberturas, partes vazadas e mecanismos legíveis. Tampas, mancais, cintas ou outras peças não devem ocultar justamente o detalhe aprovado. Corrija a geometria quando necessário, em vez de depender de um único ângulo favorável.

### 4. Validar nos editores e preservar fontes
- **Blockbench:** abra/reabra o .bbmodel, confirme formato de exportação e textura vinculada/embutida, grupos/bones, pivôs, UVs e escala. Inspecione todas as faces, cubos com dimensão nula, faces sobrepostas, peças flutuantes e limites do atlas. Execute check_model quando disponível e registre o resultado; corrija os problemas antes da aprovação técnica exigida pelo guia.
- **Blender, quando aplicável:** confirme unidades, orientação dos eixos, origem dos objetos, parentesco e escala de importação/exportação. Inspecione normais, faces duplicadas, UVs e materiais. Aplique transformações apenas quando compatíveis com o rig e o exportador; não destrua pivôs ou animações já funcionais. Teste um export pequeno e sua reimportação antes de detalhar ou converter todo o asset.
- Mantenha .bbmodel e/ou .blend editáveis, texturas e exports coerentes com a versão integrada. Materiais procedurais, modificadores e iluminação do Blender só podem ser usados no produto final se o formato/renderer os suportar ou se forem convertidos corretamente. Verifique o resultado exportado, não apenas o viewport.

### 5. Refinar por etapas verificáveis
- Trabalhe em um piloto por vez: **volumes/encaixes → mecanismo → materiais → animação → integração**. Confira ou mostre cada marco antes de expandir. Preserve o que já foi aprovado; uma correção localizada não autoriza redesenhar o conjunto ou aplicar o estilo a outros modelos.
- Ao receber uma correção, liste as diferenças observadas, mude a menor parte necessária e compare antes/depois em escala e ângulo equivalentes. Quando a causa de um erro se repete, diagnostique formato, orientação ou exportação antes de repetir builds, reinícios e ajustes de câmera.
- Prefira trabalho em segundo plano quando possível. Abra o aplicativo ou o jogo quando solicitado, usando um cenário isolado; preserve saves e trabalho aberto do usuário. Não faça merge ou publicação como consequência automática de uma aprovação visual local.

### 6. Conferir movimento e integração em contexto
- Teste início, execução, parada, retomada e limites do movimento: sentido, velocidade, hierarquia, colisões entre peças, encaixes e continuidade do loop. Partes que dependem de atividade precisam acompanhar o estado real; confirme a semântica de parada/inércia do sistema existente.
- No Minecraft, confira bloco colocado, item na mão/inventário, orientações aplicáveis e conexão com equipamentos vizinhos. Compare com um bloco vanilla para julgar escala; confirme seleção/colisão e encaixes durante o movimento, não apenas em repouso.
- Use captura próxima em três quartos, vistas auxiliares e uma vista na distância normal de jogo. Garanta luz suficiente e contraste; remova sobreposições da captura sem esconder defeitos. Use a mesma pose/estado na comparação antes/depois. Se houver animação, registre sequência ou vídeo além da imagem parada.
- Reutilize o renderer e os harnesses existentes. Escolha build, testes de recursos e runtime proporcionais ao que mudou; ampliar validação quando alterações afetarem renderização compartilhada, conexões ou comportamento. Não confunda compilar com validar aparência ou funcionamento.

### 7. Entregar evidências e manter a memória confiável
- Informe arquivos/fonte editável, mudanças observáveis, verificações executadas, evidências e pendências. Diferencie claramente: arquivo gerado, aberto no editor, exportado, compilado e observado dentro do jogo. Uma imagem conceitual ou render do Blender não comprova integração no Minecraft.
- Se Blockbench, Blender, MCP ou um teste não estiver disponível, declare a limitação e o que foi verificado por alternativa; não registre check_model sem executá-lo nem apresente pendência como aprovação. Não afirme fidelidade exata com base apenas na intenção.
- Após aprovação ou correção confirmada, registre aqui somente o aprendizado geral reutilizável, sem transformar dimensões, paletas ou soluções de um único modelo em regra universal. Preserve os guias e as instruções úteis já existentes.

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

- Ao integrar mudanças em `ModBlocks`/`ModItems`, preserve os registros industriais e suas entidades, incluindo o interruptor no conjunto de blocos válidos de `COPPER_CABLE_ENTITY`, bem como os registros de conversão industrial, esteira, caixa de engrenagens e chave de circuito, com a chave declarada antes do tipo de Block Entity dos cabos que a inclui em seus blocos válidos. A suíte de contratos verifica essas declarações para prevenir regressões de merge.

- A gearbox usa AXIS como eixo excluído das conexões. Os três modelos visuais mantêm a caixa na vertical e adaptam apenas os mecanismos aos dois eixos permitidos; a textura dedicada é compact_gearbox.png.

- Transmissões com dois eixos podem usar uma única fase sincronizada do QKS. Aplique o sinal de cada porta ao grupo giratório em seu eixo de mundo. Separe o modelo estático da geometria móvel para evitar duplicação ou cintilação.
