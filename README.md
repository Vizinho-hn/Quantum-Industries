# Quantum Industries

**Um mod de Minecraft focado em ciência, engenharia e indústria avançada.**

Quantum Industries pretende levar o jogador de processos industriais básicos até química, ciência dos materiais, engenharia nuclear, computação quântica e tecnologias dimensionais. A progressão é guiada por investigação: observar, analisar, experimentar, documentar e transformar conhecimento em tecnologia.

> **Status:** protótipo jogável em desenvolvimento. A Era I tem 14 pesquisas, inclui a oficina mecânica e termina no Dínamo Manual. A Era II conta com Indução Eletromagnética, Acumulador Galvânico (2.400 FE) e Prensa Eletromecânica.

## Visão do projeto

As cinco eras seguem a progressão: **Engenharia Mecânica e Ciência Empírica**,
**Revolução Elétrica**, **Ondas Eletromagnéticas e Tecnologia Moderna**,
**Engenharia Quântica** e **Inteligência Artificial e Automação Autônoma**.
O Dínamo Manual encerra o **arco de geração elétrica** da Era I, mas **não** é uma barreira geral para consultar ou avançar em todos os ramos da Era II. As eras agrupam pesquisas por tema; **o servidor só exige os pré-requisitos e evidências de cada estudo**. É possível desenvolver hidráulica, pneumática, recozimento ou trefilação em paralelo, se houver os conhecimentos específicos. O dínamo continua sendo necessário para pesquisar **Indução Eletromagnética**, que sustenta as máquinas elétricas.

### Pesquisas independentes e melhorias de rendimento

