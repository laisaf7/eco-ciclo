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

data class Carteira(val saldo: BigDecimal, val kgDesviados: BigDecimal, val transacoes: List<Transacao>)

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
