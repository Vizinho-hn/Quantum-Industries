# Transporte industrial

| Rede | Blocos | Recurso | Armazenamento e transferência |
| --- | --- | --- | --- |
| Itens | Transportador de Itens de Vidro e Cobre | Pilhas de itens | Um pacote por trecho. A extração consulta o destino e o pacote pode retornar ao inventário de origem. |
| Líquidos | Transportadores de Líquidos de Cobre ou Vidro | Líquidos NeoForge | A bomba peristáltica move até 50 mB por operação. Trechos ascendentes exigem pressurização hidráulica. |
| Gases | Transportador de Gases de Cobre | Vapor de água | Até 200 uV e 400 kPa por trecho. Até 20 uV/t na entrada e na saída de cada trecho. |
| Energia | Transportadores de Energia de Cobre Nu ou Isolado | FE | Capacidade, limite de fluxo, perdas e gargalos seguem o QES. |

A Chave de Configuração ajusta cada face entre transferência bidirecional, Puxar, Empurrar e Nenhum. Em um trecho pressurizado, a diferença de pressão e as permissões das duas faces determinam a transferência. O modo Puxar permite entrada, mas não cria pressão. Use os Óculos para conferir as faces e o fluxo.

## Central a vapor

1. Alimente a caldeira com água por uma entrada líquida e com carvão ou carvão vegetal por uma entrada de itens. Conecte o duto de vapor à frente da caldeira.
2. Ligue o duto às entradas traseiras dos motores. Use ramificações se houver mais de um consumidor.
3. Use o eixo direito do motor para transmitir rotação ao QKS. O motor consome 10 uV por tick com geração de torque, entregando até 24 UM. A caldeira consome 10 mB de água para produzir 10 uV por tick.
4. Para gerar FE, acople um alternador com excitação e conecte sua saída ao acumulador com um transportador de energia. A excitação inicial exige uma fonte elétrica.
5. Se o motor parar, confira água, combustível, permissões de face, pressão e autorização. Uma caldeira fornece vapor para um motor em regime nominal. Dois motores só sustentam a potência máxima com produção adicional.

A pressão é um valor de jogo calculado a partir da quantidade relativa à capacidade e da temperatura em Kelvin. A referência é 400 kPa em um duto cheio a 100 °C. A caldeira fornece uma diferença de pressão na saída. O vapor desta versão mantém 100 °C no transporte. O modelo não simula condensação, trocas de calor entre dutos ou explosões. Quando o destino para de aceitar vapor, a pressão se equilibra e a produção para quando a caldeira enche. Quebrar um duto libera o vapor armazenado.

O vapor das caldeiras antigas continua no mesmo campo do save. Ligações diretas da saída frontal com o motor adjacente continuam disponíveis. O novo duto salva separadamente o tipo de gás, a quantidade e os modos por face. As redes existentes preservam seus IDs e suas capacidades.

O modelo editável do duto fica em `design/mechanical/gas_pipe.bbmodel`.