- **Ramos fundamentais:** habilitam uma técnica realmente necessária à máquina pretendida, não a passagem global de era.
- **Ramos opcionais de utilidade:** podem oferecer mais produção, menos desperdício, transporte de itens/fluidos ou conforto (ex.: roda-d'água, recozimento, trefilação e, futuramente, moagem de trigo e panificação).
- **Ramos de eletricidade:** preservam a cadeia própria de geração → indução → acumulação → proteção; continuam exigindo seus conhecimentos e evidências.
- **Pesquisa não concluída:** um estudo de outra era pode ficar visível e bloqueado por dependência específica, sem que a era inteira esteja inacessível.
- **Compatibilidade:** nada foi apagado do progresso dos jogadores e nenhuma receita bypassa o desbloqueio validado no servidor.


Nesta branch, a oficina mecânica traz serraria manual, transmissão por eixos
e roda-d'água, independente de FE. A integração na main depende da revisão do PR.
[Auditoria, funcionamento e validação](docs/ERA1_MECHANICAL.md).

O objetivo não é apenas adicionar máquinas cada vez mais caras. O jogador deve descobrir como o mundo funciona e usar esse conhecimento para criar novas soluções industriais.

Pilares:
- **Pesquisa científica:** analisar materiais, formular hipóteses e realizar experimentos.
- **Progressão baseada em conhecimento:** descobertas e pesquisas liberam aplicações concretas.
- **Indústria em camadas:** processamento mecânico, energia, química, materiais, nuclear, quântica e dimensional.
- **Multiplayer com progresso individual:** cada jogador mantém seu próprio conhecimento, mesmo quando compartilha máquinas e instalações.
- **Minecraft como laboratório:** dimensões, criaturas, blocos e fenômenos do jogo inspiram sistemas próprios do mod.

## Progressão planejada

1. **Era I — Engenharia Mecânica e Ciência Empírica:** materiais, metalurgia, mecanismos e aproveitamento hidráulico. O dínamo encerra a era.
2. **Era II — Revolução Elétrica:** geração, distribuição, armazenamento, motores e máquinas elétricas.
3. **Era III — Ondas Eletromagnéticas e Tecnologia Moderna:** eletrônica, sinais, sensores, comunicação e controle programável.
4. **Era IV — Engenharia Quântica:** materiais avançados, precisão e processamento sofisticado.
5. **Era V — Inteligência Artificial e Automação Autônoma:** planejamento e coordenação inteligente das instalações.

## 🖼️ Galeria Visual de Itens e Equipamentos

O Quantum Industries conta com um catálogo visual completo com ilustrações em pixel art de alta definição e modelos dos componentes e máquinas industriais.

> 👉 **[Consulte o Catálogo Completo com Todos os Itens e Máquinas Detalhados (docs/ITEMS.md)](docs/ITEMS.md)**

### Destaques das Eras I & II

| Instrumentos Científicos | Metalurgia & Conformação | Oficina & Transmissão | Geração & Eletricidade | Hidráulica & Fluidos |
| :---: | :---: | :---: | :---: | :---: |
| <img src="docs/images/items/magnifying_glass.png" width="48" height="48" /><br><sub>**Lupa de Vidro**</sub> | <img src="docs/images/items/manual_hammer.png" width="48" height="48" /><br><sub>**Martelo Manual**</sub> | <img src="docs/images/blocks/mechanical_sawmill.png" width="48" height="48" /><br><sub>**Serraria Mecânica**</sub> | <img src="docs/images/blocks/manual_dynamo.png" width="48" height="48" /><br><sub>**Dínamo Manual**</sub> | <img src="docs/images/blocks/settling_vat.png" width="48" height="48" /><br><sub>**Cuba Decantação**</sub> |
| <img src="docs/images/items/assay_flask.png" width="48" height="48" /><br><sub>**Frasco de Ensaio**</sub> | <img src="docs/images/items/iron_sheet.png" width="48" height="48" /><br><sub>**Chapa de Ferro**</sub> | <img src="docs/images/blocks/primitive_water_wheel.png" width="48" height="48" /><br><sub>**Roda-d'Água**</sub> | <img src="docs/images/blocks/galvanic_accumulator.png" width="48" height="48" /><br><sub>**Acumulador (2.4k)**</sub> | <img src="docs/images/blocks/peristaltic_pump.png" width="48" height="48" /><br><sub>**Bomba Peristáltica**</sub> |
| <img src="docs/images/items/flame_test_kit.png" width="48" height="48" /><br><sub>**Kit de Chama**</sub> | <img src="docs/images/items/copper_coil.png" width="48" height="48" /><br><sub>**Bobina de Cobre**</sub> | <img src="docs/images/blocks/forming_bench.png" width="48" height="48" /><br><sub>**Bancada Conformação**</sub> | <img src="docs/images/blocks/electromechanical_press.png" width="48" height="48" /><br><sub>**Prensa (FE)**</sub> | <img src="docs/images/items/galvanic_electrolyte.png" width="48" height="48" /><br><sub>**Eletrólito**</sub> |
| <img src="docs/images/items/energy_inspection_goggles.png" width="48" height="48" /><br><sub>**Óculos Inspeção**</sub> | <img src="docs/images/items/primitive_mechanical_core.png" width="48" height="48" /><br><sub>**Núcleo Mecânico**</sub> | <img src="docs/images/blocks/empirical_assay_bench.png" width="48" height="48" /><br><sub>**Analisador Materiais**</sub> | <img src="docs/images/blocks/wire_drawing_bench.png" width="48" height="48" /><br><sub>**Trefiladora**</sub> | <img src="docs/images/items/pigment_sediment.png" width="48" height="48" /><br><sub>**Sedimento**</sub> |

---

## Protótipo jogável atual

Comece pela Mesa de Pesquisa Tier 1 e conclua **Observação dos Materiais**.
Essa é a entrada da Era I e libera o **Analisador de Materiais**, que reutiliza
o bloco da Bancada de Ensaio Empírico.

O analisador permite investigar dez materiais e quatro métodos, totalizando
**40 descobertas individuais**. Inspeção usa a lupa, aquecimento usa o fole
manual e carvão, reação usa o frasco de ensaio e o método mecânico usa a
prensa. Cada ensaio dura 10 segundos. O equipamento continua trabalhando
quando a interface é fechada. As ferramentas e os resultados pertencem ao
sistema persistente da bancada e do jogador.

Os quatro métodos empíricos (inspeção, aquecimento, reação e prensa mecânica)
fornecem evidências científicas diretas e alternativas (`AnyOf`) para destravar
as pesquisas da Era I e a transição para a Era II. Descobertas persistem no
perfil individual e são reutilizadas sem consumo, enquanto cada pesquisa exige
seus materiais e conhecimentos anteriores.

[Regras científicas e compatibilidade](docs/SCIENTIFIC_RESEARCH.md).
[Materiais, métodos e instrumentos](docs/design/bancada-de-ensaio/README.md).

### Visual da Mesa de Pesquisa Tier 1

A Tier 1 tem bancada artesanal própria em madeira, ferro e cobre, com caderno
aberto, balança mecânica, lupa, bandeja de amostras e gaveta decorativa.
O modelo ocupa um único bloco, acompanha sua orientação e aparece também
no inventário. A Tier 2 mantém seu visual tecnológico.

[Modelo editável, textura e vistas](design/research_table_tier_1.md).

### Pesquisa e fabricação

- **Oficina mecânica:** Mecânica Fundamental e Transmissão Mecânica levam à
  Serraria Mecânica. Estude também Chapas Metálicas para fabricá-la. Uma tora
  rende seis tábuas da espécie correta em cinco segundos de movimento.
  Instale a manivela existente para começar sem eletricidade. Aproveitamento
  Hidráulico libera a roda-d'água, que transmite movimento por eixos alinhados.
  Crafts e processos aparecem na mesma aba Receitas Desbloqueadas.
  [Montagem, controles, limite de carga e testes](docs/ERA1_MECHANICAL.md).

- Os Analisadores Científicos Tier 1 e Tier 2 dão acesso à árvore de pesquisa,
  à Base de Conhecimento, às Receitas Desbloqueadas e aos Relatórios. Os
  ícones na árvore identificam os itens que cada pesquisa libera.
- As receitas aparecem no caderno e no livro da bancada de trabalho do
  Minecraft conforme o progresso individual. No livro do Minecraft, clicar
  na receita posiciona os ingredientes disponíveis. Desligue o filtro de
  fabricação para ver também as receitas para as quais faltam materiais.
- Produza as chapas na Bancada de Conformação com o martelo. Estrutura
  Eletromecânica libera a estrutura e o Núcleo Mecânico Primitivo, ambos em
  3D. Projeto do Dínamo libera o dínamo e habilita a linha elétrica baseada em Indução. Indução
  Eletromagnética é um estudo da Era II.
- Fabrique a manivela separadamente, instale-a e gire. O dínamo armazena até
  240 FE, gera 12 FE por giro, aceita um giro a cada 10 ticks e transfere energia
  para blocos elétricos adjacentes. Na Era II, o **Acumulador Galvânico** estoca
  até 2.400 FE e alimenta a **Prensa Eletromecânica**, que prensa lingotes em
  chapas consumindo 60 FE por ciclo sem gastar martelos. Os Óculos de Inspeção
  Energética exibem a carga e o estado ao mirar em qualquer bloco elétrico.

[Roteiro de sobrevivência e detalhes do dínamo](docs/MANUAL_DYNAMO.md)

### Distribuição elétrica inicial da Era II

Barramentos de Cobre Nu e Conduítes Isolados levam FE do dínamo e do acumulador até consumidores carregados por rede. O barramento admite até 32 FE/t e dissipa 1 FE por quatro segmentos nus no trajeto; o conduíte admite 64 FE/t, sem perda. A rede percorre somente chunks carregados, não cria FE e informa o fluxo nos Óculos de Inspeção. Os barramentos nus sob carga podem dar choque leve em entidades sem botas.

### Trefiladora de bancada

Após pesquisar **Trefilação de Fios** (com observações mecânicas de cobre e ouro), o jogador constrói a trefiladora. Uma chapa de cobre ou ouro rende 4 fios do respectivo metal. O equipamento funciona manualmente com a Manivela na mão ou alimentado por FE, consumindo 60 FE por operação automatizada. A metalurgia permite obter chapas de ouro nos mesmos processos usados com ferro e cobre.

### Recozimento térmico

A Forja de Recozimento recebe carvão e um lingote de ferro/cobre, e utiliza o Fole Manual para sustentar o calor. Após 60 ticks quentes, o material fica trabalhável por até 600 ticks do mundo. Se ainda quente, a Bancada de Conformação produz chapa com um golpe e a Prensa completa o ciclo em 10 ticks (30 FE), mantendo o rendimento de 1 chapa/lingote. O calor não cria itens nem energia elétrica.

### Instrumentação da Era II

O **Transdutor Piezoelétrico** transforma impactos discretos em pulsos de 40 FE, com intervalo mínimo por entidade e proteção contra geração AFK imóvel. Ele mantém um buffer interno de 200 FE e pode carregar máquinas adjacentes ou pelos cabos.

O **Relé de Carga** acompanha um acumulador galvânico diretamente adjacente. Abaixo de 10% ele interrompe o fornecimento a máquinas; acima de 80% emite sinal de redstone de intensidade 15. O relé armazena até 96 FE para encaminhamento, sem geração própria.

### Pneumática, hidráulica e decantação (protótipo Era II)

A pesquisa **Pressão Pneumática** libera os tubos de vidro/cobre, o ejetor e o classificador gravitacional. Coloque o ejetor apontado do baú/fornalha/prensa para um tubo; sopre com **Fole Manual** ou use **Manivela** (1 item, Shift = até 1 pilha) ou forneça **10 FE por pulso** (modo automático com 1 item). Os tubos guardam pacotes individuais por segmento e exibem os itens viajando dentro do vidro. Os tubos descarregam em baús, fornalhas ou na entrada da prensa. Um classificador pode encaminhar itens em linha reta ou para a direita, com peneira alternável por clique com barras de ferro; as categorias são discretas (pós vs lingotes), não densidade contínua. Os modelos 3D são protótipos voxel com texturas vanilla e podem ser refinados no Blockbench.

A pesquisa **Hidrostática e Vasos Comunicantes** libera os dutos de cobre e vidro e a **Cuba de Decantação**. A rede transporta fluidos pela API NeoForge 26.1 `ResourceHandler<FluidResource>` sem usar `IFluidHandler` legado. Os dutos (250 mB de volume interno) escoam lateralmente/para baixo; a **Bomba Peristáltica** liberada por pesquisa própria transfere até **50 mB/t** usando **2 FE por tick** ou uma manivela manual, e pressuriza trechos subsequentes para vencer desníveis.

A cuba tem 8.000 mB de capacidade. Com **1.000 mB de água + 1 lápis-lazúli + 1 frasco de vidro**, após decantação controlada produz **1 Concentrado de Eletrólito Galvânico** (item engarrafado) e **1 Sedimento de Pigmento**. Este é um processo químico inicial de abstração de gameplay: ainda **não** registra um fluido novo de eletrólito nem modifica o craft antigo do Acumulador. O concentrado foi criado para uma receita ou reprocessamento futuro, preservando a progressão já salva.

## Imagens do protótipo

![Bancada com amostra e ferramentas armazenadas](docs/design/bancada-de-ensaio/images/bancada-ferro.png)

![HUD da bancada de ensaio](docs/design/bancada-de-ensaio/images/hud-inicial.png)

Mais imagens, receitas e regras estão no
[guia da Bancada de Ensaio](docs/design/bancada-de-ensaio/README.md).
Cada componente tem sua documentação no [índice de componentes](docs/design/README.md).

## Sistema de conhecimento

O projeto distingue três conceitos:

- **Descoberta:** conhecimento obtido por observação, análise ou experimento.
- **Pesquisa:** desenvolvimento de uma aplicação ou tecnologia que utiliza conhecimentos e recursos.
- **Desbloqueio:** autorização individual para fabricar ou utilizar uma tecnologia.

O conhecimento é individual e persistente. Na aba **Relatório de pesquisa**,
guarde tinta preta, pena e papel no analisador. Selecione uma pesquisa concluída
e clique em **Escrever relatório** para gastar uma unidade de cada material.
Os insumos guardados permanecem no bloco. Outro jogador pode usar o relatório
com o botão direito para aprender a pesquisa se já tiver os conhecimentos
anteriores e as evidências científicas exigidas.

Para compartilhar as quatro observações de um material, conclua os ensaios 4/4, segure uma pena na mão principal e uma amostra do material na mão secundária, leve um papel no inventário e clique com o botão direito no Analisador Científico. Isso gera uma Ficha de Observação, que outro jogador pode consumir para adquirir somente aquelas quatro descobertas, sem desbloquear pesquisas automaticamente.\n\nEm multiplayer, o servidor valida pesquisas, operações e receitas para cada
jogador, inclusive quando a bancada é compartilhada.

### Comandos administrativos

Com permissão de operador, o jogador pode usar
`/quantum:desbloquear_pesquisas` para liberar as pesquisas e
`/quantum:clear_conhecimento` para limpar o próprio progresso de pesquisa.
O livro de receitas acompanha o novo estado. O desbloqueio administrativo
concede as pesquisas diretamente. A limpeza de pesquisas mantém as descobertas
experimentais registradas no perfil do jogador.

## Dimensões e energia

Overworld, Nether, End e futuras dimensões poderão apresentar recursos e fenômenos diferentes. A ideia é que máquinas de análise e coleta reconheçam o ambiente em que operam, em vez de tratar toda atmosfera, fluido ou fonte de energia como equivalente.

Tecnologias dimensionais — como extração de energia de End Crystals sem consumi-los — são conceitos de gameplay ficcional e deverão ser equilibradas como sistemas próprios do mod.

## Tecnologia e desenvolvimento

- **Minecraft:** 26.1.2.
- **Loader:** NeoForge 26.1.2.125.
- **Linguagem:** Java 25.
- **Build:** Gradle Wrapper 9.2.1.
- **Ambiente de desenvolvimento:** VS Code e Gradle.
- **Repositório:** [Vizinho-hn/Quantum-Industries](https://github.com/Vizinho-hn/Quantum-Industries).

As versões usadas pelo projeto estão fixadas em `gradle.properties`, `build.gradle` e no Gradle Wrapper.

## Documentação

- [Roadmap](ROADMAP.md)
- [Contribuição e fluxo Git](CONTRIBUTING.md)
- [Arquitetura inicial](docs/ARCHITECTURE.md)
- [Sistema de pesquisa](docs/RESEARCH_SYSTEM.md)
- [Bancada de ensaio: materiais, métodos e caderno](docs/design/bancada-de-ensaio/README.md)

## Como rodar o projeto

```bash
# Clonar e entrar
git clone https://github.com/Vizinho-hn/Quantum-Industries.git
cd Quantum-Industries

# Compilar (baixa dependências na primeira vez)
./gradlew build

# Abrir o jogo no Linux
./Jogar.sh

# Cliente de desenvolvimento via Gradle
./gradlew runClient

# Rodar servidor de desenvolvimento
./gradlew runServer
```

**Requisitos:** JDK 25 e internet para baixar dependências Gradle/NeoForge na primeira execução.

## Como verificar

O CI executa a migração de pesquisas com `./gradlew verifyLegacyResearch`, usando
as dependências do Minecraft/NeoForge configuradas no projeto.

```bash
# Contratos e recursos
python3 -m unittest discover -s tests -v

# Integração da Bancada de Ensaio em um mundo isolado
./tools/verify-assay.sh

# Integração da Era I, receitas, relatórios, comandos e dínamo
./gradlew --init-script tests/dynamo.init.gradle runServer

# Compilação normal
./gradlew build

# Preview automatizado da interface (Linux com Xvfb e cache Gradle preenchido)
./tools/preview-assay.sh
```

O preview usa um mundo separado em `build/assay-preview` e captura oito estados
da interface e dos itens exibidos na bancada. O limite de 30 FPS e as distâncias reduzidas pertencem a esse
procedimento de verificação. Para jogar manualmente, use `./gradlew runClient`.
As classes de verificação e preview ficam fora do JAR normal. Para preparar o
servidor de teste do dínamo, consulte [Dínamo Manual V1](docs/MANUAL_DYNAMO.md).

## Licença

**Todos os direitos reservados (© 2026 Hian e Guilherme).** A concepção e a idealização do Quantum Industries são atribuídas a Hian e Guilherme. O acesso público ao repositório não concede licença para copiar, modificar, redistribuir ou explorar os conteúdos originais do projeto. Consulte o arquivo [LICENSE](LICENSE) para os termos completos, respeitados os direitos e licenças de terceiros.

## Abrir o jogo pelo atalho

- **Linux:** abra **Quantum Industries** no menu de aplicativos ou use o atalho
  na área de trabalho deste computador. No terminal, execute `./Jogar.sh`.
- **Windows:** dê dois cliques em `Jogar.bat` na raiz do repositório. O
  `Jogar.exe` existente também continua disponível.

É necessário ter JDK 25. O lançador usa o Gradle Wrapper e abre o cliente normal,
sem entrar no mundo de preview, automatizar cliques ou impor limite de FPS.
Na primeira execução, o Gradle pode baixar dependências. No Linux, o script usa
`/mnt/dados/cache/quantum-gradle` quando esse volume existe e evita abrir outra
instância do cliente do projeto. `./Jogar.sh --check` verifica os pré-requisitos
sem abrir o Minecraft. Feche e reabra o jogo para carregar alterações no código.
