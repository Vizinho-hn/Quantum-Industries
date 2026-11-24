# Analisador de Rede Elétrica MK1 — Era II

## Uso
Pesquisa Controle de Carga (load_protection). Coloque o bloco diretamente em uma face de um cabo;
o conector traseiro aponta para o cabo selecionado. Clique direito para abrir o painel.
O aparelho não recebe, produz, armazena ou encaminha FE. A conexão visual da sonda não é uma porta de energia.
O alcance é uma esfera de raio 16 blocos centrada no analisador, até 128 nós (cabos e equipamentos).

## Contrato dos dados
- A busca em largura percorre somente condutores carregados, respeitando as portas, modos e interruptores.
  Equipamentos são terminais e não pontes entre redes. A reserva soma cada terminal uma única vez.
- CableNetwork registra a extração confirmada uma única vez na primeira célula de cada rota;
  entrega e perdas uma única vez na última. Cabos intermediários contam apenas tráfego local.
  Não se deve somar o tráfego dos cabos para obter produção total.
- As duas janelas por cabo são transitórias; o analisador usa o último segundo de simulação concluído,
  com média em FE/t (20 ticks). Topologia é a atual, podendo mudar durante o intervalo.
  Em uma rede fechada e estável: injeção = entrega + perdas. Fronteiras do raio, chunks ou limite podem
  tornar o escopo parcial. Um dispositivo que carrega e descarrega pode aparecer nos dois fluxos;
  isto não equivale a produção primária ou consumo final. Transferências diretamente adjacentes sem cabo
  não são medidas.
- Pico de fluxo é o maior fluxo agregado por tick observado no cabo, segundo a semântica atual
  do CableNetwork (FE entregues, perdas contabilizadas separadamente). Vermelho = atingiu o limite.
- Histórico circular de 60 amostras enquanto o bloco está carregado; não persistido.
  O mapa é projeção XZ dos nós detectados, não um diagrama elétrico completo; andares sobrepostos.
- O servidor autoriza abertura/continuidade do menu e envia apenas dados. Sem carregamento forçado,
  sem inventar volts/ampères antes de existir modelo físico.

## Evolução
MK2 (Era III): publicação de snapshots autenticados por antenas, alcance e canais; sem transmissão de FE.
MK3 (Era IV): comandos remotos autorizados, limites de distribuição, regras e estados de segurança.
Era V: otimização inteligente. Nenhuma dessas etapas altera a rede nesta entrega.

## Verificação
Executar ./gradlew verifyCableTelemetryWindow check build e bash tools/verify-cables.sh com JDK 25.
Abrir painel em cliente com rede ramificada, abrir/fechar interruptor, desconectar a sonda, atravessar
o limite de alcance, descarregar chunk, validar localização e aparência. O modelo é um piloto visual.
