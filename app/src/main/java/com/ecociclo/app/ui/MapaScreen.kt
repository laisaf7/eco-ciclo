package com.ecociclo.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ecociclo.app.AppViewModel
import com.ecociclo.app.data.Ponto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

// shortcut: usa a última localização conhecida do aparelho; trocar por atualização ativa se ela vier vazia com frequência
@SuppressLint("MissingPermission")
private fun ultimaLocalizacao(context: Context): Location? {
    val gerente = context.getSystemService(LocationManager::class.java)
    return gerente.getProviders(true).mapNotNull { gerente.getLastKnownLocation(it) }.maxByOrNull { it.time }
}

private fun distanciaMetros(origem: Location?, ponto: Ponto): Float? {
    if (origem == null || ponto.latitude == null || ponto.longitude == null) return null
    val resultado = FloatArray(1)
    Location.distanceBetween(origem.latitude, origem.longitude, ponto.latitude, ponto.longitude, resultado)
    return resultado[0]
}

private fun textoDistancia(metros: Float): String =
    if (metros < 1000) "${metros.toInt()} m" else "%.1f km".format(metros / 1000)

private fun textoStatus(status: String) = when (status) {
    "DISPONIVEL" -> "Disponível"
    "ALERTA" -> "Quase cheio"
    "CHEIO" -> "Cheio"
    "EM_COLETA" -> "Em coleta"
    else -> "Inativo"
}

@Composable
fun MapaScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var local by remember { mutableStateOf<Location?>(null) }
    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { respostas -> if (respostas.values.any { it }) local = ultimaLocalizacao(context) }

    LaunchedEffect(Unit) {
        pedirPermissao.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    val pontos = remember(vm.pontos, local) {
        vm.pontos.filter { it.ativo && it.latitude != null && it.longitude != null }
            .sortedBy { distanciaMetros(local, it) ?: Float.MAX_VALUE }
    }

    Column {
        MapaOsm(pontos, Modifier.fillMaxWidth().height(280.dp))
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pontos.isEmpty()) item { Text("Nenhum ecoponto ativo encontrado.") }
            items(pontos, key = { it.id }) { ponto ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ponto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(ponto.endereco, ponto.bairro).joinToString(" · "))
                        val distancia = distanciaMetros(local, ponto)?.let { " · ${textoDistancia(it)}" }.orEmpty()
                        Text("Aceita ${ponto.nomeTipoResiduo} · ${textoStatus(ponto.status)}$distancia")
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${ponto.latitude},${ponto.longitude}"),
                                ),
                            )
                        }) { Text("Como chegar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaOsm(pontos: List<Ponto>, modifier: Modifier) {
    val context = LocalContext.current
    val mapa = remember {
        // Guarda os blocos do mapa no cache do app, sem exigir permissão de armazenamento
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            mapa.onDetach()
        }
    }

    LaunchedEffect(pontos) {
        mapa.overlays.clear()
        pontos.forEach { ponto ->
            mapa.overlays.add(
                Marker(mapa).apply {
                    position = GeoPoint(ponto.latitude!!, ponto.longitude!!)
                    title = ponto.nome
                    snippet = "Aceita ${ponto.nomeTipoResiduo}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                },
            )
        }
        pontos.firstOrNull()?.let { mapa.controller.setCenter(GeoPoint(it.latitude!!, it.longitude!!)) }
        mapa.invalidate()
    }

    AndroidView(
        factory = { mapa },
        modifier = modifier.semantics {
            contentDescription = "Mapa com ${pontos.size} ecopontos. A mesma lista está abaixo."
        },
    )
}
