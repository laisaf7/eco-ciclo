package com.ecociclo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.Transacao

@Composable
fun InicioScreen(vm: AppViewModel, onValidar: () -> Unit) {
    val carteira = vm.carteira
    if (carteira == null) {
        Text(
            if (vm.carregando) "Carregando sua carteira..." else "Não foi possível carregar. Toque em Atualizar.",
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp).semantics(mergeDescendants = true) {}) {
                    Text("Saldo disponível", style = MaterialTheme.typography.titleMedium)
                    Text(
                        carteira.saldo.formatado(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("EcoMoedas", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp).semantics(mergeDescendants = true) {}) {
                    Text("Seu impacto", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${carteira.kgDesviados.formatado()} kg",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("de resíduos desviados de aterros")
                }
            }
        }
        item {
            Button(onClick = onValidar, modifier = Modifier.fillMaxWidth()) {
                Text("Validar uma entrega")
            }
        }
        item {
            Text(
                "Últimas movimentações",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (carteira.transacoes.isEmpty()) {
            item { Text("Nenhuma movimentação ainda. Faça sua primeira entrega em um ecoponto.") }
        }
        items(carteira.transacoes, key = { it.id }) { LinhaTransacao(it) }
    }
}

@Composable
private fun LinhaTransacao(t: Transacao) {
    val credito = t.tipo == "CREDITO"
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(t.descricao, style = MaterialTheme.typography.bodyLarge)
                Text(dataCurta(t.dataTransacao), style = MaterialTheme.typography.bodySmall)
                t.codigoVoucher?.let {
                    Text("Voucher: $it", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                (if (credito) "+" else "−") + t.valor.formatado(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (credito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider()
    }
}
