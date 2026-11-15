package com.ecociclo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecociclo.app.AppViewModel

// Mesma lista validada pela API em ArtigoRequest
private val MATERIAIS = listOf("Plástico", "Papel", "Vidro", "Metal", "Tecido", "Madeira", "Outros")

@Composable
fun IdeiasScreen(vm: AppViewModel) {
    var material by rememberSaveable { mutableStateOf<String?>(null) }
    var aberto by rememberSaveable { mutableStateOf<Long?>(null) }
    var novo by rememberSaveable { mutableStateOf(false) }
    val lista = vm.artigos.filter { material == null || it.material == material }

    Box(Modifier.fillMaxSize()) {
        Column {
            Text(
                "Transforme o que iria para o lixo em algo novo.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = material == null, onClick = { material = null }, label = { Text("Todos") })
                }
                items(MATERIAIS) { item ->
                    FilterChip(selected = material == item, onClick = { material = item }, label = { Text(item) })
                }
            }
            LazyColumn(
                // Espaço extra embaixo para o botão flutuante não cobrir o último card
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (lista.isEmpty()) item { Text("Nenhuma ideia para este material ainda. Seja a primeira pessoa a publicar!") }
                items(lista, key = { it.id }) { artigo ->
                    val expandido = aberto == artigo.id
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = if (expandido) "Recolher" else "Ler passo a passo") {
                                aberto = if (expandido) null else artigo.id
                            },
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(artigo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${artigo.material} · por ${artigo.autor}", style = MaterialTheme.typography.labelLarge)
                            if (expandido) {
                                Text(artigo.conteudo, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                Text(artigo.conteudo, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Text("Toque para ler o passo a passo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { novo = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Publicar ideia") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (novo) DialogoNovoArtigo(vm, materialInicial = material, onFechar = { novo = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoNovoArtigo(vm: AppViewModel, materialInicial: String?, onFechar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var material by rememberSaveable { mutableStateOf(materialInicial) }
    var conteudo by rememberSaveable { mutableStateOf("") }
    val valido = titulo.isNotBlank() && material != null && conteudo.isNotBlank()

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Nova ideia") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it.take(120) },
                    label = { Text("Título") },
                    placeholder = { Text("Ex.: Tapete de camisetas velhas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Material", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MATERIAIS.forEach { item ->
                        FilterChip(selected = material == item, onClick = { material = item }, label = { Text(item) })
                    }
                }
                OutlinedTextField(
                    value = conteudo,
                    onValueChange = { conteudo = it.take(4000) },
                    label = { Text("Materiais e passo a passo") },
                    placeholder = { Text("1. Corte...\n2. Costure...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.publicarArtigo(titulo, material!!, conteudo, onSucesso = onFechar) },
                enabled = valido && !vm.carregando,
            ) { Text("Publicar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
