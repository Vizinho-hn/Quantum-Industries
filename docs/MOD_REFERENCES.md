# Quantum Industries — Referências técnicas e visuais de outros mods

**Propósito:** registrar observações verificáveis sobre Create, Oritech, Applied Energistics 2 (AE2) e Mekanism, separando **fatos do código/documentação** de **lições de design propostas** para o Quantum Industries. Não é uma licença para copiar assets, código ou APIs. Fontes consultadas em 19/11/2026; caminhos e versões podem mudar.

## Create

**Fontes oficiais:** [repositório](https://github.com/Creators-of-Create/Create) · [README](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/README.md) · [código de cinética](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics) · [assets e Ponder](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/resources/assets/create).

**Verificado:**
- O README apresenta o Create como mod de construção, decoração e **automação estética**, priorizando máquinas e componentes animados combináveis no mundo em vez de concentrar tudo em interfaces.
- O projeto contém `KineticNetwork.java`, `RotationPropagator.java` e um conjunto de componentes em `content/kinetics/` (eixos, rodas-d'água, serrarias, prensas etc.).
- A infraestrutura de renderização cinética está organizada em `content/kinetics/base/`, com `KineticBlockEntityRenderer.java`, `ShaftRenderer.java` e `ShaftVisual.java`.
- O README documenta o **Ponder**, sistema de explicação visual in-game, e o repositório possui recursos em `assets/create/ponder/`.

**Lição proposta:** fazer o mecanismo ser visível e legível no próprio mundo; modularidade, conexão física e feedback de movimento valem mais que engrenagens decorativas. Para a Era I, revisar continuidade de eixos, mancais e animação sem copiar o sistema de cinética do Create. A organização e os nomes acima **não** garantem compatibilidade de API com Minecraft 26.1.2.

## Oritech

**Fontes oficiais:** [repositório](https://github.com/Rearth/Oritech) · [README](https://github.com/Rearth/Oritech/blob/26.1/README.md) · [modelos GeckoLib](https://github.com/Rearth/Oritech/tree/26.1/src/main/resources/assets/oritech/geckolib/models) · [animações GeckoLib](https://github.com/Rearth/Oritech/tree/26.1/src/main/resources/assets/oritech/geckolib/animations) · [documentação](https://moddedmc.org/en/mod/oritech/docs).

**Verificado:**
- O README declara explicitamente o uso de **Blockbench para criar/animar modelos** e **GeckoLib para animações**.
- O repositório separa modelos e animações GeckoLib por `block`, `entity` e `item`, além de possuir `blockstates`, `models` e `textures` nos assets.
- O projeto descreve processamento avançado de minérios e máquinas tecnológicas, com compatibilidades para outros mods.
- **Atenção à origem dos assets:** o README credita modelos/texturas de ArtOfTecharium sob **CC BY-NC 4.0** e recursos de terceiros sob **CC BY 4.0**. Portanto, não presumir que todos os assets possam ser reutilizados sob uma única licença, independentemente da licença geral do repositório.

**Lição proposta:** adotar um pipeline sólido **conceito → Blockbench → textura → animação → teste in-game**, com riqueza de volume e acabamento consistente. O alvo é o **nível de execução**, não reproduzir modelos ou texturas de Oritech/Techarium.

## Applied Energistics 2 (AE2)

**Fontes oficiais:** [repositório](https://github.com/AppliedEnergistics/Applied-Energistics-2) · [README](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/README.md) · [rede ME](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/main/src/main/java/appeng/me) · [assets](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/main/src/main/resources/assets/ae2).

**Verificado:**
- O código organiza a rede em `appeng/me/`, com `Grid.java`, `GridNode.java`, `GridConnection.java`, `pathfinding/`, `service/` e `storage/`.
- O repositório separa `appeng/api`, `appeng/client`, `appeng/parts`, `appeng/menu` e `appeng/datagen`; os assets incluem modelos, texturas e telas.
- O README publica orientações para a API e para desenvolvimento de integrações, com ressalvas sobre compatibilidade de versões.

**Lição proposta:** padronizar interfaces de rede, estados, conexões e nomenclatura de componentes; favorecer legibilidade de portas e módulos. A estética mais limpa/eletrônica pode informar eras avançadas, mas **não** deve substituir a linguagem artesanal da Era I. Não importar a arquitetura de redes do AE2 sem necessidade concreta.

## Mekanism

**Fontes oficiais:** [repositório](https://github.com/mekanism/Mekanism) · [README](https://github.com/mekanism/Mekanism/blob/26.3/README.md) · [tiers](https://github.com/mekanism/Mekanism/tree/26.3/src/main/java/mekanism/common/tier) · [assets](https://github.com/mekanism/Mekanism/tree/26.3/src/main/resources/assets/mekanism).

**Verificado:**
- O README documenta quatro tiers: **basic, advanced, elite, ultimate**, com evolução por receitas ou instaladores.
- O código separa tipos como `FactoryTier.java`, `EnergyCubeTier.java`, `CableTier.java`, `PipeTier.java` e `TubeTier.java`.
- Os assets incluem `blockstates/`, `models/`, `textures/`, `render/` e `shaders/`.
- O README do branch consultado trata a versão **26.3 como alpha**, com alertas de mudanças de API e persistência. Não tomar código desse branch como exemplo compatível com o ambiente 26.1.2 do Quantum Industries.

**Lição proposta:** tornar upgrades e famílias de máquinas visualmente reconhecíveis; diferenciar capacidade, material e complexidade tecnológica sem perder identidade de marca. Não transformar as cinco eras do Quantum Industries nos quatro tiers do Mekanism: são sistemas de progressão distintos.

## Síntese para o Quantum Industries

| Referência | Aprendizado principal | Aplicação sugerida | Limite |
|---|---|---|---|
| Create | Mecânica modular e movimento legível | Eixos, rodas-d'água, serraria e conexões da Era I | Não clonar cinética, modelos ou animações |
| Oritech | Pipeline Blockbench + GeckoLib e acabamento de máquinas | Protótipos com geometria intencional e animação estável | Não reutilizar assets de origem/licença incerta |
| AE2 | Organização de rede, partes e interfaces | Sistemas de conexão, estados e leitura de portas | Não importar abstrações complexas prematuramente |
| Mekanism | Famílias de equipamentos e evolução por tiers | Consistência visual de upgrades e máquinas de eras posteriores | Não confundir tier de equipamento com era de pesquisa |

**Política de implementação:** os guias [ART_DIRECTION.md](ART_DIRECTION.md), [ERA1_VISUAL_STYLE_GUIDE.md](ERA1_VISUAL_STYLE_GUIDE.md) e [ERA2_VISUAL_STYLE_GUIDE.md](ERA2_VISUAL_STYLE_GUIDE.md) definem o design do **nosso** mod. Referências externas são inspiração e evidência técnica, nunca autoridade sobre a arquitetura local. Validar versões de NeoForge, Minecraft e GeckoLib antes de transpor padrões de código.

**Pendente de pesquisa mais profunda:** medições comparativas de densidade de texels, pipelines exatos de renderização de cada mod, custos de renderização e exemplos de integração por versão. Não declarar essas questões resolvidas sem inspeção de arquivos e testes adicionais.
