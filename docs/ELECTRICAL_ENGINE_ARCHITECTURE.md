# Quantum Electrical System — Era II

## Unidades e circuito

O QES representa corrente contínua. Cada bloco de cabo representa o caminho de
ida. As fontes e as cargas compartilham um retorno ideal de potencial zero,
implícito no modelo. A instalação usa uma única linha física de blocos.

| Grandeza | Unidade do motor | Relação implementada |
| --- | --- | --- |
| Potencial e queda | mV | ΔV = I · R / 1.000 |
| Corrente | mA | I = 1.000 · ΔV / R |
| Resistência | mΩ | Série soma resistências. Paralelo soma os inversos. |
| Potência equivalente | FE/t | P = V · I / 1.000.000 |
| Dissipação equivalente | FE/t | Pⱼ = I² · R / 1.000.000.000 |
| Energia | FE ou TU | Soma da potência equivalente ao longo dos ticks. |

A porta FE nominal usa 120.000 mV. A calibração é 1 W equivalente = 1 FE/t.
Essa é uma escala de gameplay. O FE armazenado continua representando uma
quantidade de energia e mA representa a corrente associada a um caminho.

## Solução nodal

`ElectricalSolver.solve` recebe nós indexados, fontes com tensão, resistência
interna e orçamentos próprios, cargas resistivas e arestas direcionais.
O sentido positivo da corrente segue a ordem dos terminais da aresta.
Uma face AUTO aceita os dois sentidos. PULL aceita entrada, PUSH permite saída
e NONE abre o circuito. A fonte bloqueia corrente reversa.
[ElectricalSolver.java, linha 125](../src/main/java/br/com/quantumindustries/energy/qes/ElectricalSolver.java#L125)

A matriz de condutâncias aplica a lei dos nós de Kirchhoff, com fatoração de
Cholesky. O conjunto de portas ativas é atualizado pela diferença de potencial
até estabilizar. A solução usa até 128 nós, 768 arestas, 128 fontes,
128 cargas e 16 iterações. Cada fatoração tem custo cúbico no número de nós
e memória quadrática. Um estouro de orçamento, uma solução inválida ou uma
falha de convergência devolve `SOLVER_BUDGET_EXCEEDED` com zero transferência.

Limites positivos de corrente e potência reduzem a tensão efetiva de um
componente inteiro. Essa aproximação conservadora preserva a corrente nos
nós, a distribuição proporcional entre caminhos e o estado das portas
reversas. Um único gargalo pode reduzir a entrega a ramificações saudáveis
do mesmo componente. Um segmento com orçamento zero é tratado como aberto
naquele cálculo. Carga abaixo da tensão mínima continua resistiva e recebe
diagnóstico de subtensão. Um curto é uma carga de 0 mΩ com piso numérico de
1 mΩ. A resistência interna e a da linha limitam sua corrente, os tetos de
corrente e potência podem reduzi-la ainda mais e a potência do curto entra como
perda. O modelo de falha é elétrico e passivo.

O resultado é imutável e inclui medições por nó, segmento, fonte e carga.
O erro de balanço permitido por arredondamento em ponto flutuante é
`1e-8 · max(1, extraído)` FE/t. O resíduo admissível de corrente em um nó é
`1e-5 · max(1, corrente)` mA. O balanço inteiro do adaptador FE é exato.

## Decisão 001 — Integração incremental com as portas FE

As portas existentes oferecem FE inteiro e confirmam a recepção por callback.
`CableNetwork.transfer` descobre a topologia dirigida em chunks carregados e
consulta a disponibilidade dos cabos em cada transferência. A descoberta é
reutilizada durante a chamada. A prioridade entre destinos gira por tick e um
caminho paralelo pode ser usado quando outro esgota sua vazão. Redes acima de
128 cabos ficam sem transferência nessa chamada. Mudanças de faces, interruptor
e chunks são refletidas na próxima chamada. O adaptador usa o orçamento dos
equipamentos e dos segmentos, sem cache persistente de topologia.
[CableNetwork.java, linha 41](../src/main/java/br/com/quantumindustries/energy/CableNetwork.java#L41)

O adaptador soma a resistência da linha e 100 mΩ de resistência interna
nominal. Para cada oferta, determina a menor raiz de
`P_carga = (V_fonte − I · R_total) · I`, sob o teto de corrente e da
vazão de FE. Após a confirmação do receptor, recalcula a perda a partir do
valor aceito, arredonda a dissipação para baixo em FE inteiro e pede um único
débito de `aceito + perda`. A diferença de perda acumulada entre segmentos
atribui cada FE dissipado uma vez. As frações de FE são descartadas por
transferência. Percursos com poucos cabos conservam as vazões legadas
de 32 e 64 FE/t. A nova perda depende da corrente e da resistência do caminho.
[ElectricalSolver.java, linha 97](../src/main/java/br/com/quantumindustries/energy/qes/ElectricalSolver.java#L97)

A solução nodal conjunta permanece disponível para grafos com fontes e
cargas caracterizadas. No mundo, a interface atual liquida uma fonte por
chamada. Fontes concorrentes compartilham a vazão por segmento no mesmo tick.
A divisão simultânea de corrente por todos os caminhos e a proteção contra
uma carga em curto usam a API nodal. Os blocos FE existentes ainda atuam como
receptores controlados e a alternativa de executar a solução nodal global
sobre eles exigiria um contrato de planejamento e confirmação por componente.
Foi descartada nesta etapa para preservar os callbacks e os orçamentos já
adotados pelas máquinas.

A telemetria do cabo registra a tensão de entrada mínima da rota, a soma
das magnitudes de corrente por tick, a resistência nominal do segmento
e o estado. O fluxo registra a vazão reservada, conservadoramente medida
pelo débito no início do caminho. A janela existente soma injeção na entrada
e entrega na saída uma vez por transferência. Os campos QES seguem pelo
update tag e são transitórios. O analisador mantém seu formato de amostragem
e suas telas.
[CopperCableBlockEntity.java, linha 79](../src/main/java/br/com/quantumindustries/block/entity/CopperCableBlockEntity.java#L79)

## Materiais, armazenamento e conversões

`ElectricalMaterial` descreve resistividade relativa, condutância inversa,
teto térmico e corrente de referência. Cobre, prata, ouro e electrum são
candidatos a distribuição. Constantan serve como elemento resistivo.
Porcelana, ebonite e couro são isolantes. Os parâmetros são de gameplay e
dependem da geometria. O barramento nu usa 3.500 mΩ por bloco e o conduíte
isolado usa 200 mΩ por bloco. O isolamento identifica o desenho do cabo.
Os tetos térmicos do catálogo são dados para integração futura com o QTS.
[ElectricalMaterial.java, linha 4](../src/main/java/br/com/quantumindustries/energy/qes/ElectricalMaterial.java#L4)

Pilhas e acumuladores preservam sua reserva FE e seus limites de carga e
descarga. Cabos preservam configurações salvas de face e possuem apenas
leituras transitórias de fluxo. Capacitores futuros podem usar Q = C · V e
E = ½ · C · V². Reservas galvânicas continuam sendo modeladas pelo armazenamento
FE existente.

`PhysicsEnergyContract` define recibos QKS/QES/QTS e rejeita quantidades
negativas ou saídas acima da energia disponível. A escala de Joule puro é
1 FE = 1 TU. Converter integralmente 40 FE por aquecimento resistivo dá 40 TU.
O adaptador contabiliza perdas da linha como dissipação FE. Depositar esse
calor num corpo térmico depende de integração com o QTS. UM continua sendo
a unidade mecânica interna do QKS. Uma conversão que põe QKS na contabilidade
energética deve normalizar a quantidade e manter a escala explícita.
O motor existente paga 24 FE por tick para disponibilizar até 18 UM.
O alternador devolve no máximo 8 FE por tick, conforme a curva líquida de
`GeneratorModel`. O retorno do ciclo fechado é de até um terço do custo
elétrico, antes das perdas de linha e da excitação.
[PhysicsEnergyContract.java, linha 19](../src/main/java/br/com/quantumindustries/energy/PhysicsEnergyContract.java#L19)
[RotaryConversionBlockEntity.java, linha 25](../src/main/java/br/com/quantumindustries/block/entity/RotaryConversionBlockEntity.java#L25)
[GeneratorModel.java, linha 16](../src/main/java/br/com/quantumindustries/energy/GeneratorModel.java#L16)

## Verificação independente

Com JDK 25, a partir da raiz do repositório:

```bash
out=$(mktemp -d)
base=src/main/java/br/com/quantumindustries/energy
javac -d "$out" "$base"/{GeneratorModel,PhysicsEnergyContract}.java "$base"/qes/*.java \
    tests/br/com/quantumindustries/energy/ElectricalSystemCheck.java
java -cp "$out" br.com.quantumindustries.energy.ElectricalSystemCheck
```

O verificador cobre circuitos puros, o contrato energético e a cotação FE,
incluindo aceitação parcial e orçamento zero. Ele imprime a média medida
de 30 soluções após aquecimento para grafos de 8, 32 e 128 nós e compara
cada resultado ao estado de referência. Esses tempos medem o solucionador
Java isolado. Na execução local, as médias foram 0,771 ms, 0,277 ms e 1,663 ms,
respectivamente. As médias de uma execução incluem variação da JVM e do host.
O teste de servidor de cabos passou no GameTestServer do NeoForge com portas,
interruptor, baterias, topologia e analisador.
[ElectricalSystemCheck.java, linha 19](../tests/br/com/quantumindustries/energy/ElectricalSystemCheck.java#L19)
