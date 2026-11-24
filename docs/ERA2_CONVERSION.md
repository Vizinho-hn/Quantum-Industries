# Conversão energética industrial — Era II

## Contratos

| Equipamento | Entrada | Saída | Pesquisa |
|---|---|---|---|
| Alternador eletromagnético | QKS, 2–12 UM; 24 FE iniciais | até 8 FE/t líquidos; reserva 1.200 FE | Geração eletromagnética industrial |
| Motor elétrico | 24 FE/t | 18 UM | Conformação eletromecânica |
| Caldeira | Água, carvão/carvão vegetal | 10 unidades de vapor/t; reserva 2.000 | Hidrostática + recozimento térmico |
| Motor a vapor | 10 unidades de vapor/t | 24 UM | Hidrostática + recozimento térmico |
| Gerador termoelétrico | Fontes quente e fria em faces opostas | até 6 FE/t; reserva 480 FE | Fundição elétrica de ligas |

## Implementação e limites

O rotor usa o QKS original. Motor elétrico e motor a vapor reservam o recurso para o tick seguinte; a reserva é transitória e não pode ser recriada por carregamento de mundo. O gerador só converte se há carga autorizada, fonte e velocidade. As taxas não são grandezas SI calibradas. Um ciclo motor-gerador perde FE. Cabos e adjacências dividem o limite de saída. O vapor é uma unidade abstrata de jogo em tanque interno, transferida diretamente ao motor no bloco adjacente à saída frontal da caldeira. Água usa a capacidade de fluido NeoForge existente. Não há vapor fluido registrado, explosão de pressão nem simulação de termodinâmica real.

O modelo termoelétrico seleciona o melhor par oposto nos três eixos, sem somar geração. Preserva o calor acumulado por face, a proteção acima de 250 °C, o consumo de refrigerantes finitos e a saída frontal. Saves antigos migram o refrigerante da face superior.

## Critérios de aceite

- Operador autorizado e pesquisas preservadas; sem pesquisa, sem produção.
- Geração nula com fonte removida, eixo incompatível, bateria vazia, água/combustível ausente ou temperatura sem diferença.
- Reservas e combustível persistidos, sem duplicação e sem geração após coasting ou sobrecarga.
- Portas corretas em cada orientação; transferência por cabos e adjacência; não misturar FE, UM e vapor.
- Testar servidor dedicado e cliente; revisar texturas, colisões e conexões no Blockbench/Minecraft. Valores sujeitos a balanceamento survival.

### Alternador e dínamo

Curvas de jogo, não grandezas SI: dínamo por eixo cresce de 0 a 1,5 FE/t entre 0–12 RPM; manivela preservada. Alternador bruto cresce de 0 a 9 FE/t entre 12–50 RPM, mantém 9 até 60 RPM e decresce até zero a 90 RPM. Desconta 1 FE/t de excitação. Campo inicial exige 24 FE externos; a reserva de campo decai quando não sustentada. O calor cresce acima de 65 RPM e desarma a 100, com retorno a 40. O buffer e a carga dinâmica usam o sistema FE/QKS existente. CA e frequência são conceituais; não há rede CA separada. IDs existentes preservados.

A caldeira é destino de ItemRouting: consulta de capacidade/combustível sem mutação, rejeição da face frontal, inserção apenas de carvão ou carvão vegetal após autorização. A entrada de água é uma capacidade de inserção exclusiva. Dutos em Puxar podem captar água de fonte renovável ao nível ou acima; a bomba pode captar uma fonte renovável e pressurizar dutos para elevação. O vapor não entra na rede de água.
