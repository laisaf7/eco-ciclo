## Proposta de Estruturação: EcoCiclo

**1. Identificação da equipe e do aplicativo**

* **Aplicativo:** EcoCiclo – A moeda digital que transforma resíduos em desenvolvimento local.
* (Lembrete para o slide: Inserir o nome completo e o RM de todos os integrantes do grupo).



**2. Problema identificado e público-alvo**

* **Qual é o problema?** A baixa adesão à separação e destinação correta de resíduos recicláveis devido à falta de incentivos práticos e imediatos.


* **Quem enfrenta essa dificuldade?** O ecossistema local: cidadãos que não veem vantagem imediata na reciclagem, cooperativas que sofrem com a falta de material pré-triado e pequenos comércios que buscam fidelizar clientes.


* **Em qual contexto o problema ocorre?** Nas residências e na gestão do espaço urbano, resultando em sobrecarga de sistemas de coleta, desperdício de materiais com valor de mercado e perda de oportunidades econômicas.



**3. Descrição do aplicativo**

* **O que o aplicativo fará:** O EcoCiclo é um modelo de negócio de impacto que recompensa o cidadão pela entrega de recicláveis em ecopontos homologados, gerando "EcoMoedas" para uso exclusivo na rede de comércios parceiros do próprio bairro.


* **Principais funcionalidades:** Mapa interativo de ecopontos, carteira digital de EcoMoedas, leitor de QR Code para validação de entrega e vitrine de lojas parceiras.


* **Exemplo de uso:** O usuário leva uma sacola de embalagens plásticas limpas até uma cooperativa parceira. No local, ele escaneia um QR Code com o aplicativo. O sistema calcula o valor equivalente em EcoMoedas e credita na sua carteira digital. Ele então utiliza esse saldo para pagar parte de uma compra na padaria ou quitanda do bairro.
* **Benefícios:** Gera poder de compra extra ao cidadão, reduz os custos logísticos de triagem para as cooperativas e injeta receita no pequeno comércio local.



**4. Protótipo do fluxo do aplicativo**
A jornada do usuário no protótipo navegável deve conter as seguintes telas principais:

* **Tela Inicial (Dashboard):** Resumo do saldo atual em EcoMoedas e um painel de impacto individual (ex: volume em kg de resíduos desviados de aterros).
* **Tela de Mapa (Descoberta):** Interface com mapas para localizar o ecoponto ou cooperativa mais próxima para realizar o descarte.


* **Tela de Validação (Câmera):** Acionamento da câmera para leitura do QR Code gerado pelo ecoponto no momento da pesagem e entrega do material.
* **Tela de Marketplace:** Lista de pequenos negócios locais cadastrados (com filtros por categoria) onde o usuário pode gerar um voucher ou transferir o saldo.

**5. Arquitetura do aplicativo**

* **Frontend Mobile:** Interface nativa do aplicativo construída em Kotlin com Jetpack Compose para garantir alto desempenho e acessibilidade aos cidadãos.
* **Backend/API:** Um serviço REST desenvolvido em Java com Spring Boot, embutido em contêineres Docker, atuando como o núcleo responsável por processar as transações de moedas, aplicar as regras de negócio de conversão de peso para saldo e gerenciar a segurança.


* **Banco de Dados:** Utilização de uma estrutura relacional (como Oracle SQL) para o armazenamento de usuários, carteiras digitais, registros históricos de descarte e status das solicitações.


* **Serviços Externos:** Integração com APIs de geolocalização e mapas para roteirização e exibição precisa das estações de coleta no aplicativo.



**6. Diferenciais e impacto esperado**

* **Diferenciais:** Em vez de depender apenas de conscientização ambiental, a solução cria um ecossistema financeiro hiperlocal. O modelo se sustenta financeiramente cobrando uma assinatura das empresas compradoras de resíduos (que ganham acesso a volume garantido) ou uma microtaxa sobre as transações nos comércios locais. Para fortalecer o vídeo pitch, valide essas premissas com o dimensionamento de mercado (TAM-SAM-SOM) da sua região e utilize formulários de pesquisa com moradores para cruzar dados na Matriz CSD.


* **Benefícios ao público-alvo:** Facilita a adesão à reciclagem, cria uma rede de apoio ao comércio de bairro e otimiza a operação das cooperativas.


* **Impacto esperado (Conexão ODS):** Aumento exponencial do volume de reciclagem (ODS 11 - Cidades e comunidades sustentáveis), educação da população e redução das emissões de carbono associadas ao desperdício (ODS 13 - Ação contra a mudança global do clima).



Para o vídeo de 3 minutos, reserve os primeiros 45 segundos para apresentar o problema e os dados locais da sua pesquisa de validação, focando o restante do tempo na demonstração do protótipo, no modelo de negócios e em como a tecnologia de backend suporta essa operação de forma escalável.