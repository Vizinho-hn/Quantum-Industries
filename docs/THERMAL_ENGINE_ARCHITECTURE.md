# Quantum Thermal System (QTS)

O QTS simula reservas de calor concentradas em corpos de máquinas. O servidor executa um passo discreto por tick. Cada corpo tem uma temperatura uniforme, capacidade térmica fixa e contatos direcionais opcionais. As máquinas existentes mantêm seus modelos até que adotem a API.

## Unidades e balanço

- **TU** é a unidade térmica interna. **1 TU = 1 FE-eq** é uma escala de contabilidade de gameplay. Uma máquina que converte energia precisa debitar a origem, creditar o destino e contabilizar perdas. A igualdade de escala, por si só, não fornece eletricidade.
- **°C** representa temperatura de gameplay. Os coeficientes são calibrados para simulação por tick. As grandezas não foram calibradas em joules ou watts.
- A capacidade térmica é `C = m × c`. A energia transferida é `Q = m × c × ΔT = C × ΔT`. A temperatura é `T = T_ref + U/C`, com `C ≥ 1 TU/°C`. O corpo usa `T_ref = 0 °C` e guarda `U` como um inteiro assinado em TU. Valores negativos são válidos. Na mesma temperatura, corpos com capacidades diferentes podem ter reservas diferentes.
- A primeira lei fornece `ΔU = Q − W`, com calor recebido e trabalho produzido positivos nas respectivas parcelas. Para um par isolado, o calor debitado é exatamente o creditado, com tolerância de **0 TU**. A atmosfera é uma fronteira externa, com a troca reportada por passo. O QTS não simula calor latente `Q = m × L` nesta implementação.

