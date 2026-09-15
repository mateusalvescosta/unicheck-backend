# unicheck-backend

## Execucao local no IntelliJ

Requisitos: JDK 21, PostgreSQL em execucao e o banco local `unicheck_db` criado.

Na primeira configuracao de cada maquina:

1. Copie `src/main/resources/application.exemplo.properties` para `src/main/resources/application-local.properties`.
2. No arquivo copiado, substitua `PREENCHA_USUARIO_POSTGRES`, `PREENCHA_SENHA_POSTGRES` e `PREENCHA_COM_SEGREDO_ALEATORIO_DE_32_BYTES_OU_MAIS` por valores particulares da maquina. O segredo JWT deve ter ao menos 32 bytes e permanecer estavel entre inicializacoes.
3. Execute `com.unicheck.UnicheckApplication` pelo **Run** do IntelliJ. Nao e necessario selecionar perfil nem cadastrar variaveis na IDE.

O perfil `local` e apenas o perfil padrao quando nenhum outro estiver ativo. Outros ambientes podem selecionar explicitamente outro perfil por `SPRING_PROFILES_ACTIVE` ou `-Dspring.profiles.active`. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` e `JWT_ISSUER` sobrescrevem os valores locais quando definidas.

Para gerar um segredo local no PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

O arquivo `application-local.properties` e ignorado pelo Git. Nao compartilhe esse arquivo nem use nele credenciais de ambientes reais.

## Fluxo de checklists

Os endpoints autenticados usam JWT Bearer. O Swagger está disponível em `/swagger-ui.html` e oferece o botão **Authorize**.

Principais operações administrativas:

- `POST /admin/checklists`, `PUT /admin/checklists/{id}` e `DELETE /admin/checklists/{id}`.
- `POST/PUT/DELETE /admin/checklists/{id}/itens[...]` para etapas.
- `POST/DELETE /admin/checklists/{id}/prerequisitos/{requisitoId}`.
- `POST /admin/checklists/{id}/publicar` e `/arquivar`.
- `GET /admin/horas?status=PENDENTE` e `PUT /admin/horas/alunos/{alunoId}/checklists/{checklistId}/lancamento`.

Jornada do aluno:

- `GET /checklists` lista cards e bloqueios; `GET /checklists/{id}` retorna as etapas acessíveis.
- `PUT /checklists/{id}/itens/{itemId}/conclusao` conclui, de forma idempotente, a próxima etapa permitida.
- `GET /alunos/me/horas` e `GET /alunos/me/gamificacao` consultam dados próprios.

Exemplo fictício para criar uma fase:

```json
{
  "nome": "Primeiros acessos",
  "descricao": "Configure seus acessos acadêmicos.",
  "ordem": 1,
  "xp": 20,
  "horasComplementares": 0
}
```

Exemplo fictício de etapa; `conteudo` aceita Markdown:

```json
{
  "titulo": "Acesse o portal",
  "descricao": "Faça o primeiro acesso ao portal acadêmico.",
  "conteudo": "## O que fazer agora\nAcesse o portal e confirme seu cadastro.\n\n## Você terminou quando\nSeu nome aparecer na página inicial.",
  "ordem": 1,
  "xp": 10
}
```

O backend armazena Markdown como texto e não acessa os links informados. O frontend deve sanitizar HTML e bloquear esquemas de URL inseguros antes de renderizar o conteúdo.
