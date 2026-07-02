package com.portfello.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.ui.common.canUseBiometrics
import com.portfello.ui.common.showBiometricPrompt

@Composable
fun LockScreen(viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isSetup = viewModel.isSetup

    val context = LocalContext.current
    val biometricsUsable = remember {
        !isSetup && viewModel.biometricEnabled && canUseBiometrics(context)
    }
    val launchBiometric = launchBiometric@{
        val activity = context as? FragmentActivity ?: return@launchBiometric
        val cipher = viewModel.biometricDecryptCipher() ?: return@launchBiometric
        showBiometricPrompt(activity, cipher, "Odblokuj Portfello") { authorized ->
            viewModel.onBiometricUnlock(authorized)
        }
    }
    if (biometricsUsable) {
        LaunchedEffect(Unit) { launchBiometric() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Portfello",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "AES-256",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = when {
                isSetup && !state.isConfirmStep -> "Utwórz PIN (min. 4 cyfry)"
                isSetup && state.isConfirmStep -> "Potwierdź PIN"
                else -> "Wprowadź PIN"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(32.dp))

        val displayPin = if (state.isConfirmStep) state.confirmPin else state.pin
        PinDots(length = displayPin.length, max = maxOf(displayPin.length, 4))

        Spacer(Modifier.height(8.dp))

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (state.lockoutEndTime > System.currentTimeMillis()) {
            Text(
                text = "Zbyt wiele prób. Poczekaj.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        if (state.isProcessing) {
            CircularProgressIndicator()
        } else {
            NumPad(
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onConfirm = viewModel::onConfirm
            )
            if (biometricsUsable) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { launchBiometric() }) {
                    Text("Odblokuj biometrycznie")
                }
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(max) { i ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < length) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun NumPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    NumPadButton(label = digit.toString()) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FilledTonalButton(
                onClick = onBackspace,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) {
                Text("←", style = MaterialTheme.typography.headlineSmall)
            }
            NumPadButton(label = "0") { onDigit('0') }
            Button(
                onClick = onConfirm,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("✓", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun NumPadButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}
