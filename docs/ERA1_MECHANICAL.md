# Oficina mecânica da Era I

## Auditoria e escopo

Base: `main` em `680a9b3` (Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25).
Branch: `feat/era1-mechanical-foundation`. Não integrar automaticamente.

Foram inspecionados os catálogos ResearchCatalog, AssayCatalog,
EmpiricalMaterialCatalog, AssayProfile, WorldResearchData, FoundationRecipe,
ResearchCraftingAccess, ModRecipeBook, a árvore, o caderno, as receitas
desbloqueadas, os registros, a conformação e a infraestrutura de máquinas.
O eixo e a manivela atuais são itens, sem rede mecânica. Os identificadores
e os conhecimentos persistidos serão preservados. A descoberta específica
de prensa mecânica do ferro já sustenta Mecânica Fundamental e não será
substituída por uma exigência de quatro ensaios completos.

O PR #22 foi consultado: permanece aberto e em rascunho. Trefiladora,
recozimento, cabos elétricos e pneumática não são implementados aqui.
Há sobreposição de arquivos em ModItems, ModBlocks, FoundationRecipe,
ResearchCatalog, ResearchTreeView, ResearchTableScreen e traduções. A futura
integração deve combinar registros/entradas, sem substituir catálogos inteiros.
Sistemas pneumáticos e FE não devem ser conectados implicitamente à rotação.
A manivela existente pode ser reutilizada, mas a Trefiladora do PR #22 deverá
adotar explicitamente o contrato mecânico se vier a receber eixos.

## Entrega e validação

Em implementação: serraria, eixos e roda-d'água. Os resultados de testes,
as regras operacionais e o roteiro survival serão registrados nesta página
antes da entrega. Uma prévia renderizada não equivale a um teste survival.
