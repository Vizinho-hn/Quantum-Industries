# Interface de pesquisa — conceito aprovado

**Status:** mockup aprovado; interface funcional ainda não implementada.

O conceito de UI aprovado usa um painel tecnológico escuro com detalhes ciano, inspirado na Mesa de Pesquisa.

## Estrutura principal

- Cabeçalho: símbolo atômico e título **Mesa de Pesquisa**.
- Navegação à esquerda: Fundamentos, Energia, Máquinas, Materiais, Quântica e Dimensões.
- Centro: árvore de pesquisas, dependências visuais e nós bloqueados/desbloqueados.
- Painel à direita: nome da pesquisa, descrição, pré-requisitos, tempo e botão **Pesquisar**.
- Rodapé: slots de inventário do jogador e materiais necessários.

## Estados esperados

- Bloqueada: requisitos não atendidos.
- Disponível: pré-requisitos atendidos e pesquisa iniciável.
- Pesquisando: contagem de tempo e andamento.
- Concluída: indicador de desbloqueio persistente.

## Paleta do conceito

| Uso | Cor |
| --- | --- |
| Fundo | `#1A1D24` |
| Painéis | `#2B313D` |
| Bordas | `#4A556B` |
| Destaques | `#00D9FF` |
| Texto | `#E6EFFA` |

## Variações visuais

O mockup contém uma árvore principal e alternativas de lista de pesquisas, progressão linear e enciclopédia. A **árvore** foi escolhida como layout base.

## Arquivo de referência previsto

- `docs/design/mesa-de-pesquisa/research_ui_concept.png` — imagem do conceito visual aprovado.

**Observação:** esta especificação documenta a UI, mas o PNG do mockup e os componentes gráficos ainda precisam ser versionados e convertidos em texturas e código de tela no Minecraft.
