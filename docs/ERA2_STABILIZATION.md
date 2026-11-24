# Estabilização: autorização, energia e ciência instrumental

## Alterações
- Britador, Forno de Ligas a Fole e Fundidora Elétrica registram o operador autorizado em automation_operator. Colocação/interação por jogador pesquisado vincula a máquina; inserção automatizada e processamento revalidam a pesquisa no servidor. Operador offline continua válido; revogação pausa processamento, e outro jogador pesquisado pode reautorizar. Inventários existentes permanecem recuperáveis, sem exigir pesquisa para retirada. Máquinas legadas devem receber uma interação autorizada antes de retomar o trabalho.
- Cabos: orçamento por tick contabiliza FE retirada da fonte, incluindo perda; a telemetria registra FE injetada, entregue e dissipada separadamente. Exemplo: quatro barramentos nus transferem até 32 FE/t, entregando 31 e dissipando 1; uma segunda transferência no mesmo tick não deve exceder o teto.
- Analisador Científico Tier 2: segure um lingote compatível na mão principal, papel na mão secundária, agache e clique com botão direito. A primeira caracterização consome um papel, não o lingote; consultas posteriores são gratuitas. São 14 perfis (cobre, ferro, ouro, estanho, zinco, chumbo, níquel, prata e seis ligas), com índices de gameplay de condutividade, dureza e estabilidade térmica (0–100). Os índices são referências normalizadas, não resultados de simulação física. O registro por jogador usa chaves estáveis separadas das descobertas empíricas e é copiado no evento de clone. O Tier 1 e seus quatro ensaios permanecem inalterados. Esta primeira entrega usa feedback de overlay, não uma GUI comparativa nem novos requisitos de pesquisa.

## Validação
Executado com JDK 21: EnergyCheck.java, GeneratorModel.java e TransferBudget.java, cujos blobs de produção conferem com a branch (2d3f7955faa70981d288667d76645d4461ff43d7 e bf6cc75e65c13ab834658e9cf55dd07edc1b7a32). PASS: 9244 asserções de modelos isolados. Reproduzir a partir da raiz:
```sh
javac -d /tmp/qi-energy-check src/main/java/br/com/quantumindustries/energy/GeneratorModel.java src/main/java/br/com/quantumindustries/energy/TransferBudget.java tests/energy-standalone/EnergyCheck.java
java -cp /tmp/qi-energy-check EnergyCheck
```
Adicionados ao harness de conexões: rejeição sem vínculo, rejeição de jogador sem pesquisa, vinculação autorizada, transferência com perda e limite agregado, distinção entre tiers, consumo único de papel. Esses testes Minecraft não foram executados nesta sessão; o ambiente não possui JDK 25/instalação NeoForge do projeto. A conservação global motor/QKS/alternadores, testes de reinício, multiplayer e playtest survival permanecem pendentes. Os testes isolados não demonstram conservação global nem substituem o servidor dedicado.

## Escopo restante
Revisar autorização dos demais processadores legados (prensa, trefiladora, braseiro, cuba, moedor); validar interação e visual da caracterização Tier 2, avaliar GUI e evidências avançadas integradas à árvore; medir carga mecânica e múltiplos geradores antes de alterar o balanceamento de conversão. Vault e IDs existentes não modificados.
