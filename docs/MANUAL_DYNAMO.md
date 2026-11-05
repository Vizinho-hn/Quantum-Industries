# Dínamo Manual V1

O bloco mantém o ID `manual_dynamo`. Os blocos antigos começam sem manivela e
com 0 FE: o antigo sinal de redstone não representa energia acumulada.

- Instale uma `hand_crank` com o botão direito. Em sobrevivência o encaixe usa
  um item; um encaixe de criativo não gera um item de volta.
- Com Projeto do Dínamo concluído, clique no dínamo para girar. Cada giro
  aceito gera 12 FE, com intervalo mínimo de 10 ticks e capacidade de 240 FE.
- Agache e clique com as duas mãos vazias para retirar a manivela. A energia
  continua no bloco. Quebrar o bloco também devolve a manivela paga.
- Equipe os óculos na cabeça e mire no dínamo a até seis blocos para ver a carga.
  Receita: cobre / fio de cobre / cobre na primeira linha e vidro / couro /
  vidro na segunda.

O armazenamento usa `SimpleEnergyHandler` e uma transação do NeoForge. O bloco
envia o estado da Block Entity quando ele muda e quando o chunk começa a ser
observado. O último giro sincronizado dirige a animação. A V1 não expõe
capacidade de rede nem descarrega energia em repouso.

A verificação em servidor usa `tests/dynamo.init.gradle` e o diretório
isolado `build/dynamo-verification/world`. Crie esse diretório, aceite a
EULA do Minecraft no arquivo `eula.txt` e configure um servidor de teste
local em `server.properties` com `server-ip=127.0.0.1`, `online-mode=false`
e uma porta livre. Execute `./gradlew --init-script tests/dynamo.init.gradle runServer`
e confira o resultado em `build/dynamo-verification/result.txt`. A captura local usa
`tests/dynamo-preview.init.gradle` e um save de teste chamado `DynamoQA` em
`build/dynamo-preview/saves`. As classes desses testes não entram no JAR normal.

## Roteiro de sobrevivência da Era I

1. Em um mundo novo, fabrique o Analisador Científico Tier 1 e a Bancada de
   Ensaio. A Bancada de Ensaio usa cobre, vidro e cobre na primeira linha;
   tábuas, bancada de trabalho e tábuas na segunda; e tábuas nas duas pontas
   da terceira. Faça o ensaio mecânico do cobre para registrar a deformação.
2. Pesquise Observação, Maleabilidade, Conformação Manual e Chapas Metálicas.
   Fabrique o martelo e a bancada de conformação. Martele ferro para obter chapas.
3. Conclua Mecânica Fundamental, Transmissão e Magnetismo. Fabrique engrenagens,
   eixos e uma manivela à parte. Conclua Condutividade, Fabricação de Fios,
   Isolamento e Bobinagem para produzir os fios e as bobinas.
4. Pesquise Estrutura Eletromecânica e fabrique a estrutura. A mesma pesquisa
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
