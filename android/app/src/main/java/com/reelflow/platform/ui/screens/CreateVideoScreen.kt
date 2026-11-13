package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.model.FacebookPage
import com.reelflow.platform.data.model.VeoModel
import com.reelflow.platform.viewmodel.PlatformViewModel

@Composable
fun CreateVideoScreen(viewModel: PlatformViewModel) {
    val pages by viewModel.pages.collectAsState()
    val veoKeySet by viewModel.veoKeySet.collectAsState()
    val selectedModel by viewModel.selectedVeoModel.collectAsState()

    var selectedPage by remember { mutableStateOf<FacebookPage?>(null) }
    var useMasterPrompt by remember { mutableStateOf(true) }
    var customPrompt by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var expandedPageMenu by remember { mutableStateOf(false) }

    val enabledPages = pages.filter { it.isEnabled }
    val masterPrompt = selectedPage?.let { viewModel.getMasterPrompt(it.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Create video", style = MaterialTheme.typography.headlineMedium)
            if (!veoKeySet) {
                Text(
                    "Veo API key nahi hai — Settings me key add karo, warna video nahi banegi.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            SectionTitle("Target page (one video → one page)")
            Box {
                OutlinedButton(onClick = { expandedPageMenu = true }) {
                    Text(selectedPage?.name ?: "Select a page")
                }
                DropdownMenu(expanded = expandedPageMenu, onDismissRequest = { expandedPageMenu = false }) {
                    enabledPages.take(50).forEach { page ->
                        DropdownMenuItem(
                            text = { Text(page.name) },
                            onClick = { selectedPage = page; expandedPageMenu = false }
                        )
                    }
                    if (enabledPages.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No pages — connect an account first") },
                            onClick = { expandedPageMenu = false }
                        )
                    }
                }
            }
        }
        item {
            SectionTitle("Veo model")
            VeoModel.entries.forEach { model ->
                Row {
                    RadioButton(
                        selected = selectedModel == model,
                        onClick = { viewModel.selectVeoModel(model) }
                    )
                    Text("${model.label} — ${model.approxCostPer8s}")
                }
            }
            Text("Veo generates 8-second clips.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionTitle("Aspect ratio")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = aspectRatio == "9:16", onClick = { aspectRatio = "9:16" }, label = { Text("9:16 vertical") })
                FilterChip(selected = aspectRatio == "16:9", onClick = { aspectRatio = "16:9" }, label = { Text("16:9") })
            }
        }
        item {
            SectionTitle("Prompt (sent 100% verbatim to Veo)")
            Row {
                FilterChip(selected = useMasterPrompt, onClick = { useMasterPrompt = true }, label = { Text("Page master prompt") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !useMasterPrompt, onClick = { useMasterPrompt = false }, label = { Text("Custom prompt") })
            }
            if (useMasterPrompt) {
                Text(
                    masterPrompt ?: "Is page ka master prompt save nahi hai — Pages screen me likho.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 6
                )
            }
        }
        item {
            val canGenerate = selectedPage != null && veoKeySet &&
                ((useMasterPrompt && !masterPrompt.isNullOrBlank()) || (!useMasterPrompt && customPrompt.isNotBlank()))
            Button(
                onClick = {
                    val page = selectedPage ?: return@Button
                    if (useMasterPrompt) {
                        viewModel.createVideoFromPagePrompt(page, selectedModel)
                    } else {
                        viewModel.createVideoWithPrompt(page, customPrompt, selectedModel)
                    }
                },
                enabled = canGenerate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate video")
            }
            Text(
                "Generation takes a few minutes. The video appears in Library when ready.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
