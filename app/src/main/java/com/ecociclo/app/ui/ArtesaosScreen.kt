package com.ecociclo.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.CadastroArtesaoRequest
import com.ecociclo.app.data.INCENTIVO_ATELIE
import com.ecociclo.app.data.Produto
import java.math.BigDecimal
import java.math.RoundingMode

/** Aba Lojas: comércios do bairro e produtos dos artesãos. */
@Composable
fun LojasScreen(vm: AppViewModel) {
    var artesaos by rememberSaveable { mutableStateOf(false) }
    Column {
        TabRow(selectedTabIndex = if (artesaos) 1 else 0) {
            Tab(selected = !artesaos, onClick = { artesaos = false }, text = { Text("Comércios") })
            Tab(selected = artesaos, onClick = { artesaos = true }, text = { Text("Artesãos") })
        }
        if (artesaos) ArtesaosScreen(vm) else MarketplaceScreen(vm)
    }
}

@Composable
private fun ArtesaosScreen(vm: AppViewModel) {
    var aberto by rememberSaveable { mutableStateOf<Long?>(null) }
    var comprandoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cadastrando by rememberSaveable { mutableStateOf(false) }
    val cupons = vm.carteira?.ateliesComDesconto.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Produtos feitos com material reciclado. Entregue material num ateliê e ganhe +$INCENTIVO_ATELIE% de " +
                    "EcoMoedas e um cupom de $INCENTIVO_ATELIE% em um produto de lá.",
            )
        }
        if (vm.sessao?.role == "USER") {
            item {
                OutlinedButton(onClick = { cadastrando = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Quero ser artesão")
                }
            }
        }
        if (vm.produtos.isEmpty()) item { Text("Nenhum produto publicado ainda.") }
        items(vm.produtos, key = { it.id }) { produto ->
            val expandido = aberto == produto.id
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (expandido) "Recolher" else "Ver detalhes") {
                        aberto = if (expandido) null else produto.id
                    },
            ) {
                if (produto.temImagem) {
                    AsyncImage(
                        model = produto.urlImagem,
                        imageLoader = vm.imagens,
                        contentDescription = "Foto: ${produto.nome}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(if (expandido) 240.dp else 160.dp),
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(produto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(produto.nomeAtelie, produto.bairro).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    val comCupom = produto.idPontoColeta in cupons && produto.precoMoedas != null
                    Text(textoPreco(produto), fontWeight = FontWeight.Bold)
                    if (comCupom) {
                        Text(
                            "Seu cupom: ${comDesconto(produto.precoMoedas!!).formatado()} EcoMoedas",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (expandido) {
                        produto.descricao?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                        produto.endereco?.let { Text("Retirada: $it", style = MaterialTheme.typography.bodySmall) }
                        Text(
                            "Este ateliê recebe ${produto.material}: entregue lá e ganhe +$INCENTIVO_ATELIE% de EcoMoedas e um cupom.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        when {
                            produto.artesao == vm.sessao?.username ->
                                Text("Seu produto. Edite na aba Ateliê.", style = MaterialTheme.typography.bodySmall)
                            produto.precoMoedas != null ->
                                Button(
                                    onClick = { comprandoId = produto.id },
                                    enabled = !vm.carregando,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    val valor = if (comCupom) comDesconto(produto.precoMoedas) else produto.precoMoedas
                                    Text("Comprar com ${valor.formatado()} EcoMoedas")
                                }
                            else ->
                                Text("Compra em reais, direto no ateliê.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    vm.produtos.find { it.id == comprandoId }?.let { produto ->
        val saldo = vm.carteira?.saldo ?: BigDecimal.ZERO
        val temCupom = produto.idPontoColeta in cupons
        val valor = if (temCupom) comDesconto(produto.precoMoedas!!) else produto.precoMoedas!!
        AlertDialog(
            onDismissRequest = { comprandoId = null },
            title = { Text("Comprar ${produto.nome}?") },
            text = {
                Text(
                    "Serão debitadas ${valor.formatado()} EcoMoedas" +
                        (if (temCupom) " com seu cupom de $INCENTIVO_ATELIE%" else "") +
                        " (saldo: ${saldo.formatado()}). " +
                        "Você recebe um código para retirar em ${produto.nomeAtelie}.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.comprar(produto)
                        comprandoId = null
                    },
                    enabled = !vm.carregando,
                ) { Text("Comprar") }
            },
            dismissButton = { TextButton(onClick = { comprandoId = null }) { Text("Cancelar") } },
        )
    }

    vm.compra?.let { compra ->
        AlertDialog(
            onDismissRequest = { vm.compra = null },
            title = { Text("Compra feita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(compra.descricao)
                    Text("Mostre este código no ateliê para retirar:")
                    SelectionContainer {
                        Text(compra.codigoVoucher.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("O código também fica no seu extrato, na aba Início.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { vm.compra = null }) { Text("Ok") } },
        )
    }

    if (cadastrando) DialogoVirarArtesao(vm, onFechar = { cadastrando = false })
}

/** Preço em EcoMoedas com o cupom de quem entregou no ateliê (a API faz a mesma conta). */
private fun comDesconto(preco: BigDecimal): BigDecimal =
    preco.multiply(BigDecimal(100 - INCENTIVO_ATELIE)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

private fun textoPreco(p: Produto): String =
    "R$ ${p.precoReais.formatado()}" + (p.precoMoedas?.let { " ou ${it.formatado()} EcoMoedas" } ?: "")

/** Usuário comum cadastra o ateliê: material que recebe, capacidade (até 100 kg) e endereço. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoVirarArtesao(vm: AppViewModel, onFechar: () -> Unit) {
    val context = LocalContext.current
    // Os tipos de resíduo vêm dos pontos já carregados, sem rota nova na API
    val tipos = vm.pontos.distinctBy { it.idTipoResiduo }.sortedBy { it.nomeTipoResiduo }
    var idTipo by rememberSaveable { mutableStateOf<Long?>(null) }
    var nome by rememberSaveable { mutableStateOf("") }
    var capacidadeTexto by rememberSaveable { mutableStateOf("50") }
    var endereco by rememberSaveable { mutableStateOf("") }
    var bairro by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    val pedirLocal = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respostas ->
        if (respostas.values.any { it }) {
            val local = ultimaLocalizacao(context)
            latitude = local?.latitude
            longitude = local?.longitude
            if (local == null) vm.erro = "Não foi possível obter sua localização. Abra o mapa e tente de novo."
        }
    }
    val capacidade = lerValor(capacidadeTexto)?.takeIf { it <= BigDecimal(100) }
    val valido = idTipo != null && nome.isNotBlank() && capacidade != null

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Virar artesão") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Seu ateliê vira um ponto de coleta do material que você usa. Quem entrega ganha EcoMoedas.")
                Text("Material que você recebe", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tipos.forEach { p ->
                        FilterChip(selected = idTipo == p.idTipoResiduo, onClick = { idTipo = p.idTipoResiduo }, label = { Text(p.nomeTipoResiduo) })
                    }
                }
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(120) },
                    label = { Text("Nome do ateliê") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = capacidadeTexto,
                    onValueChange = { capacidadeTexto = it },
                    label = { Text("Quanto você consegue guardar (kg)") },
                    supportingText = { Text("Até 100 kg. Ao encher, o ponto aparece como cheio no mapa.") },
                    isError = capacidadeTexto.isNotBlank() && capacidade == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it.take(200) },
                    label = { Text("Endereço para entrega e retirada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bairro,
                    onValueChange = { bairro = it.take(100) },
                    label = { Text("Bairro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = {
                    pedirLocal.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text(if (latitude == null) "Usar minha localização no mapa" else "Localização marcada") }
                if (latitude == null) {
                    Text("Sem localização, o ateliê não aparece no mapa.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.virarArtesao(
                        CadastroArtesaoRequest(
                            idTipo!!, nome.trim(), capacidade!!,
                            endereco.trim().ifBlank { null }, bairro.trim().ifBlank { null }, latitude, longitude,
                        ),
                        onSucesso = onFechar,
                    )
                },
                enabled = valido && !vm.carregando,
            ) { Text("Cadastrar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
