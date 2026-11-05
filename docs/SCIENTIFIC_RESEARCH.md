# Integração científica da Era I

A Mesa de Pesquisa Tier 1 é fabricável com recursos vanilla. Observação dos
Materiais é a única pesquisa inicial e libera a receita do Analisador de
Materiais, que mantém o ID empirical_assay_bench.

## Evidências utilizadas

| Pesquisa | Evidência adicional |
|---|---|
| Observação dos Materiais | Nenhuma |
| Maleabilidade | Ensaio mecânico do cobre |
| Condutividade | Inspeção do cobre |
| Fabricação de Fios | Ensaio mecânico do cobre |
| Conformação Manual | Conhecimento anterior |
| Chapas Metálicas | Ensaio mecânico do ferro |
| Isolamento | Conhecimento anterior |
| Mecânica Fundamental | Ensaio mecânico do ferro |
| Transmissão Mecânica | Conhecimento anterior |
| Magnetismo | Inspeção do ferro |
| Bobinagem | Ensaio mecânico do cobre |
| Estrutura Eletromecânica | Conhecimentos anteriores |
| Projeto do Dínamo | Conhecimentos anteriores |

Inspeção representa familiarização com as amostras. A pesquisa de Condutividade
estuda a condução elétrica, e Magnetismo utiliza a bússola já exigida pelo estudo.
As descrições da inspeção permanecem qualitativas.

As descobertas persistem por jogador e são consultadas sem consumo. Uma única
descoberta mecânica do cobre atende Maleabilidade, Fabricação de Fios e Bobinagem.
A Era I usa quatro evidências entre as 40 descobertas disponíveis.

## Validação e interface

O servidor valida conhecimentos anteriores e todas as evidências em
WorldResearchData.canLearn/learn. O menu só debita materiais após essa
validação e mantém o rollback transacional existente. Relatórios exigem as
mesmas evidências e só são consumidos quando a pesquisa é aprendida.

O catálogo permite requisitos específicos, listas cumulativas e alternativas.
Na árvore, o estado de falta de evidência se distingue da falta de materiais.
Os detalhes mostram o ensaio e a amostra. Passe o mouse sobre o painel de
detalhes para consultar os requisitos completos.

O analisador aparece nas Receitas Desbloqueadas e no livro vanilla após
Observação. O servidor também impede a fabricação manual antes do desbloqueio.

## Saves e administração

IDs e dados persistidos das pesquisas e dos ensaios são preservados.
Pesquisas concluídas anteriormente continuam válidas sem repetir experimentos.
Antes de aprender novos estudos, jogadores de saves antigos também precisam
ter concluído Observação dos Materiais.
O histórico galvanic permanece preservado e não equivale a reaction.

/quantum:desbloquear_pesquisas concede estudos com privilégio administrativo.
/quantum:clear_conhecimento limpa pesquisas e mantém o perfil experimental.

## Verificação reproduzível

1. Inicie um mundo de teste sem pesquisas e fabrique a Mesa Tier 1.
2. Confirme que apenas Observação está disponível e que o analisador ainda
   não pode ser fabricado. Pesquise Observação e confira a receita liberada.
3. Abra Maleabilidade com os custos no inventário. A pesquisa deve aguardar
   o ensaio mecânico do cobre. Realize o ensaio e retorne à mesa.
4. Confira a atualização do requisito, pesquise Maleabilidade e preserve
   a descoberta para as pesquisas posteriores.
5. Inspecione cobre e ferro e faça o ensaio mecânico do ferro. Avance
   até o Projeto do Dínamo e siga o roteiro MANUAL_DYNAMO.md.
6. Com outro jogador, tente compartilhar um relatório sem a evidência.
   O item deve permanecer disponível. Faça o ensaio e tente novamente.
7. Reabra o mundo e confira pesquisas, descobertas e receitas. Repita
   com um save anterior à integração para verificar os desbloqueios existentes.

Os harnesses tests/assay.init.gradle e tests/dynamo.init.gradle executam
em mundos isolados. O segundo também verifica migrações de pesquisa e a
integração científica. Seus resultados ficam em build/*-verification/result.txt.
As classes de teste não entram no JAR normal.
