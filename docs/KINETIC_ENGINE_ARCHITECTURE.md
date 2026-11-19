# Quantum Kinetic System — arquitetura de migração

**Estado:** proposta técnica e primeiro núcleo matemático implementado em `KineticDynamics.java`. **O sistema ainda NÃO substitui a rede mecânica nem o renderizador em jogo.** Não afirmar que física variável ou renderer sem GeckoLib já estão ativos.

**Objetivo aprovado:** permitir eixos, rodas-d'água de tamanhos diferentes, engrenagens, polias e volantes com velocidade variável, aceleração e inércia, usando um motor cinético **próprio do Quantum Industries**. Manter GeckoLib onde houver animações complexas não cinéticas até a respectiva migração. Não remover a dependência global enquanto existir qualquer consumidor.

## 1. Diagnóstico confirmado na main (19/11/2026)

- `mechanical/MechanicalNetwork.java`: grafo conectado que soma capacidade e carga (`UM`) e obtém sentido agregado; o booleano `powered` não é uma dinâmica de rotação.
- `mechanical/RotationalBlockEntity.java` e `KineticMath.java`: ângulo derivado de tempo de mundo e **9°/tick**, ou ~30 RPM em 20 TPS, sem inércia.
- `client/render/KineticRenderer.java`: extrai estado e define bone `rotor` proceduralmente, porém ainda **extends `GeoBlockRenderer` e usa GeckoLib** para renderizar geometria. Não é renderer independente.
- `block/entity/PrimitiveWaterWheelBlockEntity.java` e `HydraulicTorque.java`: amostram fluxo local e sinal do torque. A fonte concede capacidade fixa enquanto ativa; o valor de torque estimado ainda não regula velocidade de rotação.
- `research/GuidebookCatalog.java`: capítulos atrelados à pesquisa; o novo capítulo de física usa a pesquisa `mechanical_transmission` existente e não cria estudo novo.
- `MechanicalSawmillBlockEntity`, `ManualDynamoBlockEntity` e rotas de energia compartilham interfaces atuais. Mudá-las indiscriminadamente pode quebrar receitas, FE, geração e migração.

## 2. Separar quatro camadas

1. **Topologia / transmissão (servidor):** participantes, portas, grafos, relações de engrenagens, embreagens, limites de segurança, chunks. Grafo deve validar fechamentos/ciclos, múltiplas fontes com sentidos conflitantes e relações incompatíveis.
2. **Dinâmica física (servidor):** `θ` ângulo, `ω` velocidade angular assinada, `α` aceleração, `I` inércia equivalente, torques de fonte/carga/perda. Calcular **uma vez por componente cinético**, não por shaft por tick.
3. **Sincronização:** servidor autoritativo publica snapshot compacto (id/versão da rede, ângulo, ω, α, instante de referência, estado de falha e razão de transmissão). Cliente interpola/extrapola entre snapshots; mudanças abruptas devem evitar teletransporte de ângulo e desincronização de multiplayer.
4. **Renderização própria (cliente):** um `BlockEntityRenderer<T, S>` com `BlockEntityRenderState` e `SubmitNodeCollector` de **NeoForge 26.1.2**, sem herança de GeckoLib para **shafts, rodas e engrenagens**. Geometria estática e girante separada, texturas/UVs preservadas e pivôs corretos. Integrar recurso baked/model layer/cache a partir dos assets existentes com validação em jogo. **Não** renderizar manualmente todas as faces a cada frame se puder usar mesh compilada/cache.

## 3. Equação do movimento

Para cada componente rígido sem relações de engrenagens:

```text
tau_resultante = soma(tau_fontes_assinados) - tau_carga_contra_movimento - b * omega
alpha = tau_resultante / I_equivalente
omega_novo = clamp(omega_antigo + alpha * dt, -omega_max, +omega_max)
theta_novo = wrap(theta_antigo + (omega_antigo + omega_novo)/2 * dt)
dt = 0,05 s a 20 TPS
```

- `KineticDynamics.advance(...)` implementa **apenas esta equação para um rotor equivalente**. `torque` e `inércia` são escalas de gameplay coerentes entre si, **não** N·m nem kg·m². Exibir valores físicos reais só após calibração.
- Com torque insuficiente a partir do repouso, o sistema não inicia; sem acionamento, a resistência o desacelera sem reversão espontânea.
- `maxSpeed` limita valores e mantém estabilidade. A estabilidade numérica depende de `dt`, `I` e `b`; medir em runtime.
- Engrenagens ideais: `ω_2/ω_1 = -Z_1/Z_2`, `τ_2/τ_1 ≈ -Z_2/Z_1`, com perdas definidas. Calcular **inércia e torques refletidos para um eixo de referência**, sem aplicar a mesma inércia várias vezes.
- A potência é `P = τω` e a energia de rotação `E = Iω²/2`. A energia de inércia deve vir de trabalho anterior; engrenagem não cria energia.
- Em parada por carga excessiva, definir regra de falha (estol/limite) separada de animação residual; **não permitir serraria produzir itens só porque a roda continua girando por inércia**.

## 4. Compatibilidade e progressão do jogo

