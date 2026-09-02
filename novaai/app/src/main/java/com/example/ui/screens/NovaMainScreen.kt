package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.NovaAiViewModel
import com.example.ui.components.AuthProfileDialog
import com.example.ui.components.ChatHistoryDrawerContent
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.ChatTopBar
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.components.PlanCustomizingOverlay
import com.example.ui.components.SubscriptionPlanDialog
import com.example.ui.theme.NovaAccentCyan
import com.example.ui.theme.NovaAccentViolet
import com.example.ui.theme.NovaPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovaMainScreen(
    viewModel: NovaAiViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()

    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showModelSelector by viewModel.showModelSelector.collectAsStateWithLifecycle()
    val showPlanDialog by viewModel.showPlanDialog.collectAsStateWithLifecycle()
    val customizingPlan by viewModel.customizingPlan.collectAsStateWithLifecycle()

    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(currentMessages.size, isGenerating) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.size - 1)
        }
    }

    // Display error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawerContent(
                sessions = allSessions,
                currentSessionId = currentSessionId,
                currentPlan = currentPlan,
                onSelectSession = { id ->
                    viewModel.selectSession(id)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onClearAllSessions = {
                    coroutineScope.launch {
                        viewModel.repository.clearAllHistory()
                    }
                },
                onOpenPlanDialog = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setShowPlanDialog(true)
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                ChatTopBar(
                    selectedModel = selectedModel,
                    currentUser = currentUser,
                    currentPlan = currentPlan,
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onOpenModelSelector = { viewModel.setShowModelSelector(true) },
                    onOpenPlanSelector = { viewModel.setShowPlanDialog(true) },
                    onOpenProfile = { viewModel.setShowProfileDialog(true) },
                    onNewChat = { viewModel.startNewChat() }
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = inputText,
                    onInputTextChanged = { viewModel.onInputTextChanged(it) },
                    selectedImageUri = selectedImageUri,
                    onImageSelected = { viewModel.onImageSelected(it) },
                    onClearImage = { viewModel.onClearImage() },
                    onSendMessage = { viewModel.sendMessage() },
                    isGenerating = isGenerating
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentMessages.isEmpty()) {
                    // Empty state with welcome hero & suggestion chips
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(NovaPrimary, NovaAccentViolet, NovaAccentCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "NovaAI",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "NovaAI Size Nasıl Yardımcı Olabilir?",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        // Plan & Model info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        ) {
                            Surface(
                                color = NovaPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⚡ Qwen 3.6 27B & Vision Aktif",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = NovaPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = NovaAccentViolet.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "👑 ${currentPlan.name}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = NovaAccentViolet,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Quick Starter Suggestion Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SuggestionPromptCard(
                                icon = Icons.Default.Image,
                                title = "Görsel Analizi",
                                subtitle = "Fotoğraf yükle & detaylı incele",
                                onClick = {
                                    viewModel.onInputTextChanged("Bu görseldeki ana ögeleri analiz et ve Türkçe detaylı bilgi ver.")
                                }
                            )
                            SuggestionPromptCard(
                                icon = Icons.Default.Psychology,
                                title = "Kavram Açıklaması",
                                subtitle = "Kuantum bilgisayarlar nedir?",
                                onClick = {
                                    viewModel.onInputTextChanged("Kuantum bilgisayarların çalışma prensibini günlük hayat benzetmeleriyle anlat.")
                                }
                            )
                            SuggestionPromptCard(
                                icon = Icons.Default.Code,
                                title = "Kotlin & Compose",
                                subtitle = "StateFlow ve Room örneği",
                                onClick = {
                                    viewModel.onInputTextChanged("Kotlin Jetpack Compose ve Room kullanarak StateFlow akışı ile çalışan temiz bir kod örneği yaz.")
                                }
                            )
                            SuggestionPromptCard(
                                icon = Icons.Default.Lightbulb,
                                title = "Yaratıcı Fikirler",
                                subtitle = "Yenilikçi mobil uygulama fikirleri",
                                onClick = {
                                    viewModel.onInputTextChanged("Yapay zeka destekli, mobil cihazlarda fark yaratacak 4 yenilikçi uygulama fikri öner.")
                                }
                            )
                        }
                    }
                } else {
                    // Chat Messages List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_message_list"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 8.dp,
                            bottom = 12.dp
                        )
                    ) {
                        items(currentMessages, key = { it.id }) { message ->
                            ChatMessageItem(
                                message = message,
                                onRetry = {
                                    viewModel.retryLastMessage(
                                        prompt = message.content,
                                        imageUri = message.imageUri?.let { Uri.parse(it) }
                                    )
                                },
                                onImageClick = { previewImageUrl = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showProfileDialog) {
        AuthProfileDialog(
            currentUser = currentUser,
            currentPlan = currentPlan,
            onOpenPlanDialog = {
                viewModel.setShowPlanDialog(true)
            },
            onSignIn = { username, email, password ->
                viewModel.signIn(
                    username = username,
                    email = email,
                    password = password,
                    onSuccess = {
                        viewModel.setShowProfileDialog(false)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Hoş geldiniz, $username!")
                        }
                    },
                    onError = { errorMsg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMsg)
                        }
                    }
                )
            },
            onSignOut = {
                viewModel.signOut()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Çıkış yapıldı.")
                }
            },
            onDismiss = { viewModel.setShowProfileDialog(false) }
        )
    }

    if (showModelSelector) {
        ModelSelectorDialog(
            selectedModel = selectedModel,
            onModelSelected = { viewModel.onSelectModel(it) },
            onDismiss = { viewModel.setShowModelSelector(false) }
        )
    }

    if (showPlanDialog) {
        SubscriptionPlanDialog(
            currentPlan = currentPlan,
            onSelectPlanAndBuy = { plan ->
                viewModel.selectPlanAndBuy(plan)
            },
            onDismiss = { viewModel.setShowPlanDialog(false) }
        )
    }

    // Plan Customizing Overlay (Shown after buying / selecting plan from brandofpaper.ikas.shop)
    customizingPlan?.let { plan ->
        PlanCustomizingOverlay(
            plan = plan,
            onCustomizationComplete = {
                viewModel.onPlanCustomizationFinished()
            }
        )
    }

    // Fullscreen Image Preview Dialog
    previewImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Büyütülmüş Görsel",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionPromptCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .width(160.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NovaPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
