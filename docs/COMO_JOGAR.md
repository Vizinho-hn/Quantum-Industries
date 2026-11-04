# Como abrir o Quantum Industries facilmente

## Windows
1. Instale **Git for Windows** e **Java 25**.
2. Clone a branch de testes:
   `git clone --branch main https://github.com/Vizinho-hn/Quantum-Industries.git`
3. Dentro da pasta clonada, abra `JOGAR-WINDOWS.bat` com dois cliques.
4. O script executa `git pull --ff-only` (sem sobrescrever conflitos locais) e depois `gradlew.bat runClient`.

## Linux
1. Instale Git e Java 25.
2. Clone a branch main.
3. No terminal da pasta clonada execute: `bash JOGAR-LINUX.sh`.
4. Para executar diretamente por duplo clique (se seu gerenciador de arquivos permitir): `chmod +x JOGAR-LINUX.sh`.

## JAR pronto para instalar
A aba **Actions** publica o artifact `quantum-industries-neoforge-jar` após um build bem-sucedido. Baixe o ZIP, extraia o JAR e coloque na pasta `mods` de uma instalação compatível de Minecraft 26.1.2 com NeoForge 26.1.2.125.

O JAR não abre sozinho com dois cliques: é um mod carregado pelo Minecraft.
Cada nova alteração na branch dispara uma compilação. **Somente builds bem-sucedidos** terão um JAR para baixar. Artefatos expiram após 14 dias.

## Limitações
- Esta branch é experimental e não foi validada no jogo.
- Se o build falhar, verifique o log de Actions e não use o artefato antigo como evidência de que a nova versão passou.
- `git pull --ff-only` falha com conflitos locais em vez de sobrescrever seu trabalho.
- Os iniciadores funcionam nas branches `main` e `feature/v0.1.1-conductivity-research`.
