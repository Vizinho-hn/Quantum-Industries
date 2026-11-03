# Arquitetura inicial

Este documento descreve responsabilidades pretendidas, não uma estrutura Java já implementada. Os nomes e APIs concretos serão ajustados ao MDK NeoForge compatível com Minecraft 26.1.

## Princípios

1. **Servidor como autoridade:** descobertas, pesquisas e desbloqueios são validados no servidor.
2. **Conhecimento por jogador:** progresso científico não é global por padrão.
3. **Dados antes de classes específicas:** materiais, propriedades e descobertas devem ser definidos de forma extensível, preferencialmente orientada a dados quando apropriado.
4. **Separação de responsabilidades:** análise, descoberta, pesquisa, desbloqueio e apresentação são sistemas distintos.
5. **Protótipo vertical:** priorizar um ciclo pequeno e funcional antes de ampliar o catálogo.

## Módulos conceituais

- `material`: definições de materiais, propriedades, unidades e valores.
- `analysis`: regras e resultados de análise de amostras.
- `research`: descobertas, requisitos, experimentos e progresso.
- `playerdata`: conhecimento persistente individual.
- `unlock`: validação de acesso a receitas, máquinas e tecnologias.
- `report`: documentos científicos que podem transmitir conhecimento.
- `machine`: blocos e lógica de máquinas.
- `energy`: geração, armazenamento, transferência e consumo.
- `network`: sincronização cliente-servidor e protocolos necessários.
- `client`: telas, indicadores e apresentação visual.

## Fluxo de análise

1. Jogador insere uma amostra no Analisador.
2. Servidor valida item, estado da máquina e condições.
3. Sistema de análise calcula quais observações são possíveis.
4. Resultado é aplicado ao perfil científico daquele jogador.
5. Cliente recebe atualização para exibir a descoberta.

## Persistência e multiplayer

A implementação deve usar o mecanismo de dados persistentes suportado pela versão escolhida do NeoForge. A API exata (por exemplo, attachments ou mecanismo equivalente) precisa ser confirmada na documentação da versão 26.1 antes da implementação.

Nunca confiar em uma mensagem enviada pelo cliente dizendo que uma pesquisa foi concluída. O servidor verifica pré-requisitos, custos, estado da máquina e permissões, e então grava o resultado.

## Receitas e desbloqueios

A camada de desbloqueio deve ser consultada pela lógica que permite fabricar ou operar tecnologias. Ocultar uma receita na interface não é proteção suficiente: a validação real precisa ocorrer no servidor.

## Organização Java sugerida

Quando o MDK estiver criado, uma estrutura possível é:

```text
br/com/quantumindustries/
  QuantumIndustries.java
  material/
  analysis/
  research/
  playerdata/
  unlock/
  report/
  machine/
  energy/
  network/
  client/
```

Não criar todos esses pacotes vazios agora. Adicionar cada módulo quando houver uma primeira implementação concreta.
