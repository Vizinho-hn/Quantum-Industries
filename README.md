# Quantum Industries

**Um mod de Minecraft focado em ciência, engenharia e indústria avançada.**

Quantum Industries pretende levar o jogador de processos industriais básicos até química, ciência dos materiais, engenharia nuclear, computação quântica e tecnologias dimensionais. A progressão é guiada por investigação: observar, analisar, experimentar, documentar e transformar conhecimento em tecnologia.

> **Status:** fase de planejamento e fundação. A implementação do mod e a configuração técnica do projeto ainda precisam ser iniciadas e validadas.

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

## Primeiro protótipo jogável

O primeiro ciclo vertical será deliberadamente pequeno:

1. O jogador obtém uma amostra de cobre.
2. Analisa a amostra em um Analisador.
3. Descobre uma propriedade, como condutividade elétrica.
4. Registra a descoberta em seu banco de dados científico.
5. Desenvolve a pesquisa necessária para fabricar condutores.
6. Desbloqueia a receita de cabo de cobre.

Esse ciclo deve funcionar de ponta a ponta antes de expandirmos a quantidade de máquinas e materiais.

## Sistema de conhecimento

O projeto distingue três conceitos:

- **Descoberta:** conhecimento obtido por observação, análise ou experimento.
- **Pesquisa:** desenvolvimento de uma aplicação ou tecnologia que utiliza conhecimentos e recursos.
- **Desbloqueio:** autorização individual para fabricar ou utilizar uma tecnologia.

O conhecimento será individual e persistente por jogador. Receber um relatório científico de outro jogador pode transmitir conhecimento, mas não deve concluir automaticamente pesquisas práticas ou conceder qualquer tecnologia sem as condições definidas pelo jogo.

## Multiplayer

- Dados de pesquisa pertencem a cada jogador.
- O servidor é a autoridade para validar descobertas, pesquisas e desbloqueios.
- Máquinas podem ser compartilhadas, mas uma operação tecnológica deve respeitar a autorização do jogador que a inicia ou conclui.
- Relatórios científicos serão o mecanismo inicial de compartilhamento de conhecimento.

## Dimensões e energia

Overworld, Nether, End e futuras dimensões poderão apresentar recursos e fenômenos diferentes. A ideia é que máquinas de análise e coleta reconheçam o ambiente em que operam, em vez de tratar toda atmosfera, fluido ou fonte de energia como equivalente.

Tecnologias dimensionais — como extração de energia de End Crystals sem consumi-los — são conceitos de gameplay ficcional e deverão ser equilibradas como sistemas próprios do mod.

## Tecnologia e desenvolvimento

- **Minecraft:** 26.1 (alvo planejado; confirmar compatibilidade no início da implementação).
- **Loader:** NeoForge (alvo planejado).
- **Linguagem:** Java.
- **Ambiente de desenvolvimento:** VS Code e Gradle.
- **Repositório:** [Vizinho-hn/Quantum-Industries](https://github.com/Vizinho-hn/Quantum-Industries).

A versão do Java, do NeoForge MDK e do Gradle Wrapper deve ser definida a partir da documentação e do MDK oficial compatível com a versão de Minecraft escolhida. Não fixe versões manualmente antes dessa validação.

## Documentação

- [Roadmap](ROADMAP.md)
- [Contribuição e fluxo Git](CONTRIBUTING.md)
- [Arquitetura inicial](docs/ARCHITECTURE.md)
- [Sistema de pesquisa](docs/RESEARCH_SYSTEM.md)

## Estado atual

✅ **Projeto Gradle/NeoForge configurado e compilando** (Minecraft 26.1.2, NeoForge 26.1.2.125, Java 21).
Estrutura base implementada: mod principal (`br.com.quantumindustries.QuantumIndustries`), `neoforge.mods.toml` processado, JAR gerado.

Próximos passos: implementar o sistema de pesquisa (Analysis → Discovery → Research → Unlock) e o primeiro ciclo jogável (amostra de cobre → Analisador → condutividade → cabo de cobre).

## Como rodar o projeto

```bash
# Clonar e entrar
git clone https://github.com/Vizinho-hn/Quantum-Industries.git
cd Quantum-Industries

# Compilar (baixa dependências na primeira vez)
./gradlew build

# Rodar cliente de desenvolvimento
./gradlew runClient

# Rodar servidor de desenvolvimento
./gradlew runServer
```

**Requisitos:** Java 21+ (JDK), internet para baixar dependências Gradle/NeoForge na primeira execução.

## Licença

A licença ainda não foi definida. Até que os autores decidam, não presuma que o código ou os assets estejam liberados para redistribuição.
