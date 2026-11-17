package com.ecociclo.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.INCENTIVO_ATELIE
import com.ecociclo.app.data.Produto

/** Aba do artesão: o ponto de coleta do ateliê, o QR das entregas e os produtos à venda. */
@Composable
fun AtelieScreen(vm: AppViewModel) {
    val atelie = vm.meuAtelie
    val meus = vm.produtos.filter { it.artesao == vm.sessao?.username }
    var esvaziando by rememberSaveable { mutableStateOf(false) }
    var novo by rememberSaveable { mutableStateOf(false) }
    var editandoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var excluindoId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (atelie == null) {
            Text("Carregando seu ateliê... Se não aparecer, toque em Atualizar.")
            return@Column
        }
        Text(atelie.nome, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Recebe ${atelie.nomeTipoResiduo} · ${textoStatusAtelie(atelie.status)}")
        val fracao = (atelie.percentualOcupacao.toFloat() / 100f).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { fracao }, modifier = Modifier.fillMaxWidth())
        Text("${atelie.quantidadeAtualKg.formatado()} de ${atelie.capacidadeKg.formatado()} kg guardados")
        OutlinedButton(onClick = { esvaziando = true }, enabled = !vm.carregando) { Text("Usei o material: esvaziar") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Registrar entrega", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Pese só a parte reciclável que você vai usar, gere o QR e peça para a pessoa ler na aba Validar. " +
                "Ela ganha +$INCENTIVO_ATELIE% de EcoMoedas e um cupom de $INCENTIVO_ATELIE% em um produto seu.",
        )
        PesagemQr(vm, atelie)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Meus produtos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Button(onClick = { novo = true }, modifier = Modifier.fillMaxWidth()) { Text("Publicar produto") }
        if (meus.isEmpty()) Text("Nenhum produto ainda. Eles aparecem em Lojas › Artesãos.")
        meus.forEach { produto ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(produto.nome, fontWeight = FontWeight.Bold)
                        Text(
                            "R$ ${produto.precoReais.formatado()}" +
                                (produto.precoMoedas?.let { " · ${it.formatado()} EcoMoedas" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { editandoId = produto.id }) { Text("Editar") }
                    TextButton(onClick = { excluindoId = produto.id }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (esvaziando) {
        AlertDialog(
            onDismissRequest = { esvaziando = false },
            title = { Text("Esvaziar o ateliê?") },
            text = { Text("A ocupação volta para 0 kg e o ponto fica disponível para novas entregas.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.esvaziarAtelie()
                    esvaziando = false
                }) { Text("Esvaziar") }
            },
            dismissButton = { TextButton(onClick = { esvaziando = false }) { Text("Cancelar") } },
        )
    }
    if (novo) DialogoProduto(vm, editando = null, onFechar = { novo = false })
    vm.produtos.find { it.id == editandoId }?.let { DialogoProduto(vm, editando = it, onFechar = { editandoId = null }) }
    vm.produtos.find { it.id == excluindoId }?.let { produto ->
        AlertDialog(
            onDismissRequest = { excluindoId = null },
            title = { Text("Excluir produto?") },
            text = { Text("\"${produto.nome}\" sai do catálogo. Isso não pode ser desfeito.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.excluirProduto(produto.id)
                        excluindoId = null
                    },
                    enabled = !vm.carregando,
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { excluindoId = null }) { Text("Cancelar") } },
        )
    }
}

private fun textoStatusAtelie(status: String) = when (status) {
    "DISPONIVEL" -> "Disponível"
    "ALERTA" -> "Quase cheio"
    "CHEIO" -> "Cheio"
    else -> "Inativo"
}

/** Publica um produto novo (editando = null) ou edita um existente. */
@Composable
private fun DialogoProduto(vm: AppViewModel, editando: Produto?, onFechar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf(editando?.nome.orEmpty()) }
    var descricao by rememberSaveable { mutableStateOf(editando?.descricao.orEmpty()) }
    var reaisTexto by rememberSaveable { mutableStateOf(editando?.precoReais?.toPlainString().orEmpty()) }
    var moedasTexto by rememberSaveable { mutableStateOf(editando?.precoMoedas?.toPlainString().orEmpty()) }
    var foto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removerFoto by rememberSaveable { mutableStateOf(false) }
    // Seletor de fotos do sistema: não pede permissão de acesso à galeria
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            foto = uri
            removerFoto = false
        }
    }
    val previa: Any? = foto ?: editando?.takeIf { it.temImagem && !removerFoto }?.urlImagem
    val reais = lerValor(reaisTexto)
    val moedas = lerValor(moedasTexto)
    val moedasOk = moedasTexto.isBlank() || moedas != null
    val valido = nome.isNotBlank() && reais != null && moedasOk

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (editando == null) "Novo produto" else "Editar produto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(120) },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex.: Vaso de garrafa PET") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it.take(1000) },
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reaisTexto,
                    onValueChange = { reaisTexto = it },
                    label = { Text("Preço em R$") },
                    isError = reaisTexto.isNotBlank() && reais == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = moedasTexto,
                    onValueChange = { moedasTexto = it },
                    label = { Text("Preço em EcoMoedas (opcional)") },
                    supportingText = { Text("Preenchido, dá para comprar pelo app. Vazio, só em reais no ateliê.") },
                    isError = !moedasOk,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                previa?.let {
                    AsyncImage(
                        model = it,
                        imageLoader = vm.imagens,
                        contentDescription = "Foto do produto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (previa == null) "Adicionar foto" else "Trocar foto") }
                    if (previa != null) {
                        TextButton(onClick = {
                            foto = null
                            removerFoto = true
                        }) { Text("Remover") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.salvarProduto(editando?.id, nome, descricao, reais!!, moedas, foto, removerFoto, onSucesso = onFechar)
                },
                enabled = valido && !vm.carregando,
            ) { Text(if (editando == null) "Publicar" else "Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
