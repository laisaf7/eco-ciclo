package com.ecociclo.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.INCENTIVO_ATELIE
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun ValidarScreen(vm: AppViewModel) {
    var codigo by rememberSaveable { mutableStateOf("") }
    val leitor = rememberLauncherForActivityResult(ScanContract()) { resultado ->
        resultado.contents?.let { vm.resgatar(it) }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Entregou seus recicláveis?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Escaneie o QR Code gerado pelo ecoponto na pesagem para receber suas EcoMoedas.")
        Button(
            onClick = {
                leitor.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt("Aponte para o QR Code do ecoponto")
                        .setBeepEnabled(false)
                        .setOrientationLocked(false),
                )
            },
            enabled = !vm.carregando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Abrir câmera") }
        // Mesmo código de teste do protótipo web; a API trata ECO-4821 como entrega de demonstração
        OutlinedButton(
            onClick = { vm.resgatar("ECO-4821") },
            enabled = !vm.carregando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Simular leitura do QR Code") }

        Text("Ou digite o código", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código da entrega") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { vm.resgatar(codigo) },
            enabled = !vm.carregando && codigo.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Validar código") }
        Text(
            "Códigos de teste: ECO-4821 (plástico), ECO-7310 (metal), ATL-0550 (ateliê).",
            style = MaterialTheme.typography.bodySmall,
        )

        vm.resgate?.let { r ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (r.descarteCorreto) {
                        Text(
                            "+${r.moedasCreditadas.formatado()} EcoMoedas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (r.descontoAtelie) {
                            Text("Inclui +$INCENTIVO_ATELIE% por entregar num ateliê. Você também ganhou um cupom de $INCENTIVO_ATELIE% em um produto de ${r.nomePontoColeta} (Lojas › Artesãos).")
                        }
                    } else {
                        Text(
                            "Resíduo no ponto errado: nenhuma moeda creditada.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text("${r.quantidadeKg.formatado()} kg de ${r.nomeTipoResiduo} em ${r.nomePontoColeta}")
                    Text("Saldo atual: ${r.saldo.formatado()} EcoMoedas")
                }
            }
        }
    }
}
