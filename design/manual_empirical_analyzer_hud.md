# HUD — Analisador Empírico Manual (Era 0)

## Objetivo de experiência

Apresentar uma bancada de ensaio artesanal: o jogador escolhe uma amostra, posiciona utensílios simples, seleciona um experimento e registra uma observação no caderno. A tela comunica ciência empírica e descoberta por tentativa, sem sugerir automação, eletrônica ou instrumentos de precisão industrial. Este documento é um protótipo funcional de UX para orientar a futura implementação no Minecraft; não é uma textura final.

## Direção visual

- **Materiais:** madeira escura como superfície e moldura; placas de latão envelhecido nos cantos, divisórias e controles; papel pergaminho nas anotações; ferro escurecido apenas nos utensílios.
- **Paleta sugerida:** madeira `#493023`, madeira iluminada `#70472D`, sombra `#211B19`, latão `#B08A4B`, latão claro `#D2B879`, pergaminho `#D8C28F`, tinta `#352719`, realce de seleção `#C68A45`.
- **Linguagem:** formas retas e legíveis em pixel art, parafusos e placas simples. Luz quente discreta. Sem ciano luminoso, hologramas, grades de osciloscópio, barras de energia ou acabamento sci-fi.
- **Tipografia:** títulos compactos com aparência de letreiro gravado; instruções e resultados em fonte de jogo legível. Alto contraste entre tinta escura e pergaminho.

## Layout proposto

Canvas de referência: aproximadamente `320 × 240` unidades de GUI, centralizado e escalável. A escala não deve cortar os slots nem o inventário vanilla.

```text
┌──────────────────────────────────────────────────────────────┐
│ [símbolo] QUANTUM INDUSTRIES     ANALISADOR EMPÍRICO MANUAL  │
│                                  ERA 0                        │
├───────────────┬──────────────────────────┬───────────────────┤
│ AMOSTRA       │ ESCOLHER EXPERIMENTO     │ CADERNO           │
│ [slot]        │ [Inspeção]               │ Descobertas       │
│ material      │ [Dureza]                 │ 0/16 análises     │
│               │ [Deformação]             │ [folha/pergaminho]│
│ FERRAMENTA    │ [Densidade]              │                   │
│ [slot]        │                          │ REGISTRO          │
│ utensílio     │ [ilustração do ensaio]   │ observação atual │
├───────────────┴──────────────────────────┴───────────────────┤
│ [REALIZAR EXPERIMENTO]       INVENTÁRIO DO JOGADOR (vanilla)  │
└──────────────────────────────────────────────────────────────┘
```

Hierarquia: cabeçalho identifica máquina e era; coluna esquerda mostra os dois itens físicos; painel central concentra seleção e ação; coluna direita prioriza o registro e o progresso permanente do caderno. Os quatro experimentos são opções textuais claras, com ícone simples opcional. A ilustração central pode trocar ferramentas desenhadas conforme o ensaio, mas não deve parecer leitura numérica de laboratório.

## Conteúdo e comportamento

- **Amostra:** slot aceita materiais definidos pelo sistema de pesquisa. Exibir nome e, quando disponível, distinguir minério bruto de material refinado.
- **Ferramenta:** slot para utensílio requerido pelo experimento selecionado. A interface informa qual ferramenta falta; não habilita a ação até haver combinação válida.
- **Experimentos:** quatro seleções mutuamente exclusivas: Inspeção, Dureza, Deformação e Densidade. Mostrar seleção por moldura de latão/realce quente, não por brilho tecnológico.
- **Realizar experimento:** botão de ação explícita. O resultado só aparece depois da ação válida; a UI não finge que a ferramenta mediu algo antes do teste.
- **Registro do laboratório:** papel com texto curto de observação qualitativa e nome do material/ensaio. Evitar grandezas elétricas ou casas decimais. Não repetir recompensa de conhecimento já registrada.
- **Caderno:** contador `0/16 análises` como progresso de descobertas únicas disponíveis nesta etapa. A descoberta é persistente e pode ser consultada; repetição pode reapresentar a anotação sem conceder progresso duplicado.
- **Inventário:** usar o inventário vanilla na faixa inferior, com coordenadas e slots compatíveis com a área de interação. Não desenhar um inventário fictício dentro do painel.

