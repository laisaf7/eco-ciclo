package com.ecociclo.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecociclo.app.AppViewModel
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun OperadorScreen(vm: AppViewModel) {
    var idPonto by rememberSaveable { mutableStateOf<Long?>(null) }
    var kgTexto by rememberSaveable { mutableStateOf("") }
    val ponto = vm.pontos.firstOrNull { it.id == idPonto }
    val kg = lerValor(kgTexto)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Registrar pesagem", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Escolha o ecoponto", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            vm.pontos.filter { it.ativo }.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = item.id == idPonto, onClick = { idPonto = item.id }, role = Role.RadioButton)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = item.id == idPonto, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(item.nome)
                        Text(item.nomeTipoResiduo, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        OutlinedTextField(
            value = kgTexto,
            onValueChange = { kgTexto = it },
            label = { Text("Peso em kg") },
            singleLine = true,
            isError = kgTexto.isNotBlank() && kg == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.gerarQr(ponto!!, kg!!) },
            enabled = ponto != null && kg != null && !vm.carregando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Gerar QR Code") }

        vm.qrGerado?.let { qr ->
            val imagem = remember(qr.codigo) {
                BarcodeEncoder().encodeBitmap(qr.codigo, BarcodeFormat.QR_CODE, 720, 720).asImageBitmap()
            }
            Text(
                "${qr.quantidadeKg.formatado()} kg de ${qr.nomeTipoResiduo}: ${qr.moedas.formatado()} EcoMoedas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Image(
                bitmap = imagem,
                contentDescription = "QR Code da entrega. O código em texto está logo abaixo.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color.White)
                    .padding(8.dp)
                    .size(260.dp),
            )
            SelectionContainer { Text(qr.codigo, style = MaterialTheme.typography.bodyMedium) }
        }

        ModerarIdeias(vm)
    }
}

/** Admin não vê a aba Ideias, então modera (exclui qualquer artigo) por aqui. */
@Composable
private fun ModerarIdeias(vm: AppViewModel) {
    var excluindoId by rememberSaveable { mutableStateOf<Long?>(null) }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text("Moderar ideias", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (vm.artigos.isEmpty()) Text("Nenhuma ideia publicada.")
    vm.artigos.forEach { artigo ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(artigo.titulo, fontWeight = FontWeight.Bold)
                Text("${artigo.material} · por ${artigo.autor}", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { excluindoId = artigo.id }, enabled = !vm.carregando) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    vm.artigos.find { it.id == excluindoId }?.let { artigo ->
        AlertDialog(
            onDismissRequest = { excluindoId = null },
            title = { Text("Excluir ideia?") },
            text = { Text("\"${artigo.titulo}\", de ${artigo.autor}, será apagada para todos. Isso não pode ser desfeito.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.excluirArtigo(artigo.id)
                    excluindoId = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { excluindoId = null }) { Text("Cancelar") } },
        )
    }
}
