package com.ecociclo.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecociclo.app.data.ApiErro
import com.ecociclo.app.data.Artigo
import com.ecociclo.app.data.ArtigoRequest
import com.ecociclo.app.data.Carteira
import com.ecociclo.app.data.Comercio
import com.ecociclo.app.data.EntregaQr
import com.ecociclo.app.data.EntregaQrRequest
import com.ecociclo.app.data.LoginRequest
import com.ecociclo.app.data.PagamentoRequest
import com.ecociclo.app.data.Ponto
import com.ecociclo.app.data.Resgate
import com.ecociclo.app.data.ResgateRequest
import com.ecociclo.app.data.Sessao
import com.ecociclo.app.data.Transacao
import com.ecociclo.app.data.criarApi
import com.ecociclo.app.data.criarCliente
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("sessao", Context.MODE_PRIVATE)
    private val cliente = criarCliente { sessao?.token }
    private val api = criarApi(cliente)

    /** Coil com o mesmo cliente HTTP, porque a rota das fotos exige login. */
    val imagens = ImageLoader.Builder(app).okHttpClient(cliente).build()

    var sessao by mutableStateOf(sessaoSalva())
        private set
    var carteira by mutableStateOf<Carteira?>(null)
        private set
    var pontos by mutableStateOf<List<Ponto>>(emptyList())
        private set
    var comercios by mutableStateOf<List<Comercio>>(emptyList())
        private set
    var artigos by mutableStateOf<List<Artigo>>(emptyList())
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
    var resgate by mutableStateOf<Resgate?>(null)
    var pagamento by mutableStateOf<Transacao?>(null)
    var qrGerado by mutableStateOf<EntregaQr?>(null)

    init {
        if (sessao != null) carregar()
    }

    private fun sessaoSalva(): Sessao? {
        val token = prefs.getString("token", null) ?: return null
        return Sessao(token, prefs.getString("username", "").orEmpty(), prefs.getString("role", "").orEmpty())
    }

    // Ignora toques repetidos enquanto uma operação está em andamento, evitando pagamento duplicado
    private fun executar(bloco: suspend () -> Unit) {
        if (carregando) return
        carregando = true
        erro = null
        viewModelScope.launch {
            try {
                bloco()
            } catch (e: HttpException) {
                if (e.code() == 401 && sessao != null) {
                    sair()
                    erro = "Sua sessão expirou. Entre novamente."
                } else {
                    erro = mensagemDe(e)
                }
            } catch (e: IOException) {
                erro = "Sem conexão com o servidor. Verifique se a API está no ar."
            } finally {
                carregando = false
            }
        }
    }

    private fun mensagemDe(e: HttpException): String {
        val corpo = e.response()?.errorBody()?.string()
        val api = runCatching { Gson().fromJson(corpo, ApiErro::class.java) }.getOrNull()
        return api?.detalhes?.firstOrNull() ?: api?.message ?: "Erro ${e.code()} ao falar com o servidor."
    }

    private suspend fun buscarDados() {
        carteira = api.carteira()
        pontos = api.pontos()
        comercios = api.comercios()
        artigos = api.artigos()
    }

    fun carregar() = executar { buscarDados() }

    fun entrar(usuario: String, senha: String) = executar {
        val nova = api.login(LoginRequest(usuario.trim(), senha))
        prefs.edit {
            putString("token", nova.token)
            putString("username", nova.username)
            putString("role", nova.role)
        }
        sessao = nova
        buscarDados()
    }

    fun sair() {
        prefs.edit { clear() }
        sessao = null
        carteira = null
        pontos = emptyList()
        comercios = emptyList()
        artigos = emptyList()
        resgate = null
        pagamento = null
        qrGerado = null
    }

    fun resgatar(codigo: String) = executar {
        resgate = null
        resgate = api.resgatar(ResgateRequest(codigo.trim()))
        carteira = api.carteira()
    }

    fun pagar(comercio: Comercio, valor: BigDecimal, tipo: String) = executar {
        pagamento = api.pagar(PagamentoRequest(comercio.id, valor, tipo))
        carteira = api.carteira()
    }

    /** id nulo publica um artigo novo; com id, edita (só o autor, a API confere). */
    fun salvarArtigo(
        id: Long?, titulo: String, material: String, conteudo: String,
        foto: Uri?, removerFoto: Boolean, onSucesso: () -> Unit,
    ) = executar {
        val base64 = foto?.let { jpegBase64(it) }
        if (foto != null && base64 == null) {
            erro = "Não foi possível ler a foto escolhida."
            return@executar
        }
        val pedido = ArtigoRequest(titulo.trim(), material, conteudo.trim(), base64, removerFoto)
        if (id == null) {
            artigos = listOf(api.publicarArtigo(pedido)) + artigos
        } else {
            val editado = api.editarArtigo(id, pedido)
            artigos = artigos.map { if (it.id == id) editado else it }
        }
        onSucesso()
    }

    fun excluirArtigo(id: Long) = executar {
        api.excluirArtigo(id)
        artigos = artigos.filterNot { it.id == id }
    }

    // O Coil já reduz para caber em 1280 px e corrige a rotação da câmera (EXIF); a API aceita até 1 MB
    private suspend fun jpegBase64(foto: Uri): String? {
        val pedido = ImageRequest.Builder(getApplication()).data(foto).size(1280).allowHardware(false).build()
        val bitmap = (imagens.execute(pedido).drawable as? BitmapDrawable)?.bitmap ?: return null
        return withContext(Dispatchers.Default) {
            val saida = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, saida)
            Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
        }
    }

    fun gerarQr(ponto: Ponto, kg: BigDecimal) = executar {
        qrGerado = null
        qrGerado = api.gerarQr(EntregaQrRequest(ponto.id, ponto.idTipoResiduo, kg))
    }
}
