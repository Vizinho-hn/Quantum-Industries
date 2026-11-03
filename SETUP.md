# Preparação local (Linux / VS Code)

Este branch usa a estrutura ModDevGradle oficial para Minecraft 26.1. **Ainda falta incorporar o Gradle Wrapper binário** (`gradle-wrapper.jar` e os scripts `gradlew`/`gradlew.bat`) do MDK oficial. Não execute `./gradlew` antes de adicioná-los.

1. Instale o JDK 25 e confirme `java -version`.
2. Baixe o [MDK oficial ModDevGradle 26.1](https://github.com/NeoForgeMDKs/MDK-26.1-ModDevGradle).
3. Copie **apenas** `gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar` do MDK para a raiz deste projeto.
4. No Linux, execute `chmod +x gradlew`.
5. Execute `./gradlew build` e depois `./gradlew runClient`.
6. Se houver falha de compilação ou incompatibilidade da API 26.1, corrija-a antes de integrar na `main`.

**Status:** scaffold inicial não testado. As versões seguem um snapshot do MDK oficial consultado em 03/11/2026; devem ser conferidas ao instalar.
