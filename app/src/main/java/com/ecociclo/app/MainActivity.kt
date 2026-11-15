package com.ecociclo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecociclo.app.ui.EcoTema
import com.ecociclo.app.ui.InicioScreen
import com.ecociclo.app.ui.LoginScreen
import com.ecociclo.app.ui.MapaScreen
import com.ecociclo.app.ui.MarketplaceScreen
import com.ecociclo.app.ui.OperadorScreen
import com.ecociclo.app.ui.ValidarScreen

private enum class Aba(val rotulo: String) {
    INICIO("Início"), MAPA("Mapa"), VALIDAR("Validar"), LOJAS("Lojas"), OPERADOR("Operador")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcoTema {
                val vm: AppViewModel = viewModel()
                if (vm.sessao == null) LoginScreen(vm) else Principal(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Principal(vm: AppViewModel) {
    var aba by rememberSaveable { mutableAba() }
    val abas = Aba.entries.filter { it != Aba.OPERADOR || vm.sessao?.role == "ADMIN" }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.erro) {
        vm.erro?.let {
            snackbar.showSnackbar(it)
            vm.erro = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(aba.rotulo.takeIf { aba != Aba.INICIO } ?: "EcoCiclo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = vm::carregar, enabled = !vm.carregando) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar dados")
                    }
                    IconButton(onClick = vm::sair) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair da conta")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                abas.forEach { item ->
                    NavigationBarItem(
                        selected = aba == item,
                        onClick = { aba = item },
                        icon = { Icon(iconeDe(item), contentDescription = null) },
                        label = { Text(item.rotulo) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (vm.carregando) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (aba) {
                Aba.INICIO -> InicioScreen(vm, onValidar = { aba = Aba.VALIDAR })
                Aba.MAPA -> MapaScreen(vm)
                Aba.VALIDAR -> ValidarScreen(vm)
                Aba.LOJAS -> MarketplaceScreen(vm)
                Aba.OPERADOR -> OperadorScreen(vm)
            }
        }
    }
}

private fun mutableAba() = androidx.compose.runtime.mutableStateOf(Aba.INICIO)

@Composable
private fun iconeDe(aba: Aba): Painter = when (aba) {
    Aba.INICIO -> rememberVectorPainter(Icons.Default.Home)
    Aba.MAPA -> rememberVectorPainter(Icons.Default.Place)
    Aba.VALIDAR -> painterResource(R.drawable.ic_qr)
    Aba.LOJAS -> rememberVectorPainter(Icons.Default.ShoppingCart)
    Aba.OPERADOR -> rememberVectorPainter(Icons.Default.Build)
}
