package com.ecociclo.app.data

import com.ecociclo.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.math.BigDecimal

data class LoginRequest(val username: String, val password: String)
data class Sessao(val token: String, val username: String, val role: String)

data class Transacao(
    val id: Long,
    val tipo: String,
    val valor: BigDecimal,
    val descricao: String,
    val codigoVoucher: String?,
    val dataTransacao: String,
)

// Mesmo valor de ArtesaoService.INCENTIVO_PERCENTUAL na API: +50% de moedas no ateliê e cupom de 50% nos produtos de lá
const val INCENTIVO_ATELIE = 50

data class Carteira(
    val saldo: BigDecimal,
    val kgDesviados: BigDecimal,
    val transacoes: List<Transacao>,
    // Ids dos ateliês onde a pessoa tem cupom (entregou material e ainda não usou)
    val ateliesComDesconto: List<Long>?,
)

data class Ponto(
    val id: Long,
    val idTipoResiduo: Long,
    val nomeTipoResiduo: String,
    val nome: String,
    val endereco: String?,
    val bairro: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String,
    val ativo: Boolean,
    val capacidadeKg: BigDecimal = BigDecimal.ZERO,
    val quantidadeAtualKg: BigDecimal = BigDecimal.ZERO,
    val percentualOcupacao: BigDecimal = BigDecimal.ZERO,
    // true = ateliê de artesão (ponto pequeno, até 100 kg)
    val artesao: Boolean = false,
)

data class Comercio(
    val id: Long,
    val nome: String,
    val categoria: String,
    val descricao: String?,
    val endereco: String?,
    val bairro: String?,
)

data class ResgateRequest(val codigo: String)
data class Resgate(
    val nomePontoColeta: String,
    val nomeTipoResiduo: String,
    val quantidadeKg: BigDecimal,
    val descarteCorreto: Boolean,
    val moedasCreditadas: BigDecimal,
    val saldo: BigDecimal,
    // true = entrega em ateliê: rendeu bônus e cupom de desconto nos produtos de lá
    val descontoAtelie: Boolean,
)

data class PagamentoRequest(val idComercio: Long, val valor: BigDecimal, val tipo: String)
data class EntregaQrRequest(val idPontoColeta: Long, val idTipoResiduo: Long, val quantidadeKg: BigDecimal)
data class EntregaQr(
    val codigo: String,
    val nomePontoColeta: String,
    val nomeTipoResiduo: String,
    val quantidadeKg: BigDecimal,
    val moedas: BigDecimal,
)

data class Artigo(
    val id: Long,
    val titulo: String,
    val material: String,
    val conteudo: String,
    val autor: String,
    val dataCriacao: String,
    val dataAtualizacao: String?,
    val temImagem: Boolean,
) {
    // ?v= muda a cada edição, para o Coil não mostrar a foto antiga guardada em cache
    val urlImagem get() = "${BuildConfig.API_URL}api/artigos/$id/imagem?v=${(dataAtualizacao ?: dataCriacao).hashCode()}"
}
data class ArtigoRequest(
    val titulo: String,
    val material: String,
    val conteudo: String,
    val imagemBase64: String?,
    val removerImagem: Boolean = false,
)

/** Mesmo corpo de PontoColetaRequest na API; a capacidade do ateliê vai até 100 kg. */
data class CadastroArtesaoRequest(
    val idTipoResiduo: Long,
    val nome: String,
    val capacidadeKg: BigDecimal,
    val endereco: String?,
    val bairro: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class Produto(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val precoReais: BigDecimal,
    // nulo = vendido só em reais, direto com o artesão
    val precoMoedas: BigDecimal?,
    val idPontoColeta: Long,
    val nomeAtelie: String,
    val bairro: String?,
    val endereco: String?,
    val material: String,
    val artesao: String,
    val dataCriacao: String,
    val dataAtualizacao: String?,
    val temImagem: Boolean,
) {
    val urlImagem get() = "${BuildConfig.API_URL}api/produtos/$id/imagem?v=${(dataAtualizacao ?: dataCriacao).hashCode()}"
}
data class ProdutoRequest(
    val nome: String,
    val descricao: String?,
    val precoReais: BigDecimal,
    val precoMoedas: BigDecimal?,
    val imagemBase64: String?,
    val removerImagem: Boolean = false,
)

data class ApiErro(val message: String?, val detalhes: List<String>?)

interface EcoApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Sessao

    @GET("api/carteira")
    suspend fun carteira(): Carteira

    @POST("api/carteira/resgates")
    suspend fun resgatar(@Body request: ResgateRequest): Resgate

    @POST("api/carteira/pagamentos")
    suspend fun pagar(@Body request: PagamentoRequest): Transacao

    @GET("api/pontos-coleta")
    suspend fun pontos(): List<Ponto>

    @GET("api/comercios")
    suspend fun comercios(): List<Comercio>

    @GET("api/artigos")
    suspend fun artigos(): List<Artigo>

    @POST("api/artigos")
    suspend fun publicarArtigo(@Body request: ArtigoRequest): Artigo

    @PUT("api/artigos/{id}")
    suspend fun editarArtigo(@Path("id") id: Long, @Body request: ArtigoRequest): Artigo

    @DELETE("api/artigos/{id}")
    suspend fun excluirArtigo(@Path("id") id: Long)

    @POST("api/entregas-qr")
    suspend fun gerarQr(@Body request: EntregaQrRequest): EntregaQr

    @POST("api/artesao")
    suspend fun virarArtesao(@Body request: CadastroArtesaoRequest): Sessao

    @GET("api/artesao/atelie")
    suspend fun meuAtelie(): Ponto

    @POST("api/artesao/atelie/esvaziar")
    suspend fun esvaziarAtelie(): Ponto

    @GET("api/produtos")
    suspend fun produtos(): List<Produto>

    @POST("api/produtos")
    suspend fun publicarProduto(@Body request: ProdutoRequest): Produto

    @PUT("api/produtos/{id}")
    suspend fun editarProduto(@Path("id") id: Long, @Body request: ProdutoRequest): Produto

    @DELETE("api/produtos/{id}")
    suspend fun excluirProduto(@Path("id") id: Long)

    @POST("api/produtos/{id}/compras")
    suspend fun comprar(@Path("id") id: Long): Transacao
}

/** Cliente HTTP com o token; também usado pelo Coil para baixar as fotos protegidas. */
fun criarCliente(token: () -> String?): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val pedido = chain.request().newBuilder()
            token()?.let { pedido.header("Authorization", "Bearer $it") }
            chain.proceed(pedido.build())
        }
        // O servidor grátis (Render) "acorda" em até ~60 s após ficar parado
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

fun criarApi(cliente: OkHttpClient): EcoApi =
    Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EcoApi::class.java)
