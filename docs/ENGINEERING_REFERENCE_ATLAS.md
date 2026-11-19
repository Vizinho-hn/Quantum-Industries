# Quantum Industries — Atlas de engenharia: aprendizados de outros mods

**Escopo:** referência técnica transversal para **todos os sistemas do mod**, não apenas assets. Complementa [MOD_REFERENCES.md](MOD_REFERENCES.md), [ART_DIRECTION.md](ART_DIRECTION.md), [ARCHITECTURE.md](ARCHITECTURE.md), [ROADMAP_QUANTUM.md](ROADMAP_QUANTUM.md), [ERA1_VISUAL_STYLE_GUIDE.md](ERA1_VISUAL_STYLE_GUIDE.md) e [ERA2_VISUAL_STYLE_GUIDE.md](ERA2_VISUAL_STYLE_GUIDE.md). **Não altera a implementação nem transforma inspiração em requisito automaticamente.**

**Método (19/11/2026):** inspeção direta dos arquivos Java, assets, READMEs e estruturas dos repositórios oficiais: [Create](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev) (`mc1.21.1/dev`), [Oritech](https://github.com/Rearth/Oritech/tree/26.1) (`26.1`), [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/main) (`main`) e [Mekanism](https://github.com/mekanism/Mekanism/tree/26.3) (`26.3`). Os links de arquivos abaixo apontam para essas branches, **não** para versões necessariamente compatíveis com o Quantum Industries. Fatos verificados, recomendações e hipóteses de teste são rotulados separadamente. **Não foi feito benchmark, teste de compatibilidade ou execução in-game nesta pesquisa.**

## 1. Mapa completo de disciplinas e fontes

| Área do Quantum Industries | O que estudar e de quem | Evidência direta | Aplicação / decisão para QI |
|---|---|---|---|
| **Topologia de cabos e tubos** | Mekanism: `Transmitter` e `DynamicNetwork`; AE2: `CablePart`; Oritech: `GenericPipeInterfaceEntity` | [Mekanism/transmitter](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/Transmitter.java), [AE2/cable](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/parts/networking/CablePart.java), [Oritech/pipe graph](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/block/entity/pipes/GenericPipeInterfaceEntity.java) | Separar estado visual de conexões, permissões por face, topologia de rede e transferência de recursos |
| **Energia FE** | Mekanism: rede elétrica, cabos com buffer, distribuição; AE2: serviço de energia | [EnergyNetwork](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/EnergyNetwork.java), [UniversalCable](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/UniversalCable.java), [AE2 energy](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/main/src/main/java/appeng/me/energy) | Conservação, vazão por aresta, limites por máquina, distribuição justa, perda e atualização de rede |
| **Fluidos, pressão, válvulas e vidro** | Create: `FluidNetwork`, `FluidPropagator`, tubos/vidro/bomba; Mekanism: `MechanicalPipe` e `FluidNetwork` | [Create/FluidNetwork](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/fluids/FluidNetwork.java), [FluidPropagator](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/fluids/FluidPropagator.java), [Mekanism/MechanicalPipe](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/MechanicalPipe.java) | Definir modelo hidráulico explícito, transferências atômicas, compatibilidade de fluido, visibilidade de tubo de vidro |
| **Itens e roteamento** | Oritech: interfaces de tubo, cache de rotas, splitter; Mekanism: transportadores, pathfinding, filtros; AE2: import/export bus; Create: funis | [Oritech/ItemPipe](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/block/entity/pipes/ItemPipeInterfaceEntity.java), [SmartSplitter](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/block/entity/pipes/SmartSplitterBlockEntity.java), [Mekanism/Transporter](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/LogisticalTransporterBase.java), [AE2/import](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/parts/automation/ImportBusPart.java) | Pacotes físicos ou abstraídos, filtros, prioridade, round-robin, backpressure, rollback, save/reload |
| **Mecânica rotacional** | Create: `RotationPropagator`, `KineticNetwork`, eixos, engrenagens e embreagens | [RotationPropagator](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/RotationPropagator.java), [KineticNetwork](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/KineticNetwork.java), [transmission](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/transmission) | Capacidade/carga compartilhadas, sentido, conexão física e animação; não copiar toda a física de RPM/stress sem decisão de gameplay |
| **Armazenamento e redes digitais** | AE2: `Grid`, `StorageService`, `PathingService`, `NetworkStorage`; Mekanism: QIO | [AE2/grid](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/Grid.java), [StorageService](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/service/StorageService.java), [Mekanism/QIO](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/content/qio) | Separar estoque lógico, dispositivos, permissões e UI; reservar para fases em que haja necessidade real |
| **Autocrafting e requisições** | AE2: cálculo, plano, execução, exportação e solicitação de crafting | [CraftingCalculation](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/crafting/CraftingCalculation.java), [CraftingService](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/service/CraftingService.java), [ExportBus](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/parts/automation/ExportBusPart.java) | Para a Era V planejada: planejador de dependências, reserva de ingredientes, execução, cancelamento, recuperação e observabilidade |
| **Máquinas, processos e receitas** | Create: `SequencedAssemblyRecipe`; Oritech: máquinas de processamento; Mekanism: fábricas e tiers | [Create/sequenced](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe.java), [Oritech/processing](https://github.com/Rearth/Oritech/tree/26.1/src/main/java/rearth/oritech/block/entity/processing), [Mekanism/tiers](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/tier) | Processos multietapas, entrada/saída/energia explícitas, bloqueio por pesquisa e progressão sem redundância |
| **Multiblocos** | Mekanism: dados e validação de caldeira; AE2: calculador de multiblocos | [BoilerValidator](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/boiler/BoilerValidator.java), [BoilerMultiblockData](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/boiler/BoilerMultiblockData.java), [AE2/MBCalculator](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/cluster/MBCalculator.java) | Só criar infraestrutura de formação/validação quando um multibloco real entrar no escopo |
| **Interfaces, diagnóstico e tutorial** | Create: Ponder; AE2: status de canal nos smart cables; Oritech: UI de máquinas | [Create/ponder](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/resources/assets/create/ponder), [AE2/SmartCablePart](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/parts/networking/SmartCablePart.java), [Oritech/UI](https://github.com/Rearth/Oritech/tree/26.1/src/main/java/rearth/oritech/client/ui) | Indicar conexão, fluxo, capacidade, falha, progresso e próximo passo no jogo |
| **Visual, modelos e animação** | Oritech: Blockbench + GeckoLib; Create: renderização cinética; Mekanism: modelos e render | [Oritech/geckolib](https://github.com/Rearth/Oritech/tree/26.1/src/main/resources/assets/oritech/geckolib), [Create/kinetic render](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/base), [Mekanism/assets](https://github.com/mekanism/Mekanism/tree/26.3/src/main/resources/assets/mekanism) | Visual Minecraft industrial detalhado, animação ligada ao estado real, conexões e UVs consistentes |
| **Upgrades e balanceamento** | Mekanism: `CableTier`, `PipeTier`, `FactoryTier`, dados de upgrade | [Mekanism/tiers](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/tier), [upgrade data](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/upgrade) | Evoluir throughput/capacidade/eficiência com custo e leitura visual; não confundir tiers com as cinco eras |
| **Persistência e multiplayer** | Mekanism: save shares/transactions; Oritech: `SavedData` de redes; AE2: serviços e nós | [UniversalCable](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/UniversalCable.java), [Oritech/GenericPipeInterfaceEntity](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/block/entity/pipes/GenericPipeInterfaceEntity.java), [AE2/Grid](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/Grid.java) | Autoridade no servidor, compatibilidade de saves, invalidação de caches e sync mínimo |
| **Desempenho** | Oritech: `PipeTransferBackoff` e cache de rede; AE2: `TickManagerService`; Mekanism: cache de capabilities | [PipeTransferBackoff](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/block/entity/pipes/PipeTransferBackoff.java), [AE2/TickManagerService](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/me/service/TickManagerService.java), [Mekanism/transporter](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/LogisticalTransporterBase.java) | Recalcular topologia só quando necessário, limites de busca, *backoff* ocioso e medir custo antes de otimizar |
| **Integrações** | Oritech: compatibilidades declaradas; AE2: API e import/export; Mekanism: capacidades de recursos | [Oritech/README](https://github.com/Rearth/Oritech/blob/26.1/README.md), [AE2/README](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/README.md), [Mekanism/MechanicalPipe](https://github.com/mekanism/Mekanism/blob/26.3/src/main/java/mekanism/common/content/network/transmitter/MechanicalPipe.java) | Expor capacidades NeoForge adequadas, testar inventários e máquinas vanilla/modded, sem dependências desnecessárias |

**Regra de leitura:** presença de uma classe no repositório de referência é um **fato**; adotar seu desenho é uma **proposta**, não uma obrigação. Uma linha da tabela não comprova compatibilidade de API ou equivalência de desempenho.

## 2. Auditoria dos sistemas que o Quantum Industries já possui

Arquivos locais inspecionados na branch de documentação; as descrições abaixo são de **código observado**, não de teste funcional:

| Subsistema | Implementação local verificada | Contrato que já existe / oportunidade de revisão |
|---|---|---|
| **FE e cabos** | [`energy/CableNetwork.java`](../src/main/java/br/com/quantumindustries/energy/CableNetwork.java), [`energy/EnergyPorts.java`](../src/main/java/br/com/quantumindustries/energy/EnergyPorts.java), [`block/CopperCableBlock.java`](../src/main/java/br/com/quantumindustries/block/CopperCableBlock.java), [`block/entity/CopperCableBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/CopperCableBlockEntity.java) | Busca BFS **somente em chunks carregados**, limite `MAX_NODES=128`, limites por segmento **32 FE/t** no barramento nu e **64 FE/t** no isolado, perda de **1 FE a cada 4 trechos nus** por rota e prioridade rotativa. Cabos não são baterias. Auditar invariantes com múltiplas fontes/destinos, ciclos, falha parcial e gargalos |
| **Configuração por face** | [`logistics/ConfigurableConduit.java`](../src/main/java/br/com/quantumindustries/logistics/ConfigurableConduit.java), [`ConnectionModes.java`](../src/main/java/br/com/quantumindustries/logistics/ConnectionModes.java), [`ConnectionMode.java`](../src/main/java/br/com/quantumindustries/logistics/ConnectionMode.java) | Seis faces persistidas, modo automático para dados ausentes/malformados, modos de entrada/saída. Exigir que geometria, capacidade, extração e inserção respeitem **o mesmo contrato direcional** |
| **Fluidos** | [`fluid/FluidTransfer.java`](../src/main/java/br/com/quantumindustries/fluid/FluidTransfer.java), [`SidedPipeHandler.java`](../src/main/java/br/com/quantumindustries/fluid/SidedPipeHandler.java), [`block/entity/FluidPipeBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/FluidPipeBlockEntity.java) | Transação NeoForge com rollback se aceitação difere da extração; **250 mB por trecho, 50 mB/t**; queda/passivo e `pressureTicks` para subida; controle por face. Verificar loops, fluido misto, propagação de pressão, compatibilidade de máquina e tubos transparentes |
| **Tubos pneumáticos** | [`block/entity/PneumaticTubeBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/PneumaticTubeBlockEntity.java), [`logistics/ItemRouting.java`](../src/main/java/br/com/quantumindustries/logistics/ItemRouting.java) | **Um pacote por trecho**, **5 ticks por salto**, `packet` persistido, devolução ao quebrar, mutação do restante na inserção. Verificar bifurcações, bloqueios, destino cheio, chunk descarregado e multiplayer |
| **Separação** | [`block/entity/GravitationalSorterBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/GravitationalSorterBlockEntity.java) | Escolha de duas saídas baseada em classificação leve/densa e inversão; estudar filtros configuráveis como evolução **proposta**, sem substituir a mecânica atual sem aprovação |
| **Transmissão mecânica** | [`mechanical/MechanicalNetwork.java`](../src/main/java/br/com/quantumindustries/mechanical/MechanicalNetwork.java), [`MechanicalParticipant.java`](../src/main/java/br/com/quantumindustries/mechanical/MechanicalParticipant.java) | Capacidade/carga por componente, portas axiais, direção da fonte; **256 nós**, orçamento **8192 visitas/tick** e cache por tick. Não presumir que já há a física de RPM/engrenagens do Create |
| **Máquinas híbridas** | [`block/entity/ManualDynamoBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/ManualDynamoBlockEntity.java), [`WireDrawingBenchBlockEntity.java`](../src/main/java/br/com/quantumindustries/block/entity/WireDrawingBenchBlockEntity.java) | Dínamo manual/mecânico; trefiladora manual ou FE. Preservar custo, progresso, bloqueios de pesquisa e autorização do jogador; evitar execução duplicada no mesmo tick |
| **Pesquisa, receitas e UX** | [`research/ResearchCatalog.java`](../src/main/java/br/com/quantumindustries/research/ResearchCatalog.java), [`research/WorldResearchData.java`](../src/main/java/br/com/quantumindustries/research/WorldResearchData.java), [`recipe/FoundationRecipe.java`](../src/main/java/br/com/quantumindustries/recipe/FoundationRecipe.java), [`client/QuantumGuidebookScreen.java`](../src/main/java/br/com/quantumindustries/client/QuantumGuidebookScreen.java) | Progressão científica **por jogador**, receitas condicionadas e manual in-game. A Era V planeja pedidos/autocrafting, mas não está demonstrada por esses arquivos como funcional |
| **QA existente** | [`tests/test_cable_connections.py`](../tests/test_cable_connections.py), [`test_logistics_contracts.py`](../tests/test_logistics_contracts.py), [`test_era2_features.py`](../tests/test_era2_features.py), [`test_mechanical_resources.py`](../tests/test_mechanical_resources.py), [`test_foundation_contract.py`](../tests/test_foundation_contract.py) | Há testes de contrato/recursos e harnesses de runtime em `tests/`. **Inspecionar o que cada teste realmente cobre**; teste de presença de strings não prova conservação ou robustez em runtime |

### 2.1 Pontos de atenção para futuras auditorias — não são bugs confirmados

1. **Entrega de FE:** `CableNetwork.transfer` chama `target.receiveEnergy(offer)` e depois `extract.applyAsInt(accepted + loss)`. O código presume que a fonte sempre honre a extração contratada; se não, lança exceção. **Testar com fontes/consumidores diversos** e decidir se uma transação compartilhada seria mais robusta, sem alterar o modelo atual inadvertidamente.
2. **Limites de busca:** `MAX_NODES` e `MAX_VISITS_PER_TICK` protegem TPS, mas podem deixar redes grandes incompletas. Definir UX de “rede limitada” e teste de fronteira antes de aumentar limites.
3. **Representação de tubo de fluido:** `FluidPipeBlock.getShape` define uma caixa axial; verificar coerência entre colisão, conexões reais e modelo visual em curvas/T/cruzamentos, em vez de assumir que o visual já acompanha a topologia.
4. **Roteamento de pacotes:** o tubo escolhe a primeira saída disponível, sem retornar imediatamente pela entrada. Isso não equivale a pathfinding global. Testar loops, justiça em bifurcações e retorno de itens em redes sem saída antes de propor uma rede inteligente.
5. **Compatibilidade de documentos:** `ROADMAP.md`, `docs/ROADMAP_QUANTUM.md` e textos históricos podem registrar contagens/estados diferentes. Confirmar implementação e testes atuais; não corrigir silenciosamente regras de progressão durante uma tarefa de cabos.
6. **Renderização transparente:** não basta existir geometria para tubo de vidro; verificar camada de render, visibilidade do conteúdo, faces sobrepostas, luz, água, shader e fundo escuro/claro **dentro do jogo**.

## 3. Contrato transversal para TODOS os conduítes

Este contrato é **recomendação de engenharia** para mudanças futuras. Não substituir implementações existentes sem teste e aprovação.

### 3.1 Separar quatro responsabilidades

- **Topologia:** quais vizinhos estão conectados; atualizar ao colocar, remover, girar, alterar modo ou carregar/descarregar chunks. Não consultar chunks não carregados.
- **Permissão de face:** `AUTO`, entrada, saída, bloqueado ou outros modos **somente se já suportados**; cada lado deve validar ambos os extremos. A interface e a aparência não podem divergir da transferência real.
- **Transferência:** energia, fluido e item têm semânticas distintas; **não** usar a mesma rotina genérica sem preservar suas invariantes. Fluxo é contabilizado no servidor, com limites por aresta/recurso e sem criação de recursos.
- **Apresentação:** bloco, modelo multipart, indicador de fluxo, partículas e HUD refletem estado confirmado. Visual não é fonte de verdade da rede.

**Regra de consistência:** se a conexão está fechada, a malha visual não deve sugerir tubo aberto, a capability não deve permitir passagem por esse lado e a inspeção deve exibir o motivo. Se a máquina muda de orientação, portas e encaixes giram juntos.

### 3.2 Cabos elétricos — aprendizados específicos

**Mekanism verificado:** `UniversalCable` é um transmissor com **tier**, **buffer de energia**, cache de aceptores e operações transacionais; `EnergyNetwork` mantém buffer/rede e trata fusão de redes e distribuição. **AE2 verificado:** `CablePart` armazena conexões por lado e uso de canais por lado, com sincronização visual; `SmartCablePart` atualiza estado de exibição quando a rede muda. **Oritech verificado:** interfaces de tubo distinguem descoberta de rede, cache de destinos e agendamento de transferência.

**Decisões propostas para QI:**
1. Manter a diferença conceitual entre **barramento nu** e **conduíte isolado**, com materiais, forma, vazão e perdas coerentes. Não importar buffers de Mekanism: **os cabos atuais do QI são condutores sem armazenamento**, por contrato explícito.
2. Testar a conservação **`extraído = entregue + perda`** por tick, inclusive quando múltiplos geradores alimentam o mesmo trecho, há bifurcações e destinos parcialmente cheios.
3. Testar `rate` do **caminho inteiro** e do **trecho compartilhado**, não apenas a taxa de cada destino. Um cabo não deve transferir acima do limite ao atender vários consumidores.
4. Definir política explícita para prioridade/justiça; o QI já usa prioridade rotativa. Avaliar se há starvation com muitos consumidores.
5. Tratar conexões `UP/DOWN`, orientação de máquinas, modo de face, troca de tipo de cabo e chunks na borda.
6. Considerar diagnósticos como “FE/t transferido”, “perda/t”, “limite do gargalo”, “entrada bloqueada”, “rede limitada”; **não copiar** o conceito de canais do AE2 para FE comum.
7. Só considerar cache persistente de topologia se o custo real da BFS justificar; invalidar corretamente em qualquer mudança.

### 3.3 Tubos de fluidos, vidro, válvulas, bombas e gases

**Create verificado:** `FluidNetwork` propaga fluxo com pressão, identifica fontes/destinos, usa fases de simulação e execução e divide transferências; `FluidPropagator` atualiza pressão e redes afetadas por mudanças de tubo/bomba. **Mekanism verificado:** `MechanicalPipe` usa `ResourceHandler<FluidResource>`, valida compatibilidade de transmissores e participa de uma `FluidNetwork` com buffer. **QI verificado:** `FluidTransfer.move` usa transação atômica; `FluidPipeBlockEntity` tem tanque por trecho e regra própria de pressão.

**Decisões propostas:**
- Formalizar o comportamento desejado: gravidade, pressão, bomba, vazão, mistura, refluxo e orientação. **Não** chamar de simulação física realista se for uma regra de gameplay simplificada.
- Manter compatibilidade do tipo de fluido e de seus componentes, evitando mistura indevida ou transformação silenciosa.
- Conservar `volume_inicial + produzido - consumido = volume_final`, com rollback quando a saída recusa parte.
- Testar líquidos em T, laços, subida/descida, bomba desligada, tubo cheio, válvula fechada, quebra de trecho e reload.
- Vidro: usar **camada de render adequada**, contornos/aros legíveis, fluido visível e transparência sem sumir sobre céu/água; não resolver com vidro totalmente opaco se a função exige inspeção.
- **Gases/pressão atmosférica** são linhas futuras no roadmap; estudar o problema de temperatura, pressão e unidades, mas **não criar uma rede de gás genérica antes de existir caso de uso e contrato**.

### 3.4 Tubos pneumáticos, funis, roteadores e filtros

**Oritech verificado:** `ItemPipeInterfaceEntity` mantém **cache de caminhos**, usa `BlockCapabilityCache`, renderização de itens em tubos transparentes e `PipeTransferBackoff`; `SmartSplitterBlockEntity` implementa modos `STRICT`/`ROUND_ROBIN`, reservas e tratamento de saída bloqueada. **Mekanism verificado:** `LogisticalTransporterBase` tem pacotes em trânsito, cache de capabilities, conexões por face, transporte por cor e sincronização de itens visuais. **AE2 verificado:** `ImportBusPart`/`ExportBusPart` separam importação/exportação, filtros e contextos de transferência. **Create verificado:** há funis e filtros especializados em `content/logistics/`.

**Decisões propostas:**
- O pacote deve ter **um único dono** em cada momento: inventário de origem, tubo, máquina destino ou drop. Não copiar item antes de garantir que a origem foi decrementada.
- Em destino cheio, **reter pacote sem duplicar nem apagar**; definir política de espera, fallback, reversão ou descarte seguro. Evitar ciclos infinitos.
- Para redes pequenas, manter roteamento simples é aceitável; adotar pathfinding e cache **somente quando houver necessidade** (filtros, destinos múltiplos, caminhos longos).
- Diferenciar **filtro por item/tag**, **prioridade**, **round-robin** e **regra de densidade** do separador gravitacional. A última é uma mecânica própria do QI, não um filtro universal.
- Em inventários de terceiros, usar capabilities NeoForge quando apropriado; preservar `ItemStack` e seus componentes, inserção parcial e slot de entrada/saída.
- Sincronizar **animação do pacote** sem transmitir o inventário inteiro a cada frame; testes com duas pessoas e rede de tubos extensa.
- Testar quebra de tubo com pacote, descarregamento de chunk, save/reload, conexão desconectada e máquinas removidas.

### 3.5 Eixos, engrenagens e movimento

**Create verificado:** `RotationPropagator` calcula modificadores de velocidade e sentido por conexão, verifica ciclos/contradições e limites; `KineticNetwork` acompanha **capacidade e stress**, com influência da velocidade. **QI verificado:** `MechanicalNetwork` já calcula capacidade/carga, direção e conexões axiais, mas não implementa por isso automaticamente o modelo completo do Create.

**Decisões propostas:**
- Conservar a **identidade própria** do sistema de Unidades Mecânicas (UM), a menos que o GDD aprove uma evolução.
- Eixo, mancal e acoplamento devem estar alinhados em **modelo, orientação e lógica**; máquinas não podem receber potência por face visualmente fechada.
- Definir como serão tratados eixos verticais, sentido contrário, múltiplas fontes, ciclos, sobrecarga e componentes desconectados.
- A rotação exibida deve corresponder ao estado real de potência; sem rotor girando quando a rede está parada.
- Se engrenagens, relações de velocidade e embreagens forem implementadas, especificar **primeiro** regras de propagação, limites e falhas; não importar o algoritmo do Create sem compatibilizar com UM e a arquitetura existente.

## 4. Contratos de máquinas, armazenamento e automação

### 4.1 Máquina individual

Toda máquina deve documentar: **pesquisa exigida, insumos, produtos, slots, lados de entrada/saída, recurso energético/mecânico, taxa, capacidade, duração, bloqueios, estado visual e drops**. Operações devem ocorrer no servidor. Se saída estiver cheia, não consumir recursos e perder produto. Persistir progresso e inventário de modo compatível com saves existentes.

**Referências verificadas:** [Create/SequencedAssemblyRecipe](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe.java), [Oritech/processamento](https://github.com/Rearth/Oritech/tree/26.1/src/main/java/rearth/oritech/block/entity/processing), [Mekanism/tiers](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/tier). **Proposta:** usar receitas/processos orientados a dados quando isso reduz duplicação, sem migrar todo o sistema atual sem necessidade.

### 4.2 Multiblocos

**Verificado:** Mekanism mantém `BoilerValidator` e `BoilerMultiblockData`; AE2 tem `MBCalculator`. **Proposta:** quando houver multibloco real, separar reconhecimento da estrutura, validação, estado formado/desfeito, inventário e render. Testar formação parcial, rotação, borda de chunk, quebra e reload. **Não criar um framework de multiblocos preventivamente.**

### 4.3 Armazenamento, rede digital e autocrafting

**AE2 verificado:** `Grid`, `GridNode`, `StorageService`, `PathingService`, `CraftingService` e `CraftingCalculation` separam topologia, estoque e planejamento de fabricação. **Mekanism verificado:** QIO tem estruturas próprias de armazenamento, frequência, pesquisa e crafting. **QI planejado:** `docs/ROADMAP_QUANTUM.md` descreve pedidos e autocrafting como objetivo da Era V.

**Proposta de arquitetura futura, não implementação existente:**
1. **Catálogo de recursos** e identidade do item (componentes incluídos), com quantidade disponível/reservada.
2. **Plano de fabricação** como grafo de dependências, com receitas alternativas e detecção de ciclos.
3. **Reserva transacional** de materiais e capacidade de máquina, evitando duas requisições consumirem o mesmo estoque.
4. **Execução monitorada** com estados: aguardando insumos, em produção, bloqueado, concluído, cancelado, recuperável.
5. **Recuperação de falhas**: queda do servidor, remoção de máquina, saída cheia, jogador desconectado, rede dividida.
6. **Interface clara**: por que o pedido não pode ser feito, quais insumos faltam e qual máquina está bloqueada.
7. **Segurança por jogador/equipe** e compatibilidade com pesquisa individual; acesso à rede não deve desbloquear tecnologia indevidamente.

**Não importar:** canais, controladores ou crafting CPUs do AE2 como requisitos automáticos. São decisões de design próprias do AE2, não condições necessárias para o QI.

## 5. Interface, visual, som, documentação e experiência

### 5.1 UI e inspeção

**Verificado:** Create fornece documentação visual Ponder; AE2 sincroniza indicadores de canal nos cabos inteligentes; Oritech organiza UI em módulos. **Proposta para QI:** cada subsistema deve responder a quatro perguntas sem abrir o código: **está conectado? está recebendo? por que parou? o que fazer?**

- Óculos de inspeção: lado configurado, tipo de rede, fluxo real por tick, perda/pressão/carga, capacidade, gargalo e bloqueio.
- GUI: entrada, saída, consumo por operação, progresso, pesquisa exigida e explicação de falha.
- Manual: explicar a **montagem no mundo** (exemplo de barramento, T de fluido, rede de eixos, filtro), não só receita.
- Acessibilidade: não depender apenas de cor; usar ícones, rótulos e contraste.

### 5.2 Aparência e animação de sistemas

**Aplicar [ART_DIRECTION.md](ART_DIRECTION.md) e os guias de era**:
- Cabos: **centro + braços** nas seis direções; distinguir material e isolação, conectar até a face da máquina sem buracos nem anéis flutuantes. Desligar visualmente a face bloqueada.
- Tubos: flanges alinhados e seção visível, curvas/T/cruzamentos com UV coerente; vidro legível e conteúdo distinguível.
- Eixos: acoplamento quadrado/mancal alinhado, rotação sem descontinuidade visual entre blocos.
- Máquinas: peças móveis com pivôs corretos, estados ocioso/trabalhando/bloqueado claros, sem animação independente da lógica.
- Pixel art: 16/32/128 são **resoluções de sprites/atlas**, não medida de complexidade ou garantia de qualidade; inspecionar densidade por face.

**Não copiar** modelos, texturas, animações, shaders, áudio, GUIs ou diagramas de outros mods. A licença de assets pode diferir da licença do código, especialmente no Oritech (README explicita créditos e licenças de terceiros).

## 6. Performance, dados, integração e segurança

### 6.1 Política de desempenho

**Oritech verificado:** `PipeTransferBackoff` reduz frequência de tentativas em interfaces ociosas; `GenericPipeInterfaceEntity` limita busca e mantém cache de rede/destinos. **Mekanism verificado:** transportadores usam cache de capabilities; **AE2 verificado:** `TickManagerService` separa agendamento de dispositivos. **Proposta:**
- Atualizar topologia por eventos quando seguro, não percorrer toda a rede em cada `BlockEntity.tick` sem necessidade.
- Orçamento por tick para BFS/pathfinding e processamento de pacotes; nunca forçar carregamento de chunk para transportar.
- Usar cache com invalidação explícita em mudança de face, máquina, chunk, rede e capability.
- Reduzir sync de dados quando o estado não muda; renderizar fluido/itens com informação suficiente sem replicar cada cálculo de servidor.
- **Medir** TPS/MSPT, pacotes e memória em cenários reproduzíveis antes de refatorar. Não alegar ganho de desempenho sem benchmark.

### 6.2 Persistência e sincronização

- Dados permanentes (estoque, pesquisa, configurações, pacotes em trânsito, progresso) devem sobreviver a save/reload; estados transitórios (cache, contadores de frame) devem ser reconstruídos.
- Redes que atravessam chunks precisam de política explícita para **parada, divisão e reconexão**; não tratar chunk descarregado como bloco vazio com permissão de extração.
- Não usar dados enviados pelo cliente como autoridade para custo, transferência, pesquisa, crafting ou permissão.
- Validar migração de campos antigos e mudanças de ID; não reescrever saves para introduzir estética ou nova rede.

### 6.3 Interoperabilidade

- Compatibilidade com **NeoForge 26.1.2.125** deve ser verificada nas dependências instaladas; Create do branch consultado é de **1.21.1**, Mekanism consultado é **26.3 alpha**, Oritech é **26.1** e AE2 pode mudar. **Nunca colar assinaturas de API diretamente** de outro branch.
- Expor/consumir `Capabilities` de energia, fluido e item conforme a versão atual e regras de face; testar com blocos vanilla e implementações de terceiros.
- Diferenciar **FE** de redes de armazenamento de itens (AE2/QIO) e de **UM**; não presumir conversão automática de recursos.
- Reutilização de código ou assets exige análise de licença, atribuição e compatibilidade, além de decisão explícita da equipe.

## 7. Matriz mínima de QA por sistema

| Sistema | Cenários de teste | Invariantes / observações |
|---|---|---|
| **Cabos FE** | Linha, T, cruzamento, vertical, loop, 2 fontes, 3 consumidores, fonte vazia, destino cheio | `extraído = entregue + perda`, limite por trecho, justiça, conexão correta |
| **Modos de face** | AUTO/entrada/saída/bloqueado, ambos extremos, giro de máquina, salvar e reabrir | O render e a transferência concordam; modo persiste |
| **Chunks** | Rede atravessando borda, descarregar/recarregar, remover ponta, duas dimensões | Sem carga forçada, vazamento, duplicação, cache obsoleto ou crash |
| **Fluidos** | Água, lava/outro fluido, mistura, T, loop, subida com/sem bomba, tubo cheio, válvula | Volume conservado, rollback, pressão e tipo corretos |
| **Vidro** | Fundo claro/escuro, céu, água, luz baixa, fluido cheio/vazio, shaders quando aplicável | Tubo visível, fluido legível, sem faces quebradas |
| **Itens** | Tubo cheio, destino cheio, splitter, 2 destinos, loop, quebra com pacote, reload | Um único dono por pacote; sem duplicar/apagar |
| **Mecânica** | Eixos X/Y/Z, roda-d'água, serraria, dínamo, sobrecarga, duas fontes, conexão errada | Carga compartilhada, sentido coerente, animação sincronizada |
| **Máquinas** | Entrada inválida, saída cheia, falta de FE/UM, troca de receita, save/reload | Não consumir sem produzir; progresso e drops corretos |
| **Pesquisa** | Dois jogadores, mesma máquina, receita bloqueada, migração, comando admin | Progresso individual; servidor valida desbloqueios |
| **GUI/HUD** | Máquina parada, gargalo, erro de lado, fluxo parcial, inventário cheio | Motivo de bloqueio e próximo passo compreensíveis |
| **Performance** | Redes de 1/32/128/256+ segmentos, 50 máquinas, 2 jogadores, chunks alternando | Registrar MSPT, sync, custo de busca; sem prometer escala ilimitada |
| **Integração** | Inventário vanilla, bloco de outro mod, capabilities por face, inserção parcial | Sem bypass de filtro, sem incompatibilidade silenciosa |
| **Visual** | Inventário, GUI, mundo, primeira/terceira pessoa, iluminação, animação | Pixel art nítida, silhueta, UVs, pivôs, encaixes, sem z-fighting |

**Execução:** usar os testes e harnesses já existentes em `tests/`, o procedimento de `AGENTS.md` e QA real no Minecraft. Testes estáticos de presença de código são **complementares**, não substituem testes de transferência, save/reload e multiplayer.

## 8. Ordem recomendada de adoção

1. **P0 — estabilizar o que existe:** cabos FE, modos de face, tubos de fluido/vidro, tubos pneumáticos, rede mecânica e testes de conservação. Auditoria primeiro; mudanças pequenas e isoladas.
2. **P1 — qualidade de uso:** diagnósticos nos óculos, GUI de bloqueio, filtros/roteamento quando houver caso concreto, coerência visual de conectores e animação.
3. **P2 — escala e compatibilidade:** cache de topologia baseado em medições, interoperabilidade via capabilities, testes de multiplayer e chunks, melhorias de automação.
4. **P3 — sistemas futuros do roadmap:** redes digitais, autocrafting, controle, multiblocos, química/gases e infraestrutura da Era V; especificação e protótipo próprios antes de implementação.

**Regra para agentes:** ao trabalhar em qualquer subsistema, selecionar a linha correspondente do mapa (§1), **ler os arquivos externos indicados e os arquivos locais (§2)**, formular o contrato de comportamento, verificar APIs do ambiente atual, fazer um protótipo/patch mínimo, testar e documentar decisões. Não aplicar todas as recomendações em um único PR; não declarar o atlas como prova de que todos os sistemas foram implementados ou validados.

## 9. Limitações e manutenção

Este é um **atlas abrangente por área**, não uma auditoria linha a linha de cada repositório externo. Foram verificados arquivos representativos e estruturas de código; **não** foram inspecionados todos os shaders, modelos, receitas, testes, microbenchmarks, redes de gás e APIs de cada versão. Atualizar esta referência com links para arquivos específicos e resultados de testes quando uma tarefa exigir profundidade adicional. Não inventar números de throughput, desempenho, compatibilidade ou comportamento não observado.

Quando surgir uma nova família de máquinas ou subsistema, adicionar uma linha à matriz, arquivos locais, decisões de design, testes e limitações, preservando a separação entre **fato observado**, **proposta** e **resultado medido**.
