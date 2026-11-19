# Quantum Industries — Direção artística geral

**Status:** política visual transversal. As especificações detalhadas já existentes em [ERA1_VISUAL_STYLE_GUIDE.md](ERA1_VISUAL_STYLE_GUIDE.md) e [ERA2_VISUAL_STYLE_GUIDE.md](ERA2_VISUAL_STYLE_GUIDE.md) continuam sendo a referência de materiais, paletas, proporções e contratos de assets para essas eras. Este documento não substitui os guias específicos.

## 1. Visão: Minecraft industrial detalhado

O Quantum Industries deve parecer um mod **nativo do universo Minecraft**, mas com acabamento e sofisticação comparáveis aos bons mods tecnológicos. **Não** confundir coerência com o Minecraft com simplificação obrigatória: uma máquina pode ter múltiplas peças, profundidade, animação e detalhes mecânicos, desde que sua leitura continue clara.

Princípios:
- **Silhueta primeiro:** a função e o tipo de máquina devem ser reconhecíveis à distância de jogo, antes dos microdetalhes.
- **Detalhe com propósito:** roletes, eixos, mancais, correias, engrenagens, válvulas e suportes devem explicar o funcionamento; evitar ornamentos sem função aparente.
- **Materiais reconhecíveis:** madeira, ferro, bronze, cobre, vidro, cerâmica etc. precisam manter contraste e identidade próprios.
- **Pixel art deliberada:** bordas nítidas, blocos de cor controlados e sombras em degraus; evitar gradientes pintados, borrões, ruído uniforme e suavização que faça o asset parecer de outro jogo.
- **Geometria voxel:** respeitar proporções, interfaces e escala do Minecraft; complexidade útil é bem-vinda, superfícies orgânicas excessivamente arredondadas ou detalhes ilegíveis não.
- **Consistência de família:** máquinas de uma mesma era compartilham materiais, tipos de encaixe, densidade de detalhe e acabamento, sem precisar ser idênticas.

**Referências externas não são especificações de implementação.** Veja [MOD_REFERENCES.md](MOD_REFERENCES.md) para fontes e lições. Não copiar assets de terceiros.

## 2. As cinco eras do projeto

Os nomes oficiais seguem [ROADMAP.md](../ROADMAP.md) e [README.md](../README.md). A era de um asset deve ser confirmada por seu estudo, desbloqueio, implementação e documentação; não inferir apenas pela aparência ou por usar FE.

| Era | Nome oficial | Direção visual |
|---|---|---|
| **I** | Engenharia Mecânica e Ciência Empírica | Oficina artesanal, madeira escura envelhecida, ferro forjado, bronze e cobre; mecânica legível, montagem manual e instrumentação empírica. Usar o [guia oficial da Era I](ERA1_VISUAL_STYLE_GUIDE.md). |
| **II** | Revolução Elétrica | Industrialização eletromecânica, ferro fundido, aço rebitado, cobre industrial, latão, porcelana, isolantes e materiais térmicos/químicos. Usar o [guia oficial da Era II](ERA2_VISUAL_STYLE_GUIDE.md). |
| **III** | Ondas Eletromagnéticas e Tecnologia Moderna | **Direção ainda a detalhar.** A evolução poderá priorizar instrumentação, eletrônica e controle, mediante guia aprovado. Não inventar paleta oficial. |
| **IV** | Engenharia Quântica | **Direção ainda a detalhar.** Priorizar clareza funcional e continuidade da identidade do mod; não impor ficção científica genérica antes de aprovação. |
| **V** | Inteligência Artificial e Automação Autônoma | **Direção ainda a detalhar.** Interfaces e automação devem ser reconhecíveis e consistentes com os sistemas anteriores; sem padrão cromático presumido. |

**Atenção às transições:** a Era I já possui componentes eletromecânicos, inclusive o Dínamo Manual; isso não autoriza dar acabamento futurista a toda máquina da Era I. A **Trefiladora** (`wire_drawing_bench`) é catalogada na **Era II** no guia existente, mesmo aceitando acionamento manual ou FE. A era é uma classificação de progressão, não uma regra simplista sobre a fonte de energia.

As eras agrupam pesquisas e não são bloqueios globais de progressão; não alterar essa regra de gameplay para acomodar estética.

## 3. Texturas e materiais

