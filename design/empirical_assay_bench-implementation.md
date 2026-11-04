# Bancada de ensaio empírico — protótipo jogável

Implementação inicial feita diretamente após a entrega do worker não aparecer nas worktrees.

- Bloco: `quantumindustries:empirical_assay_bench` (obter por `/give @s quantumindustries:empirical_assay_bench`).
- Amostra: lingote de cobre. Ferramentas provisórias vanilla: vidro, pederneira e aço, pepita de ferro (peso).
- Abrir bancada, inserir os quatro itens, clicar em Realizar ensaio três vezes.
- Descobertas qualitativas: dúctil, chama verde, condutor galvânico. Cada clique conclui uma etapa; cenas desenhadas proceduralmente são ilustrativas.
- Estado salvo no persistent data do jogador; repetir não concede pontos ou itens.
- Ao fechar, itens da superfície voltam ao jogador. A bancada não guarda itens entre aberturas.
- Visual de madeira/latão/pergaminho e instrumentos manuais. Ausência de telemetria elétrica precisa/estrutura atômica.
- Build: `./gradlew build` passou. Sem testes automatizados existentes; validação visual dentro do jogo pendente.
- Ainda não há receita de fabricação nem catálogo dos outros materiais; este é o ciclo inicial de cobre.
- Textos da tela inicialmente em português; tradução completa e acabamento pixel art permanecem para próxima passagem.

## Limites de interpretação

A deformação por compressão indica maleabilidade; a anotação de ductilidade é uma simplificação de gameplay e precisará de ensaio próprio. A chama verde é um indício estilizado de compostos de cobre, sem provar pureza ou condutividade térmica. A etapa galvânica registra condução qualitativa, não resistividade medida.
