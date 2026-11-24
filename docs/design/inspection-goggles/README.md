# Óculos de Inspeção do Quantum Industries — Especificação Científica e Arquitetura

## 1. Princípio Científico Inviolável

> **O overlay nunca pode fingir que o jogo simulou uma grandeza que ele não simulou.**

Os Óculos de Inspeção são um **instrumento científico progressivo**, não um HUD decorativo:
- Na **Era I** ajudam a entender mecanismos visíveis com dados reais do `MechanicalNetwork`.
- Na **Era II** passam a medir grandezas de gameplay simuladas no servidor (energia, fluxo, perdas, fluidos, conexões).
- Nas **Eras III–V** evoluem somente à medida que novas simulações físicas (circuitos, ondas, estados quânticos, diagnóstico autônomo) forem realmente implementadas.
- Conceitos sem simulação física no servidor (ex.: RPM antes de existir velocidade angular simulada, tensão/corrente antes de modelo de circuito, linhas de campo magnético) **não** podem aparecer como medição em tempo real — pertencem ao Guidebook como diagramas conceituais.

## 2. Matriz de Grandezas Verificadas no Código Atual

| Domínio | Fonte real no código | Grandezas reais disponíveis hoje | O que NÃO mostrar ainda |
|---|---|---|---|
| **Mecânico (Era I)** | `MechanicalNetwork`, `KineticNetworkManager`, `HydraulicInspection` | Capacidade/carga, estado, RPM, velocidade/aceleração angular, torques e inércia QKS; fluxo e parcelas hidráulicas locais sob demanda | Torque físico em N·m e força em newtons (não calibrados) |
| **Elétrico (Era II)** | `EnergyInspectable`, `CopperCableBlockEntity`, `CableNetwork`, `EnergyPorts` | Energia armazenada (`energy`/`capacity`), fluxo no tick (`flow`), perda (`loss`), conectividade e portas | Tensão (V), corrente (A), campo elétrico |
| **Fluido (Era II)** | `FluidInspectable`, `FluidPipeBlockEntity`, `FluidTransfer` | Volume armazenado (`storedFluid`/`fluidCapacity`), último fluxo, ticks de pressão | Pressão em Pascal, viscosidade |
| **Pneumático (Era II)** | `PneumaticTubeBlockEntity`, `ConfigurableConduit` | Pacote em trânsito, direção de entrada (`inlet`), progresso, modos de conexão (`AUTO`/`PULL`/`PUSH`/`NONE`) | Velocidade em m/s |

## 3. Evolução por Era

| Era | Estágio dos Óculos | Foco Científico |
|---|---|---|
| **Era I — Engenharia Mecânica e Empirismo** | Inspeção Mecânica | Eixo (`X`/`Y`/`Z`), sentido de rotação (`↻`/`↺`), capacidade vs. carga da rede, estado (ativa / sobrecarregada / sem fonte / limite de nós) e topologia 3D sob demanda |
| **Era II — Revolução Elétrica** | Lente Eletromecânica e Logística | Carga, fluxo FE/t, perdas em cabos, fluidos, pontas configuráveis (`PULL`/`PUSH`/`NONE`/`AUTO`) |
| **Era III — Ondas Eletromagnéticas** | Análise Eletrodinâmica | Tensão, corrente, polaridade, frequência e sensores *quando simulados no servidor* |
| **Era IV — Engenharia Quântica** | Visor Analítico | Estados e probabilidades definidos pela mecânica real da Era IV |
| **Era V — IA e Automação** | Diagnóstico Autônomo | Interpretação de gargalos, causas de parada e rotas derivadas das medições reais |

## 4. Arquitetura da API de Instrumentação

```text
EnergyInspectionGogglesItem (equipamento / desbloqueios)
        ↓
RequestInspectionPayload (C2S sob demanda — mudança de alvo ou cadência leve)
        ↓
InspectionRegistry → InspectionProvider (Mechanical, Energy, Fluid, Pneumatic...)
        ↓
InspectionSnapshotPayload (S2C — snapshot imutável e limitado em raio/nós)
        ↓
ClientInspectionCache (limpo ao trocar de dimensão ou desequipar)
        ↓
 ┌──────────────────────────┬──────────────────────────────┐
 │ DynamoInspectionHud      │ MechanicalOverlayRenderer    │
 │ (números + símbolos)     │ (linhas 3D + setas de eixo)  │
 └──────────────────────────┴──────────────────────────────┘
```

### Regras de rede e desempenho
- **Autoridade no servidor:** o servidor valida distância (`<= 10` blocos), chunk carregado e se o jogador está usando os óculos no slot de cabeça antes de consultar `MechanicalNetwork.state(server, pos)`.
- **Zero spam de pacotes:** o cliente só pede snapshot quando o alvo muda, quando o modo de topologia (`Shift`) muda, ou em cadência baixa (5 ticks) enquanto mira um bloco mecânico.
- **Topologia limitada:** `networkNodes` enviados ao cliente são limitados por raio (`<= 12` blocos) e teto (`<= 64` nós mais próximos), mantendo tráfego e FPS sob controle em fábricas grandes.
- **Isolamento dedicado:** nenhuma classe client-only é referenciada no registro comum de pacotes.

## 5. UX e Acessibilidade
- **Sem depender apenas de cor:** todo estado combina símbolo + texto + cor:
  - `●` Ativa
  - `!` Sobrecarregada / Limite de nós
  - `×` Sem fonte
  - `↻` Horário (`+1`) / `↺` Anti-horário (`-1`)
- **Camadas visuais:**
  - **Visão normal:** painel compacto apenas do bloco mirado.
  - **Modo rede (`Shift`):** contorno discreto dos nós da rede conectada e setas 3D indicando eixo e sentido de rotação.

## 6. Matriz de Validação (MVP Mecânico)

| Cenário | Resultado esperado |
|---|---|
| Sem óculos | Nenhum painel mecânico, nenhum pacote C2S enviado, nenhum overlay 3D |
| Bloco comum com óculos | Nenhum dado mecânico |
| Rede ativa | `● Ativa`, `capacidade / carga`, eixo real e sentido coerente com `spinDirection` |
| Rede sobrecarregada (`load > capacity`) | `! Sobrecarregada` com valores reais de carga e capacidade |
| Rede sem fonte (`capacity == 0`) | `× Sem fonte` |
| Quebrar eixo intermediário | Topologia e contagem de nós refletem a separação imediatamente |
| Segurar `Shift` olhando para rede | Overlay 3D destaca apenas nós da rede conectada dentro do raio |
| Trocar de dimensão / desequipar óculos | `ClientInspectionCache` limpo imediatamente |
| Servidor dedicado | Nenhuma classe `net.minecraft.client.*` carregada |

## Visualização hidráulica 3D

Shift mirando uma roda acrescenta fluxo e contribuições de torque reais do servidor, sem remover a rede. Setas simples azuis distinguem fluxo de setas duplas de torque; as contribuições favoráveis/contrárias usam verde/vermelho enquanto há movimento, e âmbar em repouso com referência ao eixo positivo mundial. A soma exibida é o acionamento hidráulico local após as escalas da roda, não o torque líquido de toda a rede. Disponível nas rodas 1×1, 3×3 e 5×5.

O protocolo especializado limita-se a 16 amostras e quatro consultas por segundo por jogador, valida o alvo pelo olhar atual e não carrega chunks. O cache correlaciona dimensão, alvo e geração e limpa ao sair da inspeção. [Estrutura, custos, comandos e evidências](../../HYDRAULIC_INSPECTION.md).
