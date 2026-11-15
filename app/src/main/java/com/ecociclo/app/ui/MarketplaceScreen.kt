package com.ecociclo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.Comercio

@Composable
fun MarketplaceScreen(vm: AppViewModel) {
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }
    var selecionado by remember { mutableStateOf<Comercio?>(null) }
    val categorias = remember(vm.comercios) { vm.comercios.map { it.categoria }.distinct().sorted() }
    val lista = vm.comercios.filter { categoria == null || it.categoria == categoria }

    Column {
        Text(
            "Saldo: ${vm.carteira?.saldo?.formatado() ?: "--"} EcoMoedas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(selected = categoria == null, onClick = { categoria = null }, label = { Text("Todos") })
            }
            items(categorias) { item ->
                FilterChip(selected = categoria == item, onClick = { categoria = item }, label = { Text(item) })
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (lista.isEmpty()) item { Text("Nenhum comércio encontrado.") }
            items(lista, key = { it.id }) { comercio ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(comercio.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(comercio.categoria, style = MaterialTheme.typography.labelLarge)
                        comercio.descricao?.let { Text(it) }
                        Text(listOfNotNull(comercio.endereco, comercio.bairro).joinToString(" · "))
                        Button(onClick = { selecionado = comercio }) { Text("Usar EcoMoedas") }
                    }
                }
            }
        }
    }

    selecionado?.let { comercio ->
        DialogoPagamento(vm, comercio, onFechar = { selecionado = null })
    }

    vm.pagamento?.let { t ->
        AlertDialog(
            onDismissRequest = { vm.pagamento = null },
            confirmButton = { TextButton(onClick = { vm.pagamento = null }) { Text("Fechar") } },
            title = { Text(if (t.codigoVoucher != null) "Voucher gerado" else "Transferência concluída") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${t.descricao}: ${t.valor.formatado()} EcoMoedas")
                    t.codigoVoucher?.let {
                        SelectionContainer {
                            Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("Mostre este código no caixa. Ele também fica no seu histórico.")
                    }
                }
            },
        )
    }
}

@Composable
private fun DialogoPagamento(vm: AppViewModel, comercio: Comercio, onFechar: () -> Unit) {
    var valorTexto by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("VOUCHER") }
    val valor = lerValor(valorTexto)
    val saldo = vm.carteira?.saldo
    val excede = valor != null && saldo != null && valor > saldo
    val mostrarErro = valorTexto.isNotBlank() && (valor == null || excede)

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(comercio.nome) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor em EcoMoedas") },
                    singleLine = true,
                    isError = mostrarErro,
                    supportingText = {
                        Text(
                            when {
                                excede -> "Maior que seu saldo de ${saldo?.formatado()}"
                                mostrarErro -> "Informe um valor positivo com até 2 casas"
                                else -> "Saldo: ${saldo?.formatado() ?: "--"}"
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Column(Modifier.selectableGroup()) {
                    OpcaoTipo("Gerar voucher", tipo == "VOUCHER") { tipo = "VOUCHER" }
                    OpcaoTipo("Transferir para a loja", tipo == "TRANSFERENCIA") { tipo = "TRANSFERENCIA" }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.pagar(comercio, valor!!, tipo)
                    onFechar()
                },
                enabled = valor != null && !excede && !vm.carregando,
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun OpcaoTipo(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selecionado, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = null)
        Text(texto, Modifier.padding(start = 12.dp))
    }
}
