# Dínamo — acionamento manual e mecânico

O bloco mantém o ID `manual_dynamo`. Os blocos antigos começam sem manivela e
com 0 FE: o antigo sinal de redstone não representa energia acumulada.

- Instale uma `hand_crank` com o botão direito. Em sobrevivência o encaixe usa
  um item; um encaixe de criativo não gera um item de volta.
- Com Projeto do Dínamo concluído, clique no dínamo para girar. Cada giro
  aceito gera 12 FE, com intervalo mínimo de 8 ticks e capacidade de 240 FE.
- Agache e clique com as duas mãos vazias para retirar a manivela. A energia
  continua no bloco. Quebrar o bloco também devolve a manivela paga.
- Equipe os óculos na cabeça e mire no dínamo a até seis blocos para ver a carga.
  Receita: cobre / fio de cobre / cobre na primeira linha e vidro / couro /
  vidro na segunda.

O armazenamento usa `SimpleEnergyHandler` e uma transação do NeoForge. O bloco
envia o estado da Block Entity quando ele muda e quando o chunk começa a ser
observado. O giro sincronizado e o estado mecânico dirigem a animação.
A energia acumulada pode alimentar cabos e máquinas pela porta elétrica lateral.

## Acionamento por roda-d'água

O mesmo bloco, exibido como **Dínamo**, recebe movimento pelo encaixe lateral
da manivela. Conecte uma roda-d'água a eixos alinhados e termine o eixo nesse
encaixe. A saída elétrica está na face oposta. A manivela deve ser retirada
antes da conexão. O conhecimento de Projeto do Dínamo autoriza a instalação
ao colocar o bloco. Em um dínamo antigo, clique uma vez com esse conhecimento.
O operador autorizado fica salvo e a máquina continua funcionando após sair.
Se esse conhecimento for limpo, a geração automática fica suspensa.

A carga é **6 UM** e a geração é **12 FE por 8 ticks de movimento válido**,
equivalente a 1,5 FE/t. O dínamo divide a capacidade da roda com as demais
máquinas da rede. Sobrecarregar a rede, retirar o eixo ou interromper o fluxo
de água suspende a geração e zera o ciclo parcial. O buffer cheio para o rotor
até haver espaço. A energia guardada permanece disponível na porta elétrica.
Os óculos indicam se o acionamento é manual ou mecânico.

A verificação dedicada pode ser executada por `bash tools/verify-hydraulic-dynamo.sh`.
Ela usa `tests/hydraulic-dynamo.init.gradle`, com
`-PhydraulicRunDir=build/hydraulic-dynamo/runs/<execucao>`. Prepare `world/`
com EULA aceita e servidor local de teste, `pause-when-empty-seconds=0`, e rode
`runServer` duas vezes com esse init script. Confira `result.txt` em cada fase
e `complete.txt` ao final. São verificados pesquisa, água/eixo, orientação,
sobrecarga, operação manual, buffer cheio, saves antigos, reinício real e
transferência ao acumulador. Os saves normais ficam fora desse roteiro.

A verificação manual preexistente em servidor usa `tests/dynamo.init.gradle` e o diretório
isolado `build/dynamo-verification/world`. Crie esse diretório, aceite a
EULA do Minecraft no arquivo `eula.txt` e configure um servidor de teste
local em `server.properties` com `server-ip=127.0.0.1`, `online-mode=false`
e uma porta livre. Execute `./gradlew --init-script tests/dynamo.init.gradle runServer`
e confira o resultado em `build/dynamo-verification/result.txt`. A captura local usa
`tests/dynamo-preview.init.gradle` e um save de teste chamado `DynamoQA` em
`build/dynamo-preview/saves`. As classes desses testes não entram no JAR normal.

## Roteiro de sobrevivência da Era I

1. Em um mundo novo, fabrique o Analisador Científico Tier 1, conclua Observação
   dos Materiais e fabrique o Analisador de Materiais liberado pela pesquisa. Sua receita usa ferro, pepita e ferro na primeira linha, três tábuas
   na segunda e dois troncos nas pontas da terceira. Faça o ensaio mecânico do cobre para registrar a deformação.
2. Após o ensaio mecânico do cobre, pesquise Maleabilidade e Conformação Manual.
   Faça o ensaio mecânico do ferro para pesquisar Chapas Metálicas e Mecânica.
   Fabrique o martelo e a bancada de conformação. Martele ferro para obter chapas.
3. Conclua Mecânica Fundamental e Transmissão. Inspecione o ferro e pesquise Magnetismo. Fabrique engrenagens,
   eixos e uma manivela à parte. Inspecione o cobre para pesquisar Condutividade e inspecione carvão ou quartzo
   para pesquisar Isolamento. Conclua Fabricação de Fios, Isolamento e Bobinagem para produzir os fios e as bobinas.
4. Realize o ensaio de aquecimento do ferro ou do cobre, pesquise Estrutura Eletromecânica e fabrique a estrutura. A mesma pesquisa
   libera o núcleo: chapa / engrenagem / chapa; pepita / estrutura / pepita;
   vazio / eixo / vazio.
5. Conclua Projeto do Dínamo. Fabrique o dínamo com bobina / engrenagem / bobina;
   chapa / núcleo / chapa; fio isolado / eixo / fio isolado. A Era II fica
   disponível e Indução Eletromagnética permanece como pesquisa dessa era.
6. Instale a manivela, gire o dínamo e confira a carga com os óculos. Salve e
   reabra o mundo. Compare também com um jogador sem pesquisa na mesma bancada
   de trabalho para conferir os bloqueios individuais.

O núcleo reúne suportes e transmissão mecânica. O conjunto magnético é interno
ao dínamo e faz parte da montagem final, sem um ingrediente magnético separado
na V1. O giro movimenta esse conjunto em relação às bobinas. Magnetismo segue
como pré-requisito da Estrutura Eletromecânica.
