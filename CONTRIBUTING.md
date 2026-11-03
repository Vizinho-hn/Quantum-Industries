# Contribuindo

Quantum Industries é desenvolvido em colaboração. O objetivo deste documento é manter o trabalho organizado e evitar conflitos no código.

## Antes de começar
1. Leia o [README](README.md), o [Roadmap](ROADMAP.md) e os documentos em `docs/`.
2. Confirme com a equipe qual tarefa está em andamento.
3. Atualize sua branch a partir de `main` antes de começar.
4. Não adicione dependências ou altere versões do Minecraft/NeoForge sem alinhar com a equipe.

## Fluxo recomendado

- `main`: linha principal, deve permanecer compilável.
- `feat/nome-curto`: novas funcionalidades.
- `fix/nome-curto`: correções.
- `docs/nome-curto`: documentação.
- Abra Pull Request para integrar mudanças em `main`.
- Prefira commits pequenos, com mensagens objetivas.
- Não faça force-push em branches compartilhadas.

## Revisão e testes
- Explique o problema e a solução no Pull Request.
- Liste os testes executados e o resultado.
- Inclua imagens ou vídeos quando mudanças visuais precisarem de contexto.
- Não declare uma tarefa concluída sem testar o comportamento relevante.

## Convenções iniciais
- Use nomes de classes e métodos em inglês, seguindo convenções Java.
- Mantenha comentários úteis e evite comentários que apenas repetem o código.
- Prefira sistemas orientados a dados quando isso reduzir duplicação.
- Não coloque regras de jogo críticas apenas no cliente.
- Não misture dados de pesquisa de jogadores diferentes.

## Segurança e segredos
- Nunca envie tokens, senhas, chaves privadas ou arquivos locais sensíveis.
- Não compartilhe credenciais GitHub entre colaboradores; cada pessoa usa sua própria conta.
- Revise `git diff` antes de cada commit.
