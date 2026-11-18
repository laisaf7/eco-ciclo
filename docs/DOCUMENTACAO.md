# EcoCiclo Android · Documentação técnica

Guia para quem vai entender, rodar ou alterar o app. Para testar como usuário, veja o
[README](../README.md).

## 1. Visão geral

O EcoCiclo transforma recicláveis em **EcoMoedas** para gastar no comércio do bairro:

1. O ecoponto pesa a entrega e gera um **QR Code** (perfil `ADMIN` ou `ARTESAO`).
2. O cidadão lê o QR no app e recebe as moedas: `kg × moedas por kg do material`
   (+50% se a entrega for num ateliê de artesão).
3. O cidadão usa o saldo num **voucher**, numa **transferência** para uma loja ou na
   **compra** de um produto de artesão.

O app só mostra e envia dados: todas as regras (conversão, saldo, cupons, permissões)
ficam na [API](https://github.com/laisaf7/gestao-residuos-api).

## 2. Tecnologias

| Item | Uso |
|---|---|
| Kotlin + Jetpack Compose (Material 3) | Interface |
| ViewModel + `mutableStateOf` | Estado das telas |
| Retrofit + Gson + OkHttp | Chamadas à API REST, com token JWT |
| osmdroid | Mapa (blocos do MapTiler ou do OpenStreetMap) |
| ZXing (`zxing-android-embedded`) | Leitura de QR Code pela câmera |
| Coil | Fotos de artigos e produtos (usa o mesmo cliente HTTP, porque a rota exige o token) |

Mínimo Android 8.0 (API 26), alvo API 34, Java 17.

## 3. Estrutura do código

```
app/src/main/java/com/ecociclo/app
├── MainActivity.kt        -> login ou app principal; barra superior e abas por perfil
├── AppViewModel.kt        -> estado único do app e todas as ações (entrar, resgatar, pagar...)
├── data/Api.kt            -> modelos (data classes), interface Retrofit e cliente HTTP
└── ui/
    ├── Tema.kt            -> cores claras/escuras e formatação de números
    ├── LoginScreen.kt     -> logo, usuário e senha
    ├── InicioScreen.kt    -> saldo, impacto, card de entrega e histórico
    ├── MapaScreen.kt      -> mapa, lista de pontos por distância e botões
    ├── ValidarScreen.kt   -> câmera, simulação e código digitado
    ├── ArtesaosScreen.kt  -> aba Lojas: comércios e artesãos, produtos, compra e cadastro de artesão
    ├── MarketplaceScreen.kt -> lista de comércios, voucher e transferência
    ├── IdeiasScreen.kt    -> artigos de reaproveitamento (ler, publicar, editar)
    ├── AtelieScreen.kt    -> ateliê do artesão: ocupação, QR e produtos
    └── OperadorScreen.kt  -> admin: gerar QR da pesagem e moderar ideias
```

**Padrão:** cada tela recebe o `AppViewModel` e lê o estado dele. Ações passam por
`executar { }`, que liga `carregando`, chama a API e transforma erros em `erro`
(mensagem para o usuário). A navegação é uma aba selecionada em `MainActivity`; as telas
que levam a outra aba recebem um callback (ex.: `onValidar`, `onMapa`, `onEntregar`).

## 4. Abas por perfil

| Aba | `USER` | `ARTESAO` | `ADMIN` |
|---|---|---|---|
| Início, Mapa, Validar, Lojas | sim | sim | sim |
| Ideias | sim | sim | não (modera pela aba Operador) |
| Ateliê | não | sim | não |
| Operador | não | não | sim |

O perfil vem do login e fica salvo em `SharedPreferences` (`sessao`) junto com o token.
Se a API responder 401, a sessão é apagada e o app volta ao login.

## 5. Telas

| Tela | O que faz | Chamadas à API |
|---|---|---|
| Login | Entra e salva a sessão | `POST /api/auth/login` |
| Início | Saldo, kg desviados, card "Entregou seus recicláveis?" e últimas movimentações (saídas em vermelho) | `GET /api/carteira` |
| Mapa | Pontos ativos ordenados pela distância do aparelho; card com material, EcoMoedas por kg, status, "Vou entregar aqui" e "Como chegar" | `GET /api/pontos-coleta` |
| Validar | Lê o QR, simula a leitura (`ECO-4821`) ou valida um código digitado | `POST /api/carteira/resgates` |
| Lojas | Comércios com filtro; voucher ou transferência. Aba Artesãos: produtos, compra e cadastro de artesão | `GET /api/comercios`, `POST /api/carteira/pagamentos`, `/api/produtos`, `POST /api/artesao` |
| Ideias | Artigos com filtro por material; publicar, editar e excluir os próprios | `/api/artigos` |
| Ateliê | Ocupação, gerar QR de entregas, esvaziar e produtos | `/api/artesao/atelie`, `POST /api/entregas-qr`, `/api/produtos` |
| Operador | Gerar QR da pesagem e moderar ideias | `POST /api/entregas-qr`, `DELETE /api/artigos/{id}` |

## 6. Mapa

- **Blocos:** com `maptiler.key` preenchida, usa o estilo MapTiler Streets; sem a chave,
  o OpenStreetMap padrão. Os créditos aparecem no canto do mapa (`CopyrightOverlay`).
- **Cache:** os blocos ficam no cache do app (`cacheDir/tiles`), sem pedir permissão de armazenamento.
- **Marcadores:** círculo verde com reciclagem (ecoponto) e laranja com flor (ateliê).
- **Localização:** pede permissão ao abrir; com ela, ordena os pontos pela distância e mostra
  "X m" ou "X km". Usa a última localização conhecida do aparelho.
- **Valor por kg:** vem no campo `moedasPorKg` de cada ponto. Se a API não mandar, o texto é omitido.

## 7. Configuração

| Onde | Chave | Para quê |
|---|---|---|
| `gradle.properties` | `ecociclo.apiUrl` | Endereço da API (Render em produção; `http://10.0.2.2:8080/` no emulador) |
| `local.properties` (fora do git) | `maptiler.key` | Chave do MapTiler; vira `BuildConfig.MAPTILER_KEY` |
| `app/build.gradle.kts` | `cleartext` | Permite HTTP sem TLS só na versão debug (API local) |

O tempo de espera das chamadas é 90 s, porque o plano grátis do Render leva cerca de
50 s para religar a API depois de um tempo parada.

**Chave do MapTiler:** fica dentro do APK, então quem baixar o app consegue extraí-la.
Acompanhe o uso no painel do MapTiler (plano grátis: 100 mil carregamentos por mês).
Não use a restrição "Allowed HTTP Origins": o app não envia esse cabeçalho e o mapa ficaria em branco.

## 8. Rodar, instalar e gerar o APK

```powershell
.\gradlew :app:installDebug     # compila e instala no celular conectado por USB
.\gradlew assembleDebug         # gera app/build/outputs/apk/debug/app-debug.apk
```

Para usar a API local, veja "Rodar localmente" no [README](../README.md).

## 9. Dados de demonstração

Ficam na API (migrações Flyway), não no app:

| Migração | Conteúdo |
|---|---|
| `V4__ecociclo.sql` | Coordenadas dos ecopontos, moedas por kg (papel 2, plástico 5, vidro 1,5, metal 8, demais 1) e comércios |
| `V8__artesao.sql` | Conta `artesao`, o ateliê e 2 produtos |
| `V10__historico_demo.sql` | Saldo, kg e histórico de exemplo, diferentes em cada conta |
| `V11__codigos_demo.sql` | Códigos de teste reutilizáveis `ECO-4821`, `ECO-7310` e `ATL-0550` |

## 10. Teste manual rápido

1. Entrar com `usuario` / `user123`: o Início mostra saldo, kg e histórico.
2. Mapa: os cards mostram EcoMoedas por kg; "Vou entregar aqui" abre o Validar.
3. Validar: "Simular leitura do QR Code" mostra +15 EcoMoedas e o novo saldo.
4. Lojas: gerar um voucher; o Início mostra a saída em vermelho, no topo do histórico.
5. Sair e entrar com `artesao` / `artesao123`: aparece a aba Ateliê.
