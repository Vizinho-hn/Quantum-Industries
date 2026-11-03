# Sistema de pesquisa científica

## Objetivo

Transformar exploração e experimentação em progressão tecnológica. O jogador deve descobrir propriedades e relações, e então aplicar esse conhecimento para desenvolver processos e máquinas.

## Vocabulário

- **Amostra:** item ou material submetido a análise.
- **Propriedade:** característica observável ou mensurável (por exemplo, condutividade, dureza ou estabilidade térmica).
- **Descoberta:** fato que o jogador aprendeu.
- **Experimento:** procedimento com condições, entradas e resultados.
- **Pesquisa:** projeto que combina descobertas, recursos e atividades para desenvolver uma aplicação.
- **Desbloqueio:** permissão resultante de uma pesquisa concluída.
- **Relatório científico:** documento que registra e transmite conhecimento.

## Como o jogador aprende

O sistema deve misturar:
- **Descobertas por análise:** uma análise válida revela propriedades previstas para aquela amostra.
- **Descobertas experimentais:** algumas relações exigem que o jogador monte e execute um experimento.
- **Pesquisa aplicada:** transformar conhecimento em processo industrial pode exigir recursos, máquinas e etapas práticas.

Uma análise não deve revelar automaticamente tudo que existe sobre um material. O nível de detalhe pode depender do equipamento, método e condições utilizados.

## Exemplo de primeiro ciclo: cobre

1. Obter minério ou amostra de cobre.
2. Analisar no Analisador.
3. Descobrir condutividade elétrica.
4. Registrar a descoberta no banco científico individual.
5. Iniciar pesquisa de condutores com conhecimento e recursos adequados.
6. Concluir a pesquisa e liberar a fabricação de cabo de cobre.

## Dados por jogador

Cada jogador possui seu próprio conjunto de descobertas, pesquisas em andamento, pesquisas concluídas e desbloqueios. Esses dados devem persistir entre sessões.

No multiplayer, Hian não recebe automaticamente uma descoberta feita por outro jogador. O compartilhamento ocorre por relatórios ou mecanismos futuros, e a assimilação deve ser uma ação explícita.

## Relatórios científicos

Tipos planejados:
- **Observacional:** propriedades, medições e resultados de análise.
- **Experimental:** procedimento, condições, materiais e resultados.
- **Tecnológico:** aplicação, projeto e instruções de processo.

Metadados possíveis: autor, colaboradores, amostras, descobertas, método, confiabilidade, data e tecnologias relacionadas.

Receber um relatório não equivale a dominar automaticamente uma técnica de fabricação. Um relatório pode transmitir teoria e observações, enquanto a aplicação ainda exige pesquisa, recursos ou prática.

## Regras de integridade

- O servidor valida toda descoberta e conclusão.
- A interface não concede conhecimento por conta própria.
- Relatórios precisam ser validados e ter origem identificável.
- A confiabilidade e possíveis falsificações são ideias futuras; não fazem parte do primeiro protótipo.

## Escopo do primeiro protótipo

Implementar somente:
- uma amostra (cobre);
- uma propriedade (condutividade elétrica);
- um equipamento de análise;
- uma descoberta persistente por jogador;
- uma pesquisa aplicada;
- um desbloqueio de receita.

Só ampliar o sistema depois de testar esse ciclo em single-player e com dois jogadores.