As conversões estão em [ThermalMath.java](../src/main/java/br/com/quantumindustries/thermal/ThermalMath.java#L13).

## Condução e equilíbrio

Para capacidades `C₁, C₂`, energias `U₁, U₂` e condutância de contato `G` em milicondutância, o passo usa as equações abaixo.

```text
ΔT = T₁ − T₂
qRaw = round(ΔT × G / 1000)
qMax = floor(|ΔT| × C₁ × C₂ / (C₁ + C₂))
     = floor(|U₁ × C₂ − U₂ × C₁| / (C₁ + C₂))
q = clamp(qRaw, −qMax, qMax)
U₁' = U₁ − q
U₂' = U₂ + q
```

O limite inteiro permite chegar a `T_eq = (U₁ + U₂) / (C₁ + C₂)` quando a quantidade exata cabe em TU inteiras. Quando o limite cai abaixo de 1 TU, a troca para com um gradiente residual. Empates no arredondamento de `qRaw` seguem `Math.round`, na direção de mais infinito. O cálculo usa produtos inteiros e recorre a precisão arbitrária quando a aritmética intermediária excede `long`. O corpo aplica ainda o limite de armazenamento representável antes de debitar e creditar. [ThermalMath.java](../src/main/java/br/com/quantumindustries/thermal/ThermalMath.java#L24) · [ThermalBody.java](../src/main/java/br/com/quantumindustries/thermal/ThermalBody.java#L111)

## Ambiente, inércia e materiais

A temperatura base vem de bioma e dimensão por meio de `ThermalExchange.ambient`. Cada face carregada que toca lava, fogo, magma, fogueira acesa ou forno vanilla aceso contribui com um sexto da diferença para a base. Reutilizamos as temperaturas de contato do termoelétrico. Faces comuns e faces sem chunk disponível contribuem com zero. A configuração descreve influência local, sem inventário de combustível ou orçamento elétrico nessas fontes ambientais. [ThermalNetwork.java](../src/main/java/br/com/quantumindustries/thermal/ThermalNetwork.java#L24) · [ThermalExchange.java](../src/main/java/br/com/quantumindustries/energy/ThermalExchange.java#L9) · [ThermoelectricModel.java](../src/main/java/br/com/quantumindustries/energy/ThermoelectricModel.java#L9)

No corpo, `G_efetiva = G_passiva × (1000 − isolamento)/1000 + G_ativa`. A parcela ativa só entra quando o corpo está mais quente que o ambiente e a máquina deve fornecer zero quando a dissipação ativa estiver desligada ou sem alimentação. O passo segue o gradiente e limita o fluxo a `|U − C × T_amb|`. A troca passiva pode aquecer um corpo frio. A capacidade limita a variação a `ΔT = Q/C`, então o desligamento da fonte deixa calor residual que se dissipa em passos subsequentes. [ThermalMath.java](../src/main/java/br/com/quantumindustries/thermal/ThermalMath.java#L47)

`ThermalMaterial` reúne condutividade relativa em milicondutância por unidade de contato, calor específico em TU/(unidade·°C), isolamento em permil e limites de uso. A máquina escolhe capacidade e condutâncias efetivas com base na quantidade, na geometria e nos contatos. Os coeficientes e os limites iniciais são decisões de balanceamento. Os limites operacionais orientam a lógica da máquina e a telemetria. Calor que excede um limite operacional continua registrado no corpo. [ThermalMaterial.java](../src/main/java/br/com/quantumindustries/thermal/ThermalMaterial.java#L4) · [ThermalBody.java](../src/main/java/br/com/quantumindustries/thermal/ThermalBody.java#L32)

## Decisão de runtime

O QTS usa troca sequencial de pares adjacentes. Cada corpo que executa o tick é responsável por suas faces positivas (cima, sul e leste). Ambos os contatos precisam estar habilitados. A menor condutância dos dois limita a interface. Um marcador por corpo impede reprocessamento dos pares próprios no mesmo tick. Cada transferência atualiza as duas reservas, registra o fluxo nos dois corpos e marca os chunks para salvamento. O motor consulta chunks já presentes e entidades já instanciadas. [ThermalNetwork.java](../src/main/java/br/com/quantumindustries/thermal/ThermalNetwork.java#L53) · [ThermalBody.java](../src/main/java/br/com/quantumindustries/thermal/ThermalBody.java#L81)

Uma busca por componentes com resolução simultânea exigiria construir e invalidar topologia e coordenar orçamentos em nós com múltiplos contatos. Repartir a troca entre os dois ticks do mesmo par também exigiria coordenar arredondamento e deduplicação. Com a propriedade canônica, cada chamada consulta até três vizinhos e a amostragem ambiental consulta até seis. Contatos são lidos do estado atual e não exigem cache de topologia. A ordem dos ticks pode influir na distribuição intermediária em cadeias, mas cada troca conserva energia e respeita seu equilíbrio local. Se o dono canônico não estiver executando o tick, aquele par fica suspenso.

## Persistência e API para máquinas

1. Crie um `ThermalBody(initialTempC, heatCapacityTuPerC, passiveConductanceMilli, insulationPermil, minOperatingTempC, maxOperatingTempC)` por máquina.
2. No tick do servidor, chame `beginTick(level.getGameTime())` antes de aplicar calor ou dissipação. O método preserva as contribuições que chegaram antes do tick da própria máquina. Use `addHeat(long)` após debitar a fonte. `consumeHeat(long)` retorna a parcela efetivamente retirada da reserva positiva acima da referência. Para condução, use `transferTo` ou a rede, que também podem transferir calor entre corpos abaixo de zero.
3. Consulte `ambientAt(level, pos)` e aplique `stepAmbient(ambientC, activeCoolingConductanceMilli)` uma vez por tick. A máquina deve sinalizar alterações próprias para salvamento. Para trocar calor entre blocos, implemente `ThermalParticipant` e chame `stepNeighbors(level, pos, this)`.
4. Delegue o save/load da Block Entity a `save(out, prefix)` e `load(in, prefix, defaultTempC)`. O campo `prefix + "energy_tu"` é um `long` assinado. Configuração e capacidade vêm do tipo de máquina. Saves anteriores começam à temperatura escolhida pela máquina. Se uma migração mudar a capacidade, deve decidir explicitamente como transformar o estado anterior. Telemetria e marcadores reiniciam na carga.
5. Para inspeção, implemente `ThermalInspectable.thermalSnapshot()` usando `body.snapshot(targetTemperatureC)`. Aquecimento e resfriamento são magnitudes em TU/t. Fluxo líquido e fluxo de aresta são positivos quando entram no corpo. O retorno de `transferTo` é positivo quando sai do corpo de origem. As arestas mostram trocas efetivas do tick e o fluxo de exibição é limitado ao intervalo de `int`. O snapshot copia a lista e as posições. Limites mínimos de operação podem ser consultados no corpo. A máquina decide quando sincronizar a leitura.

[API do corpo](../src/main/java/br/com/quantumindustries/thermal/ThermalBody.java#L73) · [Persistência](../src/main/java/br/com/quantumindustries/thermal/ThermalBody.java#L149) · [Snapshot](../src/main/java/br/com/quantumindustries/thermal/ThermalSnapshot.java#L9) · [Participante](../src/main/java/br/com/quantumindustries/thermal/ThermalParticipant.java#L6) · [Inspeção](../src/main/java/br/com/quantumindustries/thermal/ThermalInspectable.java#L3)

## Verificação

O entrypoint `br.com.quantumindustries.thermal.ThermalSystemCheck` exercita os dez requisitos térmicos com corpos isolados e faz round-trip pela leitura e escrita binária do NBT do Minecraft. O microbenchmark mede 100 e 1.000 pares com transferências efetivas e reporta tempo total e tempo por par. As medições abrangem aritmética e aplicação do fluxo. A verificação da rede num servidor, do descarregamento de chunks e da retomada de um mundo salvo faz parte da integração. [ThermalSystemCheck.java](../tests/br/com/quantumindustries/thermal/ThermalSystemCheck.java#L27)
