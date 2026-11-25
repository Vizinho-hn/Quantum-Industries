# Padrão de Conexão Mecânica

**Status:** padrão geométrico oficial para transmissão mecânica em todas as eras.
**Escopo:** eixos, máquinas, rodas, engrenagens, esteiras e futuras transmissões mecânicas.

## Referência dimensional

Os modelos usam coordenadas de 0 a 16 em cada bloco Minecraft de 16×16×16.

| Propriedade | Padrão |
|---|---|
| Altura nominal de eixo horizontal | `y = 8` |
| Centro do encaixe na face | `8, 8` nas duas dimensões da face |
| Seção nominal do encaixe | `4×4`, centrada no eixo |
| Extremidade do eixo | Termina alinhada à fronteira do bloco (`0` ou `16`) |
| Orientações | Eixos `X`, `Y` e `Z`, conforme a face de conexão |
| Folga mínima livre para rotação | Envelope de `6×6` ao redor do eixo |

Para um eixo horizontal em `X` ou `Z`, a seção ocupa `y=6..10` e `4` unidades na outra dimensão transversal. Para um eixo vertical em `Y`, ela fica centrada em `8` nas duas dimensões da face. A folga de `6×6` é espaço desobstruído para peças móveis, não o tamanho do conector.

O shaft usa corpo de `6×6` e duas pontas independentes. Uma ponta livre mostra o veio de topo da madeira em `6×6`. Dois shafts ligados usam `6×6` na junção. Máquinas mantêm o encaixe padrão de `4×4` e podem declarar um encaixe compacto de `3×3` na face correspondente. A seção muda apenas no render e exige compatibilidade nas duas faces. O centro da face, a fronteira do bloco e a transmissão de torque permanecem os mesmos. A folga deve cobrir a diagonal da maior peça giratória, incluindo as cintas do shaft.


O eixo deve alcançar a fronteira da face funcional, sem fresta nem ponta recuada. Não adicione conectores em faces sem transmissão funcional. Renderização, geometria e animação precisam compartilhar centro, orientação, pivô e espaço de rotação; não faça peças parecerem encaixadas quando seus eixos estão desalinhados.

## Adaptação interna de máquinas

O ponto externo de acoplamento permanece no padrão mesmo quando o mecanismo de trabalho fica mais baixo, mais alto ou fora do centro. Faça a transmissão interna alcançar o mecanismo. Não desloque o conector externo para acompanhar uma peça interna e não crie um modelo de shaft exclusivo para cada máquina.

### Esteira mecânica

A esteira pode manter a superfície baixa, com meio bloco de altura. A solução de referência é integrar uma pequena transmissão lateral que recebe o eixo na altura `8` e leva o movimento ao mecanismo inferior. A face, a geometria e o caminho cinemático devem coincidir.

Essa transmissão lateral é uma proposta de implementação a validar. Antes de adotá-la, prototipe no Blockbench e verifique encaixe visual, folga de rotação de pelo menos `6×6`, hitbox, orientação e animação no Minecraft. A conexão com QKS deve conservar razão, sentido de rotação e perdas da transmissão interna. Não exija um adaptador do jogador para corrigir o desalinhamento do modelo da esteira.

## Topologia e verificações

O QKS atualmente reconhece transmissão pelas faces e eixos reportados pelos participantes; uma conexão aceita pela topologia não comprova que a geometria está coaxial. Ao criar ou remodelar uma peça mecânica, confira ambas as coisas: portas declaradas no código e dimensões/pivôs do modelo `.bbmodel` ou equivalente. Não use a topologia para justificar torque através de eixos visualmente desalinhados.

Para qualquer exceção ou nova transmissão deslocada, documente a conversão interna de eixo, razão, sentido e perdas antes de implementá-la. Um Acoplador Deslocado é uma ferramenta opcional de engenharia futura; sua menção aqui não aprova criação, receita ou integração desse bloco.

As rodas d’água perfil D usam encaixes de `3×3` (pequena), `4×4` (média) e `6×6` (grande). A ponta do shaft ajusta a seção e alcança o cubo de madeira e bronze com o mesmo centro em Y=8.
