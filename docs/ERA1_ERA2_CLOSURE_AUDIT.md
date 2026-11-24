# Fechamento das Eras I e II — auditoria estática
Base: 183af0be67a6e37937a0b6f8706b2b4ef1ccc01e (main). Esta revisão não executou Minecraft, Gradle ou servidor dedicado. Não representa aprovação de gameplay. Vault: impacto sobre estruturas é intencional; não alterar.

## 1. Progressão
Verificação estática executada nesta auditoria: 34 estudos (18 Era I, 16 Era II); todos os pré-requisitos declarados encontrados, nenhum ciclo entre estudos e nenhuma dependência declarada Era I -> Era II. A verificação não prova acessibilidade de todos os ingredientes/ensaios.
- Forno a Fole usa fogueira vanilla em vez de Braseiro. Britador usa engrenagens, bancada de conformação, eixo e chapas em vez de relé. Correções já presentes na base.
- AlloyCatalog.MANUAL_MAX_TIER=2: ambas as fundidoras aceitam seis ligas; manual exige calor relativo 160 ou 240 nas complexas (fluxo opcional -40), dobro do tempo elétrico nas complexas e maior combustível. Constantan inicial já tem rota manual.
- Caixa compacta e esteira requerem Transmissão e Chapas. Revisar outros crafts com bronze, em particular volante, polias e roda grande: indicar Metalurgia de Ligas na lista de requisitos quando necessário. A engrenagem de bronze ainda usa núcleo mecânico primitivo, avaliar simplificação para lingotes + eixo/ferro sem quebrar IDs.
- Tier 2 exige dínamo e beneficiamento mineral. ResearchTableMenu bloqueia Era II no Tier 1. Não confundir visibilidade das abas com autorização de pesquisa.
- Executar verificação de alcançabilidade de ingredientes, incluindo receitas vanilla, drops, ferramentas e ensaios. Não utilizar unlock administrativo como prova.

## 2. Autorização
ResearchTableMenu verifica proprietário do menu, validade, servidor, tier, pré-requisitos, pagamento e learn. FoundationRecipe verifica pesquisas via ResearchCraftingAccess. Serrarias e conversores novos vinculam UUID de operador e verificam conhecimento persistente.
Risco: ItemRouting chama JawCrusherBlockEntity.insert, ElectricAlloySmelterBlockEntity.insertIngredient e BellowsAlloyKilnBlockEntity.insert sem fornecer jogador. As rotas manuais/menu têm gates, mas esses métodos não apresentam uma verificação equivalente de operador autorizado. Revisar também prensa, trefiladora, braseiro e cuba. Não afirmar exploit completo sem reproduzir com jogador sem conhecimento.
Contrato recomendado: máquina compartilhável, autorização persistente por operação de instalação/vínculo explícito validado no servidor; transporte sem jogador consulta esse vínculo; revalidar pesquisa antes de processar, inclusive após revogação; extração e recuperação de conteúdo não podem prender itens. Máquina legada sem vínculo mantém inventário/energia e solicita interação autorizada. Testar transporte por tubos, esteira, funil/capabilities, menus, shift-click e quebra. Não usar UUID arbitrário recebido do cliente.

## 3. Energia — valores verificados
| Equipamento | Produção/consumo | Buffer FE |
|---|---|---:|
| Dínamo QKS | 0–1,5 FE/t, proporcional até 12 RPM | 240 |
| Alternador | curva líquida 0–8 FE/t; máximo 50–60 RPM, corte em 90 RPM, excitação inicial 24 FE, proteção térmica | 1200 |
| Termoelétrico | máximo 6 FE/t, condicionado ao modelo térmico | 480 |
| Motor elétrico | 24 FE/t pagos para próximo tick, capacidade 18 UM | 1200 |
| Motor a vapor | 10 unidades de vapor/tick quando disponível diretamente da caldeira, capacidade 24 UM | — |
| Prensa | 3 FE/t, 60 FE/chapa normal | 480 |
| Britador | 3 FE/t, 60 FE/ciclo elétrico | 480 |
| Serraria elétrica | 4 FE/t, 160 FE/8 tábuas | 1200 |
| Fundidora | 80 FE/80t (bronze/latão), 120 FE/100t (invar/electrum/constantan), 160 FE/120t (aço); cobrança por receita | 1200 |
| Pilha | orçamento de transferência 48 FE/t | 2400 |
| Acumulador chumbo-ácido | orçamento de transferência 256 FE/t | 50000 |
| Barramento/conduíte | 32/64 FE/t por cabo; perdas nus por rota | sem buffer |

