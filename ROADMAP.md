# Roadmap — Quantum Industries

Este roadmap registra a direção do projeto. Itens são planejados, não promessas de entrega. As fases podem ser ajustadas após protótipos e testes.

## Fase 0 — Fundação técnica
- [ ] Confirmar Minecraft 26.1 e compatibilidade do NeoForge.
- [ ] Selecionar o MDK oficial correspondente e validar JDK/Gradle exigidos.
- [ ] Criar projeto mínimo que compile e abra no cliente de desenvolvimento.
- [ ] Definir package base, convenções de código e estrutura de assets.
- [ ] Configurar execução local e teste em servidor dedicado.
- [ ] Configurar CI para build e verificações básicas.

## Fase 1 — Núcleo de pesquisa
- [ ] Definir modelo de materiais e propriedades.
- [ ] Definir catálogo de descobertas e requisitos de análise.
- [ ] Implementar dados persistentes individuais por jogador.
- [ ] Implementar validação no servidor.
- [ ] Criar protótipo de Analisador.
- [ ] Criar Bancada de Pesquisa e Banco de Dados Científico.
- [ ] Implementar o ciclo cobre → condutividade → pesquisa de condutores → cabo de cobre.
- [ ] Testar persistência após sair e entrar novamente.
- [ ] Testar dois jogadores com progresso independente.

## Fase 2 — Primeira indústria
- [ ] Processamento mecânico básico.
- [ ] Geração e armazenamento de energia.
- [ ] Rede de energia e consumo por máquinas.
- [ ] Máquinas iniciais e receitas.
- [ ] Interface de estado, energia e progresso.

## Fase 3 — Química e materiais
- [ ] Reações e processos químicos.
- [ ] Separação, purificação e análise de substâncias.
- [ ] Propriedades físicas e químicas com unidades e escalas definidas.
- [ ] Síntese e materiais industriais avançados.

## Fase 4 — Engenharia atômica e nuclear
- [ ] Análise de composição dos materiais.
- [ ] Extração e processamento de elementos.
- [ ] Isótopos e separação isotópica.
- [ ] Fissão e sistemas de segurança.
- [ ] Fusão e tecnologias de plasma.
- [ ] Balanceamento de resíduos, calor e riscos.

## Fase 5 — Computação avançada
- [ ] Redes de controle e automação.
- [ ] Computadores industriais e IA hierárquica.
- [ ] Núcleo de computação quântica.
- [ ] Estabilização, entrelaçamento e aplicações ficcionais.

## Fase 6 — Tecnologias dimensionais
- [ ] Caracterizar recursos e fenômenos por dimensão.
- [ ] Sistemas de coleta atmosférica e energética específicos por dimensão.
- [ ] Reator de energia de End Crystal com cristal preservado.
- [ ] Investigar fenômenos do Nether, End e futuras dimensões.
- [ ] Tecnologias de alto nível e riscos dimensionais.

## Critérios para avançar de fase

Uma fase não deve avançar apenas porque os blocos existem. Os sistemas centrais precisam:
- funcionar em single-player e servidor dedicado;
- validar operações no servidor;
- persistir dados corretamente;
- ter receitas, custos e feedback compreensíveis;
- ser testáveis sem depender de comandos administrativos.
