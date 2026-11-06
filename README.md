# Quantum Industries

**Um mod de Minecraft focado em ciência, engenharia e indústria avançada.**

Quantum Industries pretende levar o jogador de processos industriais básicos até química, ciência dos materiais, engenharia nuclear, computação quântica e tecnologias dimensionais. A progressão é guiada por investigação: observar, analisar, experimentar, documentar e transformar conhecimento em tecnologia.

> **Status:** protótipo jogável em desenvolvimento. A Era I tem 13 pesquisas e termina no Dínamo Manual. A Era II conta com Indução Eletromagnética, Acumulador Galvânico (2.400 FE) e Prensa Eletromecânica.

## Visão do projeto

O objetivo não é apenas adicionar máquinas cada vez mais caras. O jogador deve descobrir como o mundo funciona e usar esse conhecimento para criar novas soluções industriais.

Pilares:
- **Pesquisa científica:** analisar materiais, formular hipóteses e realizar experimentos.
- **Progressão baseada em conhecimento:** descobertas e pesquisas liberam aplicações concretas.
- **Indústria em camadas:** processamento mecânico, energia, química, materiais, nuclear, quântica e dimensional.
- **Multiplayer com progresso individual:** cada jogador mantém seu próprio conhecimento, mesmo quando compartilha máquinas e instalações.
- **Minecraft como laboratório:** dimensões, criaturas, blocos e fenômenos do jogo inspiram sistemas próprios do mod.

## Progressão planejada

1. **Fundação:** infraestrutura do mod e sistema de pesquisa.
2. **Indústria inicial:** processamento de recursos, geração e armazenamento de energia.
3. **Química e materiais:** reações, separação, síntese e propriedades dos materiais.
4. **Engenharia atômica e nuclear:** análise de composição, isótopos, fissão e fusão.
5. **Computação avançada:** automação, redes de controle, computação quântica e IA.
6. **Tecnologia dimensional:** exploração e conversão de fenômenos energéticos de diferentes dimensões.

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

- Os Analisadores Científicos Tier 1 e Tier 2 dão acesso à árvore de pesquisa,
  à Base de Conhecimento, às Receitas Desbloqueadas e aos Relatórios. Os
  ícones na árvore identificam os itens que cada pesquisa libera.
- As receitas aparecem no caderno e no livro da bancada de trabalho do
  Minecraft conforme o progresso individual. No livro do Minecraft, clicar
  na receita posiciona os ingredientes disponíveis. Desligue o filtro de
  fabricação para ver também as receitas para as quais faltam materiais.
- Produza as chapas na Bancada de Conformação com o martelo. Estrutura
  Eletromecânica libera a estrutura e o Núcleo Mecânico Primitivo, ambos em
  3D. Projeto do Dínamo libera o dínamo e dá acesso à Era II. Indução
  Eletromagnética é um estudo da Era II.
- Fabrique a manivela separadamente, instale-a e gire. O dínamo armazena até
  240 FE, gera 12 FE por giro, aceita um giro a cada 10 ticks e transfere energia
  para blocos elétricos adjacentes. Na Era II, o **Acumulador Galvânico** estoca
  até 2.400 FE e alimenta a **Prensa Eletromecânica**, que prensa lingotes em
  chapas consumindo 60 FE por ciclo sem gastar martelos. Os Óculos de Inspeção
  Energética exibem a carga e o estado ao mirar em qualquer bloco elétrico.

[Roteiro de sobrevivência e detalhes do dínamo](docs/MANUAL_DYNAMO.md)

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

Em multiplayer, o servidor valida pesquisas, operações e receitas para cada
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