## Estados da interface

1. **Vazia:** sem amostra; orientar “Coloque uma amostra para começar”. Experimentos e ação ficam indisponíveis.
2. **Amostra inserida:** mostrar material e habilitar apenas ensaios aplicáveis; ferramenta necessária permanece indicada.
3. **Experimento selecionado, ferramenta ausente:** destacar o utensílio exigido e explicar em uma linha o que falta.
4. **Pronta:** amostra, utensílio e experimento compatíveis; botão “Realizar experimento” habilitado.
5. **Ensaio concluído:** apresentar observação no papel, atualizar caderno uma única vez e manter seleção legível. Nenhum valor elétrico aparece.
6. **Descoberta já registrada:** permitir consultar/repetir a observação sem aumentar o contador nem conceder conhecimento duplicado.
7. **Combinação inválida:** explicar incompatibilidade com texto simples; não consumir item nem registrar resultado.

## Experimentos e tom das observações

| Experimento | Utensílio representado | Tipo de registro |
|---|---|---|
| Inspeção | lupa simples | aparência, brilho, cor, textura ou forma visível |
| Dureza | ponta de risco/pequeno ponteiro | comparação qualitativa de resistência ao risco |
| Deformação | martelo pequeno ou morsa manual | comportamento ao impacto/dobra; registrar maleabilidade e ductilidade como conceitos distintos quando aplicável |
| Densidade | balança de pratos e recipiente com água | comparação aproximada por massa/volume ou deslocamento, sem falsa precisão |

As frases são observações de gameplay inspiradas em propriedades conhecidas, não laudos instrumentais. A densidade pode dizer “afundou mais que a amostra de referência”; não deve exibir um número de precisão que a máquina manual não consegue medir.

## Progressão e limites de conhecimento

Na Era 0, os ensaios dão conhecimento empírico: aparência, resistência relativa, deformabilidade e comparação de densidade. Isso pode habilitar pesquisas posteriores de classificação de materiais, conformação, chapas ou fios, conforme as regras do mod. Aprender que cobre é adequado para formar fios não ensina, por si só, que conduz eletricidade nem como construir circuitos.

Condutividade elétrica medida, resistividade precisa, análise química quantitativa e estrutura atômica ficam fora desta máquina e pertencem a equipamentos de eras posteriores. Não mostrar valores de condutividade, resistividade, temperatura precisa ou composição percentual na HUD manual.

## Minério bruto versus lingote refinado

Inspeção de minério bruto pode registrar veios, ganga, cor e textura como características visuais. Ensaios de deformação devem preferir material metálico refinado (por exemplo, lingote); o minério pode ser marcado como inadequado para esse teste ou produzir uma observação sobre fragmentação, segundo a regra de gameplay. A HUD deve deixar essa distinção explícita e nunca apresentar a observação do minério como propriedade medida do metal puro.

## Acessibilidade e legibilidade

- Não comunicar seleção apenas pela cor: combinar cor, contorno e rótulo.
- Usar textos curtos, sem depender de emoji para identificar item ou ação.
- Reservar espaço para nomes traduzidos maiores e tooltip de ferramenta/ensaio.
- Preservar foco e navegação por teclado/controle nas quatro opções e no botão.
- Manter as notas legíveis em resoluções baixas e com a escala de GUI do Minecraft.

## Fora do protótipo

Este documento não especifica bloco, modelo 3D, receitas, menu Java, persistência de inventário ou textura final. A máquina física será desenhada depois que o design do bloco for definido.
