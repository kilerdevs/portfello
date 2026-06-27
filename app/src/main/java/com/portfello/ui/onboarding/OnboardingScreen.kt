package com.portfello.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portfello.data.AppPrefs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(prefs: AppPrefs, onDone: () -> Unit) {
    var selected by remember { mutableStateOf(prefs.baseCurrency) }
    val currencies = listOf("PLN", "EUR", "USD", "GBP", "CHF", "CZK", "SEK", "NOK")

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Portfello", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Wybierz walutę bazową", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Wszystkie aktywa będą przeliczane na tę walutę",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            currencies.forEach { cur ->
                FilterChip(
                    selected = selected == cur,
                    onClick = { selected = cur },
                    label = { Text(cur) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = {
            prefs.baseCurrency = selected
            prefs.onboardingComplete = true
            onDone()
        }, Modifier.fillMaxWidth()) {
            Text("Kontynuuj")
        }
    }
}
