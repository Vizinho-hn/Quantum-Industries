# Interface da Mesa de Pesquisa

**Status:** implementada como interface de bancada no menu da Mesa de Pesquisa.

## Layout

- Cabeçalho Quantum Industries com o nome da bancada.
- Coluna esquerda com entradas para amostra e sonda e o slot de saída.
- Monitor central com rede cristalina CFC animada e traço de osciloscópio.
- Painel direito com resistividade, condutividade, estado e progresso da análise.
- Inventário do jogador em uma faixa inferior.

A interface é desenhada proceduralmente em código, com paleta escura industrial,
bordas pixeladas e acentos em ciano e cobre. A textura de fundo não é necessária
para renderizar o menu.

## Estados

- **Em espera:** aguardando amostra e sonda compatíveis.
- **Ativa:** monitor e progresso indicam a análise em andamento.
- **Concluída:** os dados de pesquisa aparecem no slot de saída.

Os valores de resistividade e condutividade exibidos atualmente representam a
amostra de cobre suportada pela bancada.
