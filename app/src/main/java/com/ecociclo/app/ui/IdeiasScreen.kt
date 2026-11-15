package com.ecociclo.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import com.ecociclo.app.data.Artigo

// Mesma lista validada pela API em ArtigoRequest
private val MATERIAIS = listOf("Plástico", "Papel", "Vidro", "Metal", "Tecido", "Madeira", "Outros")

@Composable
fun IdeiasScreen(vm: AppViewModel) {
    var material by rememberSaveable { mutableStateOf<String?>(null) }
    var aberto by rememberSaveable { mutableStateOf<Long?>(null) }
    var novo by rememberSaveable { mutableStateOf(false) }
    var editandoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var excluindoId by rememberSaveable { mutableStateOf<Long?>(null) }
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
                        if (artigo.temImagem) {
                            AsyncImage(
                                model = artigo.urlImagem,
                                imageLoader = vm.imagens,
                                contentDescription = "Foto: ${artigo.titulo}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(if (expandido) 240.dp else 160.dp),
                            )
                        }
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(artigo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${artigo.material} · por ${artigo.autor}", style = MaterialTheme.typography.labelLarge)
                            if (expandido) {
                                Text(artigo.conteudo, modifier = Modifier.padding(top = 8.dp))
                                if (artigo.autor == vm.sessao?.username) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { editandoId = artigo.id }) { Text("Editar") }
                                        TextButton(onClick = { excluindoId = artigo.id }) {
                                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
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

    if (novo) DialogoArtigo(vm, editando = null, materialInicial = material, onFechar = { novo = false })
    vm.artigos.find { it.id == editandoId }?.let {
        DialogoArtigo(vm, editando = it, materialInicial = it.material, onFechar = { editandoId = null })
    }
    vm.artigos.find { it.id == excluindoId }?.let { artigo ->
        AlertDialog(
            onDismissRequest = { excluindoId = null },
            title = { Text("Excluir ideia?") },
            text = { Text("\"${artigo.titulo}\" será apagada para todos. Isso não pode ser desfeito.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.excluirArtigo(artigo.id)
                        excluindoId = null
                    },
                    enabled = !vm.carregando,
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { excluindoId = null }) { Text("Cancelar") } },
        )
    }
}

/** Publica uma ideia nova (editando = null) ou edita uma existente. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoArtigo(vm: AppViewModel, editando: Artigo?, materialInicial: String?, onFechar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf(editando?.titulo.orEmpty()) }
    var material by rememberSaveable { mutableStateOf(materialInicial) }
    var conteudo by rememberSaveable { mutableStateOf(editando?.conteudo.orEmpty()) }
    var foto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removerFoto by rememberSaveable { mutableStateOf(false) }
    // Seletor de fotos do sistema: não pede permissão de acesso à galeria
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            foto = uri
            removerFoto = false
        }
    }
    // Foto nova escolhida agora, ou a que o artigo já tinha (se não foi removida)
    val previa: Any? = foto ?: editando?.takeIf { it.temImagem && !removerFoto }?.urlImagem
    val valido = titulo.isNotBlank() && material != null && conteudo.isNotBlank()

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (editando == null) "Nova ideia" else "Editar ideia") },
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
                previa?.let {
                    AsyncImage(
                        model = it,
                        imageLoader = vm.imagens,
                        contentDescription = "Foto da ideia",
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
                    vm.salvarArtigo(editando?.id, titulo, material!!, conteudo, foto, removerFoto, onSucesso = onFechar)
                },
                enabled = valido && !vm.carregando,
            ) { Text(if (editando == null) "Publicar" else "Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