1. Reutilizar, quando compatível, as paletas, rampas de cinco valores e atlas já definidos nos guias das Eras I e II.
2. Manter **pixels nítidos** e densidade de texel coerente entre faces e peças que ficam lado a lado. Evitar antialiasing ou escalonamento suave em pixel art.
3. **Não impor 16×16 a todas as texturas.** Há sprites de 16×16 e 32×32 e atlas de 128×128 documentados no projeto. A grade geométrica de 16 unidades por bloco **não** é a resolução obrigatória da textura.
4. Aumento de resolução só se justifica quando melhora leitura, UV ou acabamento; não usar mais pixels para introduzir ruído.
5. Fazer o material ser reconhecível sem depender de brilho emissivo. Reservar efeitos luminosos para tecnologias e funções que realmente os exijam.
6. Quando um atlas é compartilhado, auditar **todos os consumidores e UVs**, inclusive arquivos `.bbmodel`, modelos JSON e `.geo.json`, antes de alterar a imagem.

## 4. Geometria, animação e renderização

- Inspecionar o caminho de renderização real antes de escolher ferramenta: **modelo JSON vanilla** quando basta geometria estática; **GeckoLib** quando a implementação necessita de bones/animação. Não converter indiscriminadamente.
- Separar peças móveis por função (rotor, manivela, carro, engrenagem) e preservar nomes de bones, pivôs, eixos de rotação, orientação, visibilidade condicional e animações existentes.
- Conferir conexões modulares, alturas de eixo, portas de entrada/saída, orientação de bloco e limites de colisão/seleção. Respeitar contratos específicos dos guias e testes automatizados.
- Evitar peças flutuantes, faces coplanares com *z-fighting*, UVs fora do atlas e microgeometria ilegível.
- Não copiar proporções de uma imagem conceitual cegamente: a imagem pode conter perspectiva impossível, ligações desconexas ou peças que não cabem na grade do Minecraft.

## 5. Pipeline obrigatório para um asset

1. **Identificar:** localizar registro, estudo/era, modelos, texturas, atlas, renderizador, animações e arquivos de origem. Verificar se há mudanças paralelas no mesmo asset.
2. **Diagnosticar:** capturar ou examinar estado atual; separar defeitos de geometria, textura, UV, renderização, iluminação e escala. Não refazer o que já está bom.
3. **Projetar:** descrever silhueta, materiais, mecanismo, peças móveis, vistas frontal/lateral/superior e critérios de aceite. Conceitos gerados por imagem são **referências**, não instruções literais de cubos.
4. **Prototipar:** usar Blockbench/MCP quando disponível, preservar arquivos-fonte e contratos de exportação; modificar **um asset piloto** antes de qualquer padronização em lote.
5. **Integrar:** validar IDs, blockstates, modelos de item, traduções, recipes, recursos, bones, UVs e funcionamento relevante.
6. **Testar:** usar verificações proporcionais ao risco e o QA in-game descrito em [AGENTS.md](../AGENTS.md); comparar a máquina com blocos vanilla e com máquinas da mesma era, preferencialmente com imagens antes/depois.
7. **Aprovar:** só expandir o padrão após revisão visual. Não afirmar que houve teste in-game se apenas o Blockbench foi aberto.

## 6. Checklist de aprovação

- [ ] A máquina é reconhecível em escala normal de jogo, sem depender de close-up?
- [ ] O acabamento é elaborado quando necessário, mas as peças têm propósito visual ou mecânico?
- [ ] Madeira, metal, vidro e outros materiais são distinguíveis por textura, valor e geometria?
- [ ] O asset parece pertencer ao Minecraft e à **era correta** do Quantum Industries?
- [ ] A densidade de pixels e as UVs são coerentes com assets vizinhos?
- [ ] Conexões, pivôs, bones, animações e orientação funcionam sem peças flutuantes ou *z-fighting*?
- [ ] Modelo no mundo, inventário/GUI e iluminação foram avaliados quando aplicável?
- [ ] Os testes realmente executados e suas limitações foram registrados?

## 7. Estratégia de adoção

Começar por **um protótipo representativo**. Para a oficina mecânica da Era I, `mechanical_shaft_block` e sua conexão com roda-d'água/serraria são referências já documentadas; avaliar a qualidade atual antes de remodelar. Para a **Era II**, `wire_drawing_bench` é um candidato a piloto de acabamento industrial. Não promover alterações em lote sem aprovação e não substituir guias existentes por preferências de um único conceito visual.
