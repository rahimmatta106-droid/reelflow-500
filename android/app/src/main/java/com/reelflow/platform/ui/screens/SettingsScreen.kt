package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.repository.PublishingMode
import com.reelflow.platform.viewmodel.PlatformViewModel

/**
 * Settings: connected accounts (Facebook Login button + Token se Connect),
 * Veo API key, publishing mode (demo/real), and system logs.
 *
 * NOTE: there is deliberately NO email+password login field anywhere —
 * Facebook forbids password login in third-party apps.
 */
@Composable
fun SettingsScreen(viewModel: PlatformViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val mode by viewModel.publishingMode.collectAsState()
    val veoKeySet by viewModel.veoKeySet.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var tokenText by remember { mutableStateOf("") }
    var showTokenDialog by remember { mutableStateOf(false) }
    var veoKeyText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            SectionTitle("Connected accounts (${accounts.size} / 50)")
            if (accounts.isEmpty()) {
                Text("No account connected yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                accounts.forEach { acc ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(acc.name)
                                Text("${acc.pageCount} pages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.removeAccount(acc.id) }) { Text("Remove") }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.requestFacebookLogin() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Login with Facebook") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showTokenDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Token se Connect") }
            Text(
                "Paste a User Token from Graph API Explorer. Password kabhi nahi — token sirf page access deta hai.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            SectionTitle("Veo API key")
            Text(
                if (veoKeySet) "Key saved (encrypted on device)." else "Key not set — video generation needs it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = veoKeyText,
                onValueChange = { veoKeyText = it },
                label = { Text("Google AI Studio API key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { viewModel.saveVeoKey(veoKeyText); veoKeyText = "" }) {
                Text("Save Veo key")
            }
        }
        item {
            SectionTitle("Publishing mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == PublishingMode.MOCK,
                    onClick = { viewModel.setPublishingMode(PublishingMode.MOCK) },
                    label = { Text("Demo (mock)") }
                )
                FilterChip(
                    selected = mode == PublishingMode.REAL,
                    onClick = { viewModel.setPublishingMode(PublishingMode.REAL) },
                    label = { Text("Real (Meta API)") }
                )
            }
            Text(
                "Demo mode simulates publishing. Real mode uploads to Facebook with saved page tokens.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { SectionTitle("System logs") }
        items(logs.take(30)) { log ->
            Text("[${log.level}] ${log.tag}: ${log.message}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Token se Connect") },
            text = {
                Column {
                    Text("Graph API Explorer se User Token paste karo (password kabhi nahi). Is se us account ke sare pages aik saath connect ho jayenge.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        label = { Text("User Token paste karo...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.requestTokenLogin(tokenText)
                    tokenText = ""
                    showTokenDialog = false
                }) { Text("Connect") }
            },
            dismissButton = { TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") } }
        )
    }
}
