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

Coordenadas dos ecopontos, moedas por kg e comércios são dados de demonstração (`V4__ecociclo.sql` na API).

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
