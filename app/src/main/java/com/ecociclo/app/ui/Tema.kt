package com.ecociclo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Claro = lightColorScheme(
    primary = Color(0xFF1B6B45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1C8),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDFA0),
    onSecondaryContainer = Color(0xFF261A00),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF181D19),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF99D5AD),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFFB4F1C8),
    secondary = Color(0xFFEFC048),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDFA0),
    background = Color(0xFF101511),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF101511),
    onSurface = Color(0xFFDFE4DD),
)

@Composable
fun EcoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro, content = conteudo)
}

private val numero = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun BigDecimal.formatado(): String = numero.format(this)

fun dataCurta(iso: String): String = runCatching {
    LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}.getOrDefault(iso)

/** Aceita vírgula ou ponto; retorna null se não for um valor positivo com até 2 casas. */
fun lerValor(texto: String): BigDecimal? =
    texto.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it.scale() <= 2 }
