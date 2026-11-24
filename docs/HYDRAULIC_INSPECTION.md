# Inspeção hidráulica 3D

Com os Óculos de Inspeção equipados, mire o bloco central de qualquer roda e segure Shift. O overlay de rede permanece disponível. Setas simples azuis mostram o fluxo efetivamente amostrado; setas duplas mostram a contribuição de torque. Verde favorece a velocidade angular atual, vermelho a contraria. Em repouso, ambas usam âmbar e a referência positiva do eixo mundial (+X/+Z). A orientação física independe da câmera; apenas a pequena separação visual da face acompanha o lado observado para evitar ocultação pela superfície da água.

## Fonte e unidades

`HydraulicTorque.Contribution` é imutável: deslocamento inteiro da célula, vetor de fluxo usado no servidor e parcela assinada do **acionamento hidráulico local QKS**. `effective()` distingue fluxo sem torque. As rodas retêm a lista já usada no cálculo aprovado, e só calculam a lista detalhada quando inspecionadas, uma vez por amostragem. Não há amostragem de água no cliente.

A primitiva mantém ganho 18 e saturação +/-27. As parcelas compartilham o mesmo fator de saturação. No limiar inativo, somente o pequeno resíduo da banda morta é distribuído proporcionalmente ao módulo das parcelas para preservar soma zero e os torques opostos no equilíbrio. Nas rodas de aro, cada contribuição usa o clamp de braço/raio e capacidade/(8*raio), exatamente como no cálculo vigente. A soma é o torque local passado ao QKS, incluindo cancelamento; não é o torque líquido da rede, que também inclui cargas, perdas e atrito. Nenhum valor é anunciado em N·m.

## Protocolo e custo

`RequestHydraulicPayload` / `HydraulicInspectionPayload` são separados do snapshot básico, que mantém seu codec. O servidor valida identidade, óculos, Shift, dimensão, chunk já carregado, distância de até dez blocos, roda real e alvo atingido pelo olhar atual. O limite é uma consulta a cada cinco ticks por jogador; o cliente também usa cinco ticks. A resposta é enviada apenas ao solicitante.

Cada resposta tem até 16 amostras (4 na primitiva, 8 na média e 16 na grande), com três bytes de deslocamento e quatro doubles por amostra: 35 bytes/amostra. O total de dados do payload padrão é inferior a 700 bytes, cerca de 2,8 kB/s no limite de quatro consultas por segundo, além do envelope do protocolo. O tráfego de inspeção básico e de topologia preexistente não está incluído nessa conta. Nenhum chunk é forçado pelo recurso.

O cliente exige dimensão, alvo e geração de consulta correspondentes; descarta IDs atrasados, futuros e respostas de gerações anteriores. O prazo local é de 12 ticks. Ao soltar Shift, retirar os óculos, trocar de alvo/mundo, abrir uma tela ou destruir a roda, o overlay deixa de ser elegível imediatamente. Água removida é refletida na consulta seguinte (normalmente até cinco ticks, mais latência), mesmo durante a inércia do volante.

## Verificação

- `tests/hydraulic-inspection.init.gradle`: servidor dedicado com água e registries reais; ambos os eixos e três tamanhos; soma, saturação, cancelamento, posição, torque e inércia, codec e pedidos inválidos. Duas identidades `FakePlayer` verificam isolamento e limites por jogador. Isso não equivale a QA visual com dois clientes humanos.
- `tests/hydraulic-inspection-preview.init.gradle`: cliente isolado, seis cenários, respostas reais do protocolo e capturas PNG; valida descarte de respostas atrasadas/de outra dimensão/alvo, remoção dos óculos, mudança de alvo, quebra da roda e transição real para o Nether.
- Classes do harness ficam fora do JAR de produção. Os logs de carregamento do servidor permitem auditar ausência de classes client-only.

As fixtures mantêm estados reais de água controlados para separar a amostragem da erosão natural dos canais. As capturas não são ilustrações e não demonstram comportamento de rios naturais em todas as topologias.

## Evidências visuais

As seis capturas ficam em `docs/images/hydraulic-inspection/`, com fluxo único, dois favoráveis, fluxos opostos, cancelamento, aro 5x5 e volante em desaceleração. Revisão principal em PT-BR, 1600x1000, escala de GUI 2. Outros tamanhos e canais naturais permanecem sujeitos a QA de gameplay.

### Resultado executado nesta entrega

- Servidor dedicado: **96 verificações aprovadas**, incluindo identidade independente de dois jogadores e auditoria sem classes client-only.
- Regressão QKS: **786 verificações na fase write e 30 após reinício**, preservando movimento, correias e FE; isolamento client-only aprovado nas duas JVMs.
- Suíte de contratos: **58 testes Python aprovados**.
- Cliente integrado: **43 verificações aprovadas**, seis PNGs decodificados e revisados visualmente; a troca real de dimensão para o Nether também passou.
- Capturas: [fluxo único](images/hydraulic-inspection/01-single-flow.png), [favoráveis](images/hydraulic-inspection/02-supporting-flows.png), [opostos](images/hydraulic-inspection/03-opposing-flows.png), [cancelamento](images/hydraulic-inspection/04-cancellation.png), [aro grande](images/hydraulic-inspection/05-large-perimeter.png), [volante em inércia](images/hydraulic-inspection/06-flywheel-coasting.png).

Comandos reproduzíveis (JDK 25 e dependências instaladas):

```bash
bash tools/verify-hydraulic-inspection.sh
./gradlew --max-workers=2 --init-script tests/hydraulic-inspection-preview.init.gradle runClient --console=plain
```

O preview espera uma cópia de um mundo de QA em `build/hydraulic-inspection-preview/saves/HydraulicQA`; o jogador e os blocos da fixture são preparados automaticamente. Não use um mundo pessoal nessa pasta.
