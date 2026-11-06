# Bancada de Ensaio Empírico — versão atual

A bancada permite **10 materiais × 4 métodos = 40 descobertas individuais**:
inspeção, aquecimento, reação e prensa mecânica. Cada método registra uma
observação qualitativa própria, consultável no caderno. Esses ensaios são
simplificações de gameplay, não medições laboratoriais rigorosas.

## Como usar

1. Insira uma amostra aceita na bancada.
2. Instale a lupa para inspeção, o fole manual para aquecimento ou o frasco
   de ensaio para reação. O aquecimento também aceita itens de chama legados,
   incluindo o isqueiro vanilla. A prensa mecânica já faz parte da bancada.
3. Selecione um dos **quatro** métodos e clique em **Realizar ensaio**.
4. Aguarde **10 segundos (200 ticks)**, com o chunk carregado, e consulte o resultado.
5. Complete os quatro métodos para finalizar o ciclo de uma amostra.

O caderno percorre os dez materiais, revelando somente as observações já
concluídas. Exibe **0/4 por material** e **0/40 no catálogo** inicialmente.
Consultar os resultados não consome itens nem concede novas descobertas.

## Amostra e persistência

O primeiro ensaio válido retira exatamente uma unidade do slot e a reserva no
perfil individual. A indicação de reserva na HUD não é um segundo item retirável.
Existe apenas um ciclo aberto por jogador. Fechar a tela, sair do mundo ou usar
outra bancada permite retomar a mesma amostra e os resultados anteriores.

Interromper o teste cancela somente o temporizador. Acesso e ferramentas são
revalidados pelo servidor; um teste interrompido precisa ser iniciado novamente.
A reserva é consumida definitivamente ao completar os quatro métodos únicos.
Ferramentas são reutilizáveis. Os quatro slots pertencem à bancada: fechar a
HUD não devolve ferramentas nem amostras ainda não reservadas ao inventário.
Os itens permanecem após salvar e carregar o mundo, podem ser retirados pela
interface e caem ao quebrar a bancada. Os dois lados do bloco acessam o mesmo
inventário; jogadores que usam a mesma bancada compartilham esses itens.

A lupa, o fole, o frasco e a amostra aparecem sobre a bancada quando seus
slots estão ocupados. A amostra utiliza seu ItemStack real, preservando
a aparência do item inserido, inclusive cobre bruto. O aparato integrado
permanece visível independentemente dos itens de ferramenta.
Quando o primeiro ensaio reserva uma amostra, ela deixa o slot físico e passa a
ser indicada como **Reservada** na HUD. Essa reserva continua individual e não
é um segundo item exposto ou retirável da bancada. Dois jogadores mantêm ciclos e descobertas independentes.

As descobertas usam identificadores estáveis de material e método, separados das
pesquisas tecnológicas. A migração preserva os ensaios antigos de prensa e
aquecimento. O resultado legado `galvanic` permanece no histórico e **não**
é convertido em reação ou inspeção; essas descobertas exigem os novos ensaios.

## Catálogo e ferramentas

Os dez materiais aceitos são: cobre, ferro, ouro, carvão, diamante, quartzo,
redstone, lápis-lazúli, esmeralda e ametista. **Cobre bruto** também é aceito
como variante da amostra de cobre. Cada um possui os mesmos quatro métodos,
com resultados próprios nas traduções do jogo. Não confundir um resultado de
ensaio com uma propriedade científica universal.

- **Inspeção:** usa a lupa (`magnifying_glass`).
- **Aquecimento:** usa o fole manual (`manual_bellows`) ou, por compatibilidade,
  o item legado de chama / isqueiro vanilla.
- **Reação:** usa o frasco de ensaio (`assay_flask`).
- **Prensa mecânica:** utiliza o mecanismo integrado à bancada, sem exigir
  os antigos pesos como ferramenta.

As ferramentas não são consumidas. O ensaio usa uma unidade de material
para o ciclo completo de quatro observações, e não uma por método.
Os antigos pesos e identificadores legados continuam sujeitos às regras de
compatibilidade; não devem ser descritos como equipamentos obrigatórios
da bancada atual.

## Galeria

As imagens abaixo foram produzidas em 2026-11-04 e podem retratar uma versão anterior da interface; a especificação atual está descrita acima e em `design/assay-v2/README.md`.

### Bancada com itens guardados

![Captura histórica da bancada com amostra de ferro](images/bancada-ferro.png)

Trocar a amostra muda o item exibido no mundo:

![A mesma bancada com uma amostra de ouro](images/bancada-ouro.png)

### Bancada sem itens

![Bancada com slots vazios, sem lupa ou amostra fixa](images/bancada-vazia.png)

### Interface de ensaio

![HUD com amostra e ferramentas inseridas, antes de iniciar o ensaio](images/hud-inicial.png)

![Captura histórica do caderno de observações](images/hud-completa.png)

## Próxima etapa de design

Definir com o Hian as aplicações tecnológicas destas observações e a integração
com pesquisas e receitas. Esta entrega mantém a árvore de pesquisas, os relatórios
existentes e os bloqueios de receitas.

## Verificação reproduzível

Na raiz do projeto, com Java 25 e o cache Gradle configurado:

```bash
python3 -m unittest discover -s tests -v
./tools/verify-assay.sh
./gradlew build
```

O servidor de teste da versão atual exercita **quatro métodos em dez materiais**,
incluindo reserva de amostra, ensaio interrompido, persistência, autoria,
recuperação de resultados e migração de dados. Veja
`design/assay-v2/README.md` para os resultados documentados dessa versão
(570 verificações na entrega de 2026-11-04).

O preview opcional usa `./tools/preview-assay.sh` em ambiente Linux com
Java 25, Xvfb e cache Gradle. Os resultados de teste acima não foram
executados novamente apenas para esta atualização documental.

## Histórico de versões anteriores

As capturas e relatórios anteriores à versão com quatro métodos podem mostrar
**0/3 ou 0/30**, ferramentas de pesos/isqueiro ou ensaios galvânicos. Esses
valores descrevem somente o protótipo antigo; não correspondem ao jogo atual.
O resultado `galvanic` antigo continua preservado como histórico durante a
migração, mas não representa o método atual de reação.
