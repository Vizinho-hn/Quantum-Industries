# Estabilização: autorização, energia e ciência instrumental

## Autorizações e compatibilidade
- Britador, Forno de Ligas a Fole, Fundidora Elétrica, Prensa, Trefiladora, Braseiro, Cuba de Decantação e Moedor Manual registram o operador em `automation_operator`. Colocação/interação de jogador qualificado vincula a máquina; o servidor revalida a pesquisa para inserção e produção. A recuperação de itens não exige pesquisa. A Serraria Elétrica e os conversores rotativos também permitem reautorização após revogação.
- O operador offline continua válido. Se sua pesquisa for removida, outro jogador qualificado pode reautorizar. Máquinas de saves antigos conservam inventários/energia e precisam de uma interação autorizada para retomar a produção. Transporte por tubos consulta a autorização no look-ahead; a inserção final também é protegida. Reservatórios da cuba continuam aceitando água como infraestrutura de fluidos, mas não processam reagentes sem autorização.
- Não alteramos IDs históricos, nem o impacto intencional do Vault.

## Energia
- Cabos contabilizam a quantidade extraída incluindo perdas na vazão de cada segmento; telemetria separa injeção, entrega e dissipação. Quatro barramentos nus: até 32 FE extraídos, 31 entregues e 1 dissipado por transferência saturada.
- QKS registra trabalho resistente por participante a partir da resistência efetivamente usada pelo solver e deslocamento angular conservador. Cada posição pode reivindicar esse trabalho uma vez por tick; refreshes no mesmo tick não apagam a allowance de outras redes.
- Dínamo e alternador limitam a curva de geração ao trabalho resistente contabilizado. A escala de gameplay é 2 FE por unidade de trabalho mecânico para o motor e 1,8 FE por unidade para conversão geradora. Com torque 18, velocidade limitada a 12 rad/s e 0,05 s, o motor tem trabalho nominal máximo equivalente a 21,6 FE por tick, inferior aos 24 FE pagos antecipadamente. O cálculo é conservador e não é calibração SI. As curvas anteriores são tetos, não promessas de geração; pouca carga, baixa velocidade e perdas reduzem a produção. A prova de conservação global depende ainda da dinâmica do solver, das topologias e dos testes de runtime.
- Não alterar indiscriminadamente capacidades de baterias e cabos. Revisar posteriormente os limites agregados das entradas legadas que ainda são por chamada e a eficiência observada em uma fábrica survival.

## Analisador científico Tier 2
Abra a GUI do Tier 2, selecione Base de Conhecimento > Análise instrumental. Clique na linha de um material com lingote correspondente e papel no inventário. O servidor verifica pesquisa, tier, menu ativo, distância/permissão, sample e pagamento; consome somente um papel e registra o conhecimento. Consultas e cliques repetidos não consomem novamente. O atalho de agachar e usar um lingote com papel na outra mão também existe, sujeito à interação vanilla.
- 14 metais e ligas, índices relativos de condutividade, dureza e estabilidade térmica (0–100). Valores de gameplay, não medições SI nem propriedades simuladas. A tabela mostra ? para registros ainda não obtidos, sincronizados pelo menu.
- As análises de cobre e ferro são evidências reutilizáveis da pesquisa Geração Industrial. Não há dependência da pesquisa para obter os dois lingotes ou construir o Tier 2. Estudos já concluídos permanecem concluídos.
- Registros instrumentais por IDs estáveis, separados das 52 descobertas empíricas; preservação no evento de clone. Traduções PT-BR/EN-US e orientação no Códice.

## Validação
- Executado localmente com JDK 21: GeneratorModelCheck, com GeneratorModel.java e GeneratorModelCheck.java conferidos por Git blob SHA (7866e4aa49864409f5fc53eb34e505bbea4c3e2d e f85037b1fa6580f67deca21fb2cb55812c78a2f2). PASS: curvas de geração, limite de trabalho, pagamento do motor e partilha teórica entre 1–64 geradores e 1–16 motores.
- Reproduzir: `javac -d /tmp/qi-energy-check src/main/java/br/com/quantumindustries/energy/GeneratorModel.java tests/br/com/quantumindustries/energy/GeneratorModelCheck.java`; `java -cp /tmp/qi-energy-check br.com.quantumindustries.energy.GeneratorModelCheck`.
- A execução Actions 45337018146 identificou import HashSet ausente, corrigido em cf54e616. Execução 45337339108 passou contratos/recursos e compilação/migração de pesquisa nessa revisão anterior. Não extrapolar resultados para o head posterior.
- Adicionados testes de servidor para todos os processadores, rejeição de operador sem pesquisa, revogação, reautorização, restart, perdas e vazão, análise Tier 1/Tier 2, consumo e repetição pela GUI, e evidência de geração industrial.
- QKS: fixture de dois motores, dois alternadores, caixa de transmissão e volante, com entradas FE contabilizadas, verificação de output positivo e reivindicação única de trabalho.
- CI roda Java 25, verificações de conexão, QKS, hidráulica e persistência. Consultar resultados do head do PR antes de aprovar. Os testes adicionados ainda precisam de resultado final confirmado; não equivalem a prova global de conservação.
- Ambiente local: JDK21; tentativa de git ls-remote falhou por DNS. Não foi possível executar cliente Minecraft e playtest de coleta real, sem comandos, nem multiplayer humano. Os testes de servidor com suprimentos e pesquisas preparados não substituem esse playtest.

## Aceitação restante
Aprovar somente após CI verde no head, validar visual/escala da GUI no cliente e jogar a progressão Era I–II sem cheats nem recursos preparados. Validar desempenho e balanceamento de trabalho/FE, múltiplas redes, reconfiguração de engrenagens e persistência. Não iniciar a Era III automaticamente.
