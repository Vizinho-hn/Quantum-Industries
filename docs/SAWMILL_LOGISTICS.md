# Serraria, tubos e inspeção elétrica

## Transporte da serraria

A ponte `ItemRouting` reconhece a Serraria Mecânica como destino e origem. A inserção automatizada aceita apenas materiais de receitas mecânicas existentes e compatíveis com a pilha atual, até o limite nativo do item. A extração recolhe exclusivamente produtos e respeita a quantidade solicitada; jamais retira toras não processadas. As receitas permanecem inalteradas (carvalho: uma tora para seis tábuas).

Na origem, configure Puxar para retirar itens do baú e Empurrar na direção da serraria. Na saída da serraria, a ponta do tubo voltada para a máquina deve estar em Puxar; a outra segue para o baú de produtos. A seleção inteligente ignora materiais não aceitos pela serraria e respeita as mesmas conexões/chunks dos tubos existentes.

## Pesquisa por jogador e persistência

`automation_operator` guarda o UUID do jogador autorizado que colocou a máquina ou estabeleceu seu primeiro vínculo. Para máquinas antigas sem vínculo, interaja com ela depois de pesquisar Transmissão Mecânica. O primeiro vínculo não é substituído por outros jogadores. A pesquisa exigida pela receita é consultada no `WorldResearchData` desse jogador; estar offline não apaga conhecimento. Limpar a pesquisa bloqueia novas inserções, e o lote em processamento continua obedecendo à autorização original. Remover a última tora não apaga o vínculo de automação.

Os IDs, receitas, operador do lote, progresso, pilhas e comportamento manual preexistentes são preservados. Nenhuma pesquisa global é concedida para permitir automação.

## Painel dos óculos

A arte segue a referência industrial em cobre e fundo escuro. Os estados e estoques vêm do update tag da Block Entity no servidor. A duração vem da receita real. Cortando exige receita permitida, espaço na saída e acionamento QKS; giro por inércia não equivale a corte. Recebendo/Enviando indicam transferências automáticas reais nos últimos 20 ticks, enquanto Aguardando, Bloqueado ou Sem conexão identificam outras situações. Retirada/inserção manual não simula atividade dos tubos.

Shift preserva o painel QKS abaixo da ficha da serraria. Óculos removidos ou mudança de alvo ocultam o painel pelo mesmo teste de alvo/equipamento da inspeção existente.

## Cabos: fluxo versus reserva

Os cabos existentes não são baterias. O painel agora exibe explicitamente 0 FE armazenado no próprio cabo, fluxo/limite em FE/t, perdas recentes e reserva real dos equipamentos conectados. Para snapshots ENERGY de cabos, `load`/`capacity` carregam a reserva/capacidade agregada e `localCapacity`/`localLoad` continuam fluxo/limite. O codec e os snapshots dos demais equipamentos mantêm o formato anterior.

A coleta não carrega chunks, respeita as portas e os modos e visita no máximo 128 nós. Cada equipamento é somado uma vez; condutores são excluídos para não transformar tráfego em FE. O total se refere ao trecho inspecionado, podendo ser parcial em fábricas maiores. Perdas não são reservas e a alteração não cria energia.

## Verificação

`tests/sawmill-logistics.init.gradle` executa servidor dedicado em duas fases sobre o mesmo mundo isolado: seleção de materiais no baú, autorização individual, produção real de duas toras para doze tábuas, coleta, pilhas e vínculo após reinício, update tag e reserva dos dois tipos de cabo. `tests/sawmill-preview.init.gradle` abre um mundo isolado em segundo plano para conferir o HUD da serraria, saída desativada e leitura dos dois cabos.

As fixtures fornecem água controlada e energia a equipamentos reais, sem substituir o transporte ou as receitas por mocks. Fontes de QA ficam fora do JAR de produção. Capturas e resultados são documentados após a execução; não representam todas as resoluções ou topologias possíveis.

### Evidências desta entrega

- Servidor dedicado: 26 verificações write e 6 após reinício; nenhuma classe client-only carregada.
- Regressão dos transportes existentes: 541 verificações e 11 após reinício (itens, fluidos, energia e fundidora).
- Suíte geral: 58 testes Python aprovados; build, verifyLegacyResearch e verifyMechanicalFoundation aprovados.
- Cliente real: quatro capturas nativas revisadas em PT-BR, 1600×1000, GUI 2: [serraria em operação](images/sawmill-logistics/01-sawmill-running.png), [saída desativada](images/sawmill-logistics/02-sawmill-output-disconnected.png), [cabo isolado](images/sawmill-logistics/03-insulated-reserve.png) e [fio de cobre](images/sawmill-logistics/04-busbar-reserve.png).

Comandos (JDK 25 e dependências instaladas):

```bash
bash tools/verify-sawmill-logistics.sh
./gradlew --max-workers=2 --init-script tests/sawmill-preview.init.gradle runClient --console=plain
```

O preview requer uma cópia de um mundo de QA em `build/sawmill-preview/saves/SawmillQA`; não use um mundo pessoal nessa pasta. Outras resoluções, servidores com mods adicionais e fábricas acima do limite da inspeção exigem QA de gameplay adicional.
