# Mesa de Pesquisa — primeiro protótipo

## O que existe
- Um bloco registrável: `quantumindustries:research_table`.
- Receita de crafting apenas com itens vanilla.
- Aparência temporária de bloco de ferro, **não é a arte aprovada**.
- Interação de pesquisa: segure um ingrediente em cada mão e clique com o botão direito na mesa.
- O servidor confere o par, consome uma unidade de cada ingrediente (exceto criativo) e informa o resultado no chat.

## Pares de itens
- Papel + carvão: Fundamentos.
- Ferro + carvão: Metalurgia.
- Cobre + redstone: Condutividade.
- Quartzo + fragmento de ametista: Cristalografia.

## Limitações assumidas
- A HUD aprovada ainda **não está implementada**; a interface é temporariamente o chat.
- Pesquisas **não são persistidas**, nem possuem árvore, tempo, pré-requisitos ou desbloqueios.
- Não existe block entity, inventário interno nem menu personalizado.
- Não há novos materiais, apenas a Mesa de Pesquisa.
- Este código ainda não passou por compilação/execução com Java 25 e o MDK real.
- Os PNG e arquivos .bbmodel aprovados não foram encontrados no repositório, somente a documentação do design.

## Teste planejado
1. Instalar JDK 25 e conferir `java -version`.
2. Usar Gradle Wrapper do MDK oficial 26.1 ModDevGradle.
3. Executar `./gradlew runClient`.
4. Obter `/give @s quantumindustries:research_table`.
5. Colocar papel na mão principal e carvão na secundária; clicar na mesa.
6. Verificar que os itens são consumidos apenas no servidor.
7. Testar as outras três combinações e uma combinação inválida.

## Próxima etapa
Integrar modelo/texturas originais, construir menu e tela da HUD aprovada, implementar progresso individual persistente e validação de pré-requisitos.
