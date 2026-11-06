# Integração científica da Era I e transição para a Era II

A Mesa de Pesquisa Tier 1 é fabricável com recursos vanilla. Observação dos
Materiais é a única pesquisa inicial e libera a receita do Analisador de
Materiais, que mantém o ID `empirical_assay_bench`.

## Evidências utilizadas

| Pesquisa | Evidência científica exigida |
|---|---|
| Observação dos Materiais | Nenhuma |
| Maleabilidade | Prensa de parafuso de Cobre ou de Ouro (`AnyOf`) |
| Condutividade | Inspeção com lupa de Cobre |
| Fabricação de Fios | Prensa de parafuso de Cobre |
| Conformação Manual | Conhecimento anterior |
| Chapas Metálicas | Prensa de parafuso de Ferro |
| Isolamento | Inspeção com lupa de Carvão ou de Quartzo (`AnyOf`) |
| Mecânica Fundamental | Prensa de parafuso de Ferro |
| Transmissão Mecânica | Conhecimento anterior |
| Magnetismo | Inspeção com lupa de Ferro |
| Bobinagem | Prensa de parafuso de Cobre |
| Estrutura Eletromecânica | Aquecimento com fole de Ferro ou de Cobre (`AnyOf`) |
| Projeto do Dínamo | Conhecimentos anteriores |
| Indução Eletromagnética (Era II) | Reação em frasco de Redstone e de Cobre ou Lápis-lazúli (`AnyOf`) |

As descobertas persistem por jogador e são consultadas sem consumo. Uma única
descoberta mecânica do cobre atende Maleabilidade, Fabricação de Fios e Bobinagem.
Os quatro métodos da bancada (inspeção, prensa mecânica, aquecimento e reação)
participam diretamente da árvore de pesquisa.

## Validação e interface

O servidor valida conhecimentos anteriores e todas as evidências em
`WorldResearchData.canLearn`/`learn`. O menu só debita materiais após essa
validação e mantém o rollback transacional existente. Relatórios exigem as
mesmas evidências e só são consumidos quando a pesquisa é aprendida.

O catálogo permite requisitos específicos, listas cumulativas e alternativas (`AnyOf`).
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
O histórico `galvanic` permanece preservado e não equivale a `reaction`.

`/quantum:desbloquear_pesquisas` concede estudos com privilégio administrativo.
`/quantum:clear_conhecimento` limpa pesquisas e mantém o perfil experimental.
