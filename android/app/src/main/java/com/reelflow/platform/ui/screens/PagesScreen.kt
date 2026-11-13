package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.model.FacebookPage
import com.reelflow.platform.data.model.TokenHealth
import com.reelflow.platform.viewmodel.PlatformViewModel

/**
 * Pages screen: up to 500 pages — name, Graph Page ID, token status,
 * enabled toggle, token check, and the per-page master prompt editor.
 */
@Composable
fun PagesScreen(viewModel: PlatformViewModel) {
    val pages by viewModel.pages.collectAsState()
    var query by remember { mutableStateOf("") }
    var editingPromptPage by remember { mutableStateOf<FacebookPage?>(null) }
    var promptText by remember { mutableStateOf("") }

    val shown = pages.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
            it.pageId.contains(query) || it.niche.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Facebook Pages (${pages.size} / 500)", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Pages come from connected accounts. Each page has its own master prompt, sent verbatim to Veo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search pages") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Text("No pages yet — connect an account in Settings, or switch on demo mode there.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shown) { page ->
                    PageRow(
                        page = page,
                        hasToken = viewModel.hasPageToken(page.pageId),
                        onToggle = { viewModel.togglePageEnabled(page) },
                        onVerify = { viewModel.verifyPageToken(page) },
                        onEditPrompt = {
                            editingPromptPage = page
                            promptText = viewModel.getMasterPrompt(page.id) ?: ""
                        }
                    )
                }
            }
        }
    }

    editingPromptPage?.let { page ->
        AlertDialog(
            onDismissRequest = { editingPromptPage = null },
            title = { Text("Master prompt — ${page.name}") },
            text = {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("Master prompt (verbatim)") },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    maxLines = 12
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveMasterPrompt(page.id, promptText)
                    editingPromptPage = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingPromptPage = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PageRow(
    page: FacebookPage,
    hasToken: Boolean,
    onToggle: () -> Unit,
    onVerify: () -> Unit,
    onEditPrompt: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(page.name, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${page.pageId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (page.niche.isNotBlank()) Text(page.niche, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        when (page.tokenHealth) {
                            TokenHealth.ACTIVE -> "Token: Active"
                            TokenHealth.EXPIRING_SOON -> "Token: Expiring soon (${page.tokenExpiryDays}d)"
                            TokenHealth.EXPIRED -> "Token: Expired"
                        } + if (hasToken) " • saved on device" else " • no token saved",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = page.isEnabled, onCheckedChange = { onToggle() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onVerify) { Text("Verify token") }
                TextButton(onClick = onEditPrompt) { Text("Master prompt") }
            }
        }
    }
}
