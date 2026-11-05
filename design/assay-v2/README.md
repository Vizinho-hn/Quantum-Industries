# Analisador Empírico — Era 0

Bancada integrada de dois blocos: inspeção, aquecimento, reação e prensa mecânica de parafuso. Madeira, ferro, couro, vidro e material refratário; sem latão.

## Fontes editáveis

- `empirical_workshop.bbmodel`: modelo criado e revisado no Blockbench, com oito grupos e quatro sequências de dez segundos. `python3 tools/export-assay-model.py` exporta a geometria compartilhada e separa a estrutura estática das partes móveis.
- `empirical_workshop.aseprite`, `magnifying_glass.aseprite`, `manual_bellows.aseprite`, `assay_flask.aseprite`: fontes de textura e ícones no LibreSprite.
- `analisador-era0-v2.mcgui.json`: projeto nativo do Minecraft GUI Maker. Os slots estão posicionados no projeto, mas o acabamento escuro é desenhado pelo jogo. Textos, controles e cena 3D são dinâmicos.
- `tools/export-assay-vfx.mjs /caminho/para/pixelfx`: exporta vinte quadros determinísticos da chama. O Node e o PixelFX não são dependências do mod. Licenças e atribuições estão em `licenses/`; nenhuma fonte da galeria é distribuída.

## Execução

`AssayMotion` define a linha temporal comum ao mundo e à HUD. As animações do Blockbench são prévias editáveis dessa linha temporal. Preparação 0–30 ticks, execução 30–160, observação 160–185 e recolhimento 185–200. `AssayApparatus` usa a mesma geometria para ambos os renderizadores; a HUD possui uma câmera própria.

Uma unidade do material é reservada para a pesquisa completa. Cada ensaio representa uma pequena porção. A prensa já pertence à bancada; inspeção requer lupa, aquecimento requer fole ou kit de chama (pederneira aceita por compatibilidade) e reação requer frasco. Ferramentas não são consumidas. Reagentes e brasas são abstrações desta versão, não combustível ou química quantitativa. As observações são qualitativas e estilizadas para gameplay.

O servidor é autoritativo. Fechar o menu ou sair do alcance não cancela o ensaio. Um chunk descarregado pausa o trabalho; o estado NBT retoma quando carregado. Resultados concluídos são enfileirados em SavedData por UUID e entregues de forma idempotente. Quebrar uma bancada ativa cancela esse ensaio e devolve somente os itens físicos não consumidos; uma reserva antiga pode ser retomada pelo autor.

IDs do bloco foram preservados. Descobertas antigas usam chaves estáveis: `mechanical` e `heating` continuam válidas; `galvanic` permanece no histórico, sem conceder `reaction` ou `inspection`. Reservas antigas continuam aceitas. Uma pesquisa ativa bloqueia a bancada para outros autores.

## Verificação

- `./gradlew build`: compilação e verificação de pesquisa legada.
- `python3 tests/test_assay_resources.py`: traduções, receitas da Era 0, separação da geometria e dimensões do VFX.
- `tools/verify-assay.sh`: servidor dedicado em mundo descartável, cobrindo quatro ensaios × dez materiais, fechamento da HUD, autoria, migração, persistência e orientações.
- `tools/preview-assay.sh`: cliente em mundo descartável; nove capturas reais da HUD e do mundo em `build/assay-preview/screenshots/v2-*.png`.

### Validação desta entrega (2026-11-04)

Build de produção aprovado; JAR conferido sem classes do harness. Servidor dedicado: 570 verificações aprovadas, incluindo contato entre cabeçote e amostra e deformação retida no recolhimento. Quatro testes de recursos aprovados. Nove capturas reais de cobre foram geradas e inspecionadas; os outros nove materiais foram exercitados no servidor. Auditoria final do Blockbench: 101 cuboides, oito grupos, quatro animações e nenhuma face sobreposta reportada.

### Polimento: ferramentas instaláveis e amostra real

Lupa, suporte óptico, fole e conjunto do frasco só são renderizados com os itens correspondentes nos slots. A bandeja permanece parada. O material em movimento usa o ItemStack original (incluindo minério bruto e modelos de resource packs), tanto na HUD quanto no mundo. O fole agora separa base, couro e tampa articulada. A captura `v3-empty-tools.png` mostra a bancada antes da instalação das ferramentas.