Proposta de balanceamento inicial: manter dínamo, alternador, termoelétrico, consumidores e limites dos cabos até medir rede real. Um alternador ideal alimenta duas serrarias sem perdas; não simultaneamente serraria+prensa+britador (10 FE/t). Acumulador grande é reserva, não geração; uma saída via conduíte de 64 limita a transferência nessa rota. Motor de 24 FE/t exige três alternadores ideais somente para igualar consumo; testar fontes/relações múltiplas para excluir ciclo líquido positivo, incluindo dínamos, volantes, gravidade e mola. O limite de 8 FE/t de um alternador isolado não prova conservação global. Revisar cobrança do trabalho mecânico efetivo, cargas e reconfiguração QKS. Se o motor ficar impraticável, testar consumo proporcional à carga, mínimo em vazio e teto 24 FE/t, com escala compartilhada de conversão. Não baixar custo isoladamente.
Aferir FE extraída vs recebida + perdas, inclusive barramento com 4/8 segmentos, múltiplos emissores e receptores, mesmo tick, buffers cheios e loops. Validar taxa agregada, não apenas por chamada. Verificar se recordTransfer contabiliza o mesmo conceito de vazão que o orçamento da rota, inclusive perdas. PRs #53 e #56 em aberto não são baseline.

## 4. Materiais
Manter seis ligas e 18 minerais adicionais, sem adicionar minérios. Bronze: mancais e engrenagens; latão: válvulas e instrumentos; aço: estrutura industrial; invar: instrumentos estáveis; electrum: contatos (não declarar superior ao cobre puro); constantan: resistências e termoeletricidade. Níquel e prata alimentam ligas; estanho e zinco são insumos iniciais; chumbo para baterias; grafite para refratários e carbono; fluorita é fluxo, não combustível; apatita e enxofre para química/fertilizantes. Halita -> HCl é abstração já documentada, não reação real. Al, Ti, W, U, Os, Pt, Co e cinábrio: reservar aplicações avançadas ou avaliar utilidade pontual, sem tornar todos obrigatórios. Revisar obtenção fácil de metais estratégicos sem remover minérios de saves. Preferir funções distintas a tiers universais de material.

## 5. Tier 2 — proposta funcional, ainda não implementada
Preservar mesa de pesquisa como organização dos conhecimentos e bancada como executor dos ensaios. Tier 1 qualitativo e quatro métodos atuais (13 materiais x 4 = 52); Tier 2 habilita comparação instrumental e fichas avançadas. Primeiro vertical slice: níquel, prata e seis ligas; comparar condutividade relativa, dureza/maleabilidade e comportamento térmico em catálogo de propriedades normalizadas. Identificar referências científicas e valores efetivamente simulados separadamente; não inventar medição SI. Vincular ensaio avançado a instrumento/bancada Tier 2, não apenas à aba de pesquisa. Reutilizar evidências antigas. Persistir novas evidências por ID estável, sem reinterpretar bitmask antigo; testes de migração. Não condicionar a fabricação inicial do Tier 2 às suas próprias evidências. Composição atômica fica para eras futuras.

## 6. Playtest de aceitação (pendente)
Criar mundo Survival novo, sem cheats, Creative, give, unlock ou suprimentos preparados. Registrar seed, duração, recursos obtidos, pesquisa, receita, gargalo e captura real. Começar pelo Vault (impacto intencional), mesa, Observação, bancada de ensaio e instrumentos; adquirir ferro/cobre, chapas, engrenagens, eixo, serraria e roda; metalurgia e seis ligas; britador, caixa, esteira, volante, dínamo e mesa Tier 2. Após Era I, aprender indução, construir pilha, cabos, prensa, fundidora com constantan manual, serraria elétrica, acumulador, alternador com excitação, interruptor, bomba, caldeira/motor e termoelétrico. Validar ramos opcionais sem forçar todos como pré-requisito. Confirmar conservação com redes carregadas e reinício. Repetir operação compartilhada com dois jogadores humanos, conhecimento diferente, sem liberar pesquisas via comandos. Guardar logs e screenshots e registrar falhas, não declarar aprovado sem executar.
