# Conversão energética industrial — Era II

## Contratos

| Equipamento | Entrada | Saída | Pesquisa |
|---|---|---|---|
| Gerador eletromagnético | QKS, 12 UM de carga | até 8 FE/t; reserva 1.200 FE | Indução eletromagnética |
| Motor elétrico | 24 FE/t | 18 UM | Conformação eletromecânica |
| Caldeira | Água, carvão/carvão vegetal | 10 unidades de vapor/t; reserva 2.000 | Hidrostática + recozimento térmico |
| Motor a vapor | 10 unidades de vapor/t | 24 UM | Hidrostática + recozimento térmico |
| Gerador termoelétrico | Fonte quente abaixo e fria acima | 1–3 FE/t; reserva 480 FE | Fundição elétrica de ligas |

## Implementação e limites

O rotor usa o QKS original. Motor elétrico e motor a vapor reservam o recurso para o tick seguinte; a reserva é transitória e não pode ser recriada por carregamento de mundo. O gerador só converte se há carga autorizada, fonte e velocidade. As taxas não são grandezas SI calibradas. Um ciclo motor-gerador perde FE. Cabos e adjacências dividem o limite de saída. O vapor é uma unidade abstrata de jogo em tanque interno, transferida diretamente ao motor no bloco adjacente à saída frontal da caldeira. Água usa a capacidade de fluido NeoForge existente. Não há vapor fluido registrado, explosão de pressão nem simulação de termodinâmica real.

## Critérios de aceite

- Operador autorizado e pesquisas preservadas; sem pesquisa, sem produção.
- Geração nula com fonte removida, eixo incompatível, bateria vazia, água/combustível ausente ou temperatura sem diferença.
- Reservas e combustível persistidos, sem duplicação e sem geração após coasting ou sobrecarga.
- Portas corretas em cada orientação; transferência por cabos e adjacência; não misturar FE, UM e vapor.
- Testar servidor dedicado e cliente; revisar texturas, colisões e conexões no Blockbench/Minecraft. Valores sujeitos a balanceamento survival.
