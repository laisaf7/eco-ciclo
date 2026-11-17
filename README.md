# EcoCiclo (Android)

A moeda digital que transforma resíduos em desenvolvimento local. O cidadão
entrega recicláveis num ecoponto, recebe moedas pelo peso e gasta em comércios
do bairro.

App em Kotlin com Jetpack Compose. O backend é a
[gestao-residuos-api](https://github.com/laisaf7/gestao-residuos-api)
(Spring Boot + PostgreSQL), hospedada no Render com o banco no Neon.

## Testar no celular

1. Baixe o **[EcoCiclo.apk](https://github.com/laisaf7/eco-ciclo/releases/latest/download/EcoCiclo.apk)** no celular Android.
2. Abra o arquivo e permita a instalação de "fontes desconhecidas" quando o Android pedir.
3. Siga o roteiro abaixo.

A API usa o plano grátis do Render, que desliga após um tempo sem uso. Se a
primeira tela demorar até 1 minuto, é o servidor ligando; depois fica rápido.

## Roteiro da demonstração

| Passo | Conta | Onde |
|---|---|---|
| 1. Gerar o QR da pesagem | `admin` / `admin123` | Aba Operador |
| 2. Ler o QR ou digitar o código | `usuario` / `user123` | Aba Validar |
| 3. Ver saldo e kg | `usuario` | Aba Início |
| 4. Gerar voucher ou transferir | `usuario` | Aba Lojas |
| 5. Ler e publicar ideias de reaproveitamento | `usuario` | Aba Ideias |
| 6. Excluir qualquer ideia (moderação) | `admin` | Aba Operador, seção "Moderar ideias" |
| 7. Gerar o QR de uma entrega no ateliê | `artesao` / `artesao123` | Aba Ateliê |
| 8. Ler o QR e comprar um produto com EcoMoedas | `usuario` | Abas Validar e Lojas › Artesãos |
Coordenadas dos ecopontos, moedas por kg e comércios são dados de demonstração (`V4__ecociclo.sql` na API).

## Aba Ideias

Artigos que ensinam a transformar material reciclável em outro produto (ex.: bolsa
com calça jeans velha, horta vertical com garrafa PET).

- **Filtro por material:** Plástico, Papel, Vidro, Metal, Tecido, Madeira e Outros.
- **Ler:** o card mostra a foto e o começo do texto; ao tocar, abre o passo a passo.
- **Publicar:** botão "Publicar ideia", com título, material, passo a passo e foto opcional.
- **Editar e excluir:** aparecem só nos artigos de quem está logado.
- **Moderação:** o `admin` não vê a aba Ideias; ele exclui qualquer artigo pela aba Operador.

Os 6 artigos de exemplo vêm da API (`V5` e `V6`), com fotos de licença livre e o
crédito no fim do texto.

**Detalhes técnicos:**

| Onde | O quê |
|---|---|
| `ui/IdeiasScreen.kt` | Lista, filtro e a janela que publica ou edita |
| `ui/OperadorScreen.kt` | Seção "Moderar ideias" do admin |
| `AppViewModel.kt` | `salvarArtigo` (publica ou edita), `excluirArtigo` e a compressão da foto |
| Fotos | [Coil](https://coil-kt.github.io/coil/) com o mesmo cliente HTTP da API, porque a rota da foto exige o token |
| Escolher foto | Seletor de fotos do Android (`PickVisualMedia`), que não pede permissão de galeria |
| Enviar foto | O Coil reduz para até 1280 px e corrige a rotação da câmera; o app envia JPEG 80% em Base64 (a API aceita até 1 MB) |
| Cache | A URL da foto leva `?v=` com a data da última edição, para não mostrar a foto antiga depois de trocar |

## Perfil Artesão

Incentiva quem cria e vende produtos feitos com material reciclado.

- **Virar artesão:** em Lojas › Artesãos, o usuário escolhe o material que recebe, a
  capacidade (até 100 kg; ecopontos têm centenas), endereço e localização. A conta
  passa a ser de artesão na hora, sem novo login. Não teste isso com a conta pública
  `usuario`: a troca é permanente e muda o roteiro para quem vier depois.
- **Ateliê = ponto de coleta pequeno:** aparece no mapa com marcador laranja e o texto
  "Ateliê de artesão". Na aba Ateliê, o artesão gera o QR das entregas (só no próprio
  ateliê) e toca em "esvaziar" quando usa o material.
- **Produtos:** preço em reais (vitrine) e, opcional, em EcoMoedas. Com preço em
  EcoMoedas, qualquer pessoa compra pelo app e recebe um código de retirada; as moedas
  vão para o saldo do artesão.

**Detalhes técnicos:**

| Onde | O quê |
|---|---|
| `ui/ArtesaosScreen.kt` | Aba Lojas com as abas Comércios e Artesãos, catálogo, compra e cadastro de artesão |
| `ui/AtelieScreen.kt` | Ocupação do ateliê, QR das entregas e os produtos do artesão |
| `ui/OperadorScreen.kt` | `PesagemQr`, usado pelo admin e pelo artesão |
| `AppViewModel.kt` | `virarArtesao`, `esvaziarAtelie`, `salvarProduto`, `excluirProduto`, `comprar` |
| API | `V8__artesao.sql`, `ArtesaoService` e `ProdutoService` |

## Rodar localmente

1. Na pasta da API, defina `JWT_SECRET` no `.env` e rode `docker compose up --build -d`.
2. Em `gradle.properties`, troque `ecociclo.apiUrl` para `http://10.0.2.2:8080/` (emulador)
   ou para `http://localhost:8080/` (celular via USB, depois de `adb reverse tcp:8080 tcp:8080`).
3. Abra esta pasta no Android Studio e execute.

## Hospedagem (novembro de 2026)

**Objetivo:** qualquer pessoa consegue instalar o APK e testar a qualquer
momento, sem um PC ligado rodando a API.

- Banco no Neon (região US East), com as tabelas criadas pelo Flyway.
- API em `https://gestao-residuos-api-1.onrender.com/`, com ping a cada 10 min
  em `/actuator/health` via cron-job.org.
- `ecociclo.apiUrl` em `gradle.properties` aponta para o Render.
- Em `data/Api.kt`, o tempo de espera de resposta é 90 s (o padrão do OkHttp é
  10 s), porque o Render leva cerca de 50 s para religar a API.
- A senha do `admin` é pública de propósito, para quem testa conseguir seguir o roteiro.
- APK: `.\gradlew assembleDebug` gera `app/build/outputs/apk/debug/app-debug.apk`.
