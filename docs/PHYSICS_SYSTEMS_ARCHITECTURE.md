# Arquitetura dos sistemas físicos

A Era I ensina Mecânica Clássica e Ciência Empírica com o **Quantum Kinetic System (QKS)**. A Era II, Industrialização Científica, abre Termometria e Termodinâmica com o **Quantum Thermal System (QTS)** e Eletrostática e Eletrodinâmica com o **Quantum Electrical System (QES)**. A faixa de Integração Industrial agrupa aplicações compartilhadas. O Dínamo Manual liga a oficina da Era I à geração da Era II. Os tubos pneumáticos, o ejetor e o classificador continuam na Era II.

## ADR 0002 — Ramos por metadados e conversão nas máquinas

- **Contexto.** Uma tecnologia pode combinar princípios de vários domínios. É preciso comunicar essas relações na interface e manter o significado dos IDs gravados nos mundos e relatórios.
- **Decisão.** O catálogo associa cada estudo da Era II a uma faixa de apresentação. As dependências científicas continuam no grafo de estudos. Cada motor possui seu estado e suas regras de atualização. As máquinas integradoras definem as fronteiras e escalas de conversão.
- **Alternativas avaliadas.** Árvores isoladas duplicariam estudos compartilhados e exigiriam tradução entre IDs. Um bloqueio por era impediria caminhos independentes, como recozimento e trefilação. Um motor unificado acoplaria o passo de atualização e os requisitos de carregamento de todas as redes.
- **Consequências.** A organização pode mudar sem migração. Novos estudos usam IDs próprios, e o servidor valida pré-requisitos e evidências por jogador. Uma conversão exige balanço explícito em ambas as pontas. O Códice acompanha as pesquisas concluídas como leitura opcional. [Classificação no catálogo](../src/main/java/br/com/quantumindustries/research/ResearchCatalog.java#L161) · [Validação no servidor](../src/main/java/br/com/quantumindustries/research/WorldResearchData.java#L152) · [Desbloqueio de capítulos](../src/main/java/br/com/quantumindustries/menu/QuantumGuidebookMenu.java#L49)

## Estado, unidades e aproximações

| Sistema | Base observada no checkout | Convenção e evolução |
| --- | --- | --- |
| QKS | A dinâmica por componente integra rotação com inércia, torque, carga e arrasto. O núcleo calcula potência a partir de torque e velocidade angular e energia de rotação a partir de inércia e velocidade angular ao quadrado. | Ângulo em radianos e velocidade em rad/s, com RPM na apresentação. Capacidade e carga usam UM de gameplay. Uma máquina que converte movimento declara sua escala para FE. [Dinâmica cinética](../src/main/java/br/com/quantumindustries/mechanical/KineticDynamics.java#L64) · [Arquitetura QKS](KINETIC_ENGINE_ARCHITECTURE.md) |
| QES | A distribuição existente roteia FE por cabos carregados, considera capacidade do destino, vazão por rota e perda calculada pelo número de trechos nus. | FE mede energia armazenada ou transferida e FE/t mede transferência por tick. Tensão, corrente e resistência requerem um modelo resistivo de corrente contínua com retorno implícito. V = R × I, P = V × I e P = I² × R são referências para essa evolução. A solução nodal e a simulação de formas de onda CA ficam pendentes de verificação. [Transferência elétrica](../src/main/java/br/com/quantumindustries/energy/CableNetwork.java#L73) |
| QTS | O gerenciamento térmico existente calcula a troca como diferença entre contato e ambiente multiplicada pela condutância efetiva do módulo, com arredondamento. Sua unidade térmica de gameplay é FE-equivalente. | Um modelo de reservatório distingue energia interna de temperatura. A capacidade térmica C relaciona a variação de temperatura ao calor recebido por ΔT = Q/C. Para a condução, o fluxo depende da diferença de temperaturas e da condutância do contato. Valores de gameplay exigem uma escala validada antes de serem apresentados como °C ou J. [Troca térmica existente](../src/main/java/br/com/quantumindustries/energy/ThermalExchange.java#L3) |

## Contrato de conservação

O balanço de uma conversão, em uma escala comum declarada pelo adaptador, é **energia de entrada + redução da reserva = saída útil + perdas**. Aumento de reserva conta como energia retida. Para cada transferência, defina origem, destino, unidade, quantidade debitada, quantidade aceita e destino das perdas. Em trocas internas, a quantidade debitada de um corpo é creditada ao outro. Calor transferido ao ambiente sai da reserva da máquina. Perdas elétricas convertidas em calor passam a pertencer ao balanço térmico.

| Integração | Balanço esperado |
| --- | --- |
| Motor elétrico | FE debitada financia trabalho QKS e perdas. |
| Gerador eletromagnético | Trabalho QKS financia FE produzida e perdas. Um ciclo motor–gerador consome energia disponível. |
| Fornalha Elétrica | FE aceita aumenta a reserva térmica. As receitas e as trocas com o ambiente consomem essa reserva. O calor residual só sustenta operações enquanto houver saldo suficiente. |
| Motor a vapor e gerador termoelétrico | A energia que sai da fonte térmica financia a saída útil e o calor rejeitado. |

Valide portas, limites por tick, receita, espaço de saída e reservas antes de confirmar a operação. O débito corresponde à entrega confirmada, usando a mesma transação lógica no servidor. A energia permanece atribuída a uma reserva durante reinícios e mudanças de topologia. Dividir redes, unir componentes ou repetir callbacks no mesmo tick conserva o saldo. Cada adaptador deve declarar arredondamento e tolerância nos testes de balanço.

## Runtime e persistência

- O servidor avança os estados autoritativos. O cliente renderiza snapshots de telemetria. A instrumentação distingue energia injetada, entregue, perdida e armazenada. O tráfego intermediário serve para diagnosticar limites locais.
- Percorra somente chunks carregados e imponha limites por componente. O roteamento elétrico atual limita a travessia a 128 nós. Um solver persistente por componente deve reutilizar a topologia e invalidar o cache em mudanças de portas, conexões e chunks. [Busca dos cabos](../src/main/java/br/com/quantumindustries/energy/CableNetwork.java#L21) · [Telemetria elétrica](ELECTRICAL_NETWORK_ANALYZER.md)
- Use passo de integração definido e limite as trocas térmicas ao equilíbrio alcançável naquele passo. Avalie custo por componente em redes pequenas, médias e grandes antes de ampliar os limites. Metas de desempenho e tolerâncias das novas integrações aguardam medição.
- Persista inventários, reservas e configurações nas máquinas responsáveis. Na pesquisa, preserve IDs e dados por jogador, incluindo a ordem congelada usada para decodificar os bitmasks legados. [IDs legados](../src/main/java/br/com/quantumindustries/research/LegacyResearch.java#L7)
- A progressão planejada para a Fornalha Elétrica converge em Efeito Joule, após Transferência de Calor, Indução Eletromagnética e Trefilação. O registro, a autorização de fabricação, o balanço em runtime e a aparência no jogo dependem da integração e do aceite da máquina. [Estudos eletrotérmicos](../src/main/java/br/com/quantumindustries/research/ResearchCatalog.java#L132)

## Eras futuras

- **Era III — Ondas Eletromagnéticas e Tecnologia Moderna.** QWS para ondas, ressonância e sinais. QPS para pneumática avançada, com pressão, vazão e compressão. A logística pneumática existente permanece na Era II.
- **Era IV — Engenharia Quântica.** Estados quânticos e caracterização avançada de materiais, com escala e alcance definidos por tecnologia.
- **Era V — Inteligência Artificial e Automação Autônoma.** Controle industrial, planejamento de produção e integração computacional sobre as máquinas existentes.

QWS, QPS e os sistemas das Eras IV e V permanecem em planejamento.
