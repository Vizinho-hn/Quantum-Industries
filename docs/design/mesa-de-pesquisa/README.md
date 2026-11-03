# Mesa de Pesquisa — Quantum Industries

**Status:** conceito visual aprovado; implementação pendente.

A Mesa de Pesquisa é uma máquina de um único bloco Minecraft (1×1×1 de ocupação), com modelo 3D personalizado e detalhes salientes no topo.

## Direção visual aprovada

- Base industrial em grafite / metal escuro, reforços nos cantos e madeira na bancada.
- Iluminação azul-ciano e símbolo de átomo na superfície e na frente.
- Livro aberto, pequeno monitor vertical e pilha de livros como geometria adicional.
- Referência de ferramenta de modelagem: Blockbench (.bbmodel).
- Textura de trabalho: PNG 128×128, a ajustar conforme o modelo.

## Arquivos previstos

- `assets/mesas-de-pesquisa/quantum_research_table.bbmodel` — fonte editável no Blockbench.
- `assets/mesas-de-pesquisa/quantum_research_table_texture.png` — atlas do modelo.
- `docs/design/mesa-de-pesquisa/README.md` — especificação do bloco.

**Observação:** os arquivos binários de arte devem ser enviados sem alteração para os caminhos indicados. Este documento não significa que os arquivos já tenham sido incorporados ao jogo.

## Próximos passos

1. Versionar modelo e textura originais.
2. Abrir e validar a geometria e UVs no Blockbench.
3. Exportar para o formato de renderização escolhido para o mod.
4. Implementar o bloco, block entity e vínculo com a interface de pesquisa.