- **Não apagar** `MechanicalNetwork` nem redefinir a unidade `UM` de uma vez. Criar adaptador entre capacidade/carga atuais e torque de gameplay. Só remover o caminho legado quando testes de produção, progressão e multiplayer forem aprovados.
- Preservar pesquisas por jogador, IDs, itens, receitas, mundo antigo, chunk boundaries, geração de FE do dínamo e posições de modelos.
- A roda atual permanece 1 bloco enquanto novos diâmetros são prototipados. Rodas 3×3 e 5×5 são **propostas**, ainda não estão implementadas.
- A água de Minecraft não expõe uma simulação fluida contínua real. Transformar vetor de fluxo local em torque de gameplay calibrado, sem declarar cálculo físico de vazão/queda como medida científica exata.
- Reutilizar e aperfeiçoar `HydraulicTorque`, sem regredir o sentido de giro invertido por lado/fluxo.

## 5. Plano de implementação incremental e critérios de saída

### Etapa A — Base testável (este PR)
- [x] Criar integrador de dinâmica **independente do jogo e do GeckoLib** com testes determinísticos.
- [x] Adicionar capítulo **opcional** de física rotacional ao Códice após a pesquisa já existente, em PT-BR e EN-US.
- [x] Documentar contrato, migração e critérios de aceitação.
- [ ] Executar `gradlew build` e teste no cliente 26.1.2 (não possível no ambiente do conector GitHub).

### Etapa B — Física na rede real
- [ ] Um estado por componente de rede: θ, ω, α, I; fontes, cargas e perdas; avaliação uma vez/tick.
- [ ] Atualizar bloco/tile com snapshot leve e ritmo de sync limitado. Desacoplar `invalidate` topológico de mudança de ângulo por frame.
- [ ] Acionar máquinas apenas quando há recurso e potência disponíveis, com regressão de serraria, dínamo e saves.
- [ ] Roda varia rotação pelo efeito do fluxo; desacelera ao remover água; sentido muda corretamente para fluxo oposto.

### Etapa C — Renderer sem GeckoLib (migração de verdade)
- [ ] Converter/usar geometria de `mechanical_shaft_block` e `primitive_water_wheel` em mesh estática + rotor de forma **idêntica** e preservar os `.bbmodel`.
- [ ] Implementar `QuantumKineticRenderer` de NeoForge 26.1.2 sem `GeoBlockRenderer`, `GeoModel`, `BoneSnapshots` ou `GeoBlockEntity` nesses dois blocos.
- [ ] Retirar a implementação GeckoLib de `RotationalBlockEntity` somente quando a serraria estiver desacoplada com segurança; separar interface de motor cinético da interface de animações complexas.
- [ ] Testes visual/escala, eixos X/Y/Z, pivôs, mancais parados, face/inventário, sobreposição transparente, distância e câmera.
- [ ] Nenhum visual 3D crítico desaparece durante a migração. Não trocar geometria complexa existente por blocos simples provisórios sem aprovação.

### Etapa D — Transmissões e rodas novas
- [ ] Engrenagem com razão de dentes, inversão de sentido, multiplicador/redutor, polia/correia.
- [ ] Eixos múltiplos e rodas pequenas/médias/grandes; modelagem e colisão multibloco, rendimento de água e inércia.
- [ ] Proteção contra fontes em sentido oposto, energia extra, ciclos de engrenagem inconsistentes e overflows.
- [ ] Benchmark MSPT, GPU, rede e snapshots vs GeckoLib nos cenários equivalentes.

### Etapa E — Óculos e física opcional avançada
- [ ] Óculos básicos: capacidade/carga, sentido, conexão, motivo de parada.
- [ ] Upgrade: RPM, aceleração, direção da potência e vetores indicativos, **somente se forem dados realmente simulados**.
- [ ] Livro: operação prática primeiro; fundamentos de MCU/MCUV/torque/engrenagens em capítulos opcionais sem novos bloqueios de crafting.

## 6. QA obrigatório

- **Física pura:** repouso, torque abaixo da carga, aceleração, inércia, frenagem, inversão, velocidade máxima, wrap, NaN/overflow.
- **Redes:** 1, 32, 128, 256 nós, 2 rodas com sentidos opostos, cadeia com polias/engrenagens, ciclo impossível, máquina desconectada.
- **Mundo:** fonte de água à esquerda/direita, água removida, fluxo por cima/baixo, chunk descarregado, save/reload, trocar de dimensão, dois jogadores.
- **Integridade:** sem duplicar FE, sem craft sem custo, sem apagar pacotes, sem perder pesquisas, sem mudar IDs.
- **Visual:** Mancal NÃO gira com eixo; eixo, abraçadeiras solidárias e rotor giram; roda corresponde ao sinal do torque; 20/30/60/120 FPS e baixos TPS.
- **Medição:** comparar CPU, quantidade de renderizações, snapshots e MSPT antes de declarar ganhos.

**Regra de entrega:** PRs separados por etapa; cada um deve compilar, testar e ser reversível. A **migração total é um objetivo**, não a descrição de uma funcionalidade já implementada. 
