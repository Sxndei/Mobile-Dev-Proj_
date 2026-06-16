package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(
    viewModel: ServiceViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val activeChatId by viewModel.activeChatProviderId.collectAsState()
    val latestNotification by viewModel.notification.collectAsState()
    
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()
    val isFirestoreLive by viewModel.isFirestoreLive.collectAsState()

    // Navigation tab index within CUSTOMER role: 0=Browse, 1=Bookings, 2=Chats
    var customerTabIndex by remember { mutableStateOf(0) }

    // Navigation tab index within PROVIDER role: 0=Dashboard, 1=Chats
    var providerTabIndex by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentUser == null) {
            // Full screen login / sign-up flow
            AuthScreen(viewModel = viewModel)
        } else if (activeChatId != null) {
            // Direct chat window overlay (full bleed screen)
            DirectMessageWindow(
                viewModel = viewModel,
                providerId = activeChatId!!,
                senderRole = currentRole
            )
        } else {
            // Scaffold with Main Navigation Tabs
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WorkHistory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Local Services",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = if (isFirestoreLive) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                shape = RoundedCornerShape(50)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = firestoreStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        actions = {
                            var showProfileDialog by remember { mutableStateOf(false) }

                            if (showProfileDialog) {
                                AlertDialog(
                                    onDismissRequest = { showProfileDialog = false },
                                    title = {
                                        Text("My Profile", fontWeight = FontWeight.Bold)
                                    },
                                    text = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = currentUser?.name ?: "User",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = currentUser?.email ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (currentUser?.role == "PROVIDER") Icons.Default.Handyman else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (currentUser?.role == "PROVIDER") "Service Provider" else "Customer Member",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showProfileDialog = false }) {
                                            Text("Close")
                                        }
                                    },
                                    dismissButton = {
                                        Button(
                                            onClick = {
                                                showProfileDialog = false
                                                viewModel.logoutUser()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sign Out")
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                )
                            }

                            // Interactive profile bubble icon
                            IconButton(
                                onClick = { showProfileDialog = true },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("app_bar_profile_icon")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Immersive role change switch
                            FilledTonalButton(
                                onClick = {
                                    if (currentRole == "CUSTOMER") {
                                        viewModel.setRole("PROVIDER")
                                    } else {
                                        viewModel.setRole("CUSTOMER")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (currentRole == "PROVIDER") 
                                        MaterialTheme.colorScheme.tertiaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("role_switch_button")
                            ) {
                                Icon(
                                    imageVector = if (currentRole == "PROVIDER") Icons.Default.Storefront else Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentRole == "PROVIDER") "Provider Portal" else "Customer Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        if (currentRole == "CUSTOMER") {
                            NavigationBarItem(
                                selected = customerTabIndex == 0,
                                onClick = { customerTabIndex = 0 },
                                icon = { 
                                    Icon(
                                        imageVector = if (customerTabIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Browse"
                                    ) 
                                },
                                label = { Text("Browse") },
                                modifier = Modifier.testTag("nav_browse")
                            )

                            NavigationBarItem(
                                selected = customerTabIndex == 1,
                                onClick = { customerTabIndex = 1 },
                                icon = { 
                                    Icon(
                                        imageVector = if (customerTabIndex == 1) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = "My Bookings"
                                    ) 
                                },
                                label = { Text("My Bookings") },
                                modifier = Modifier.testTag("nav_bookings")
                            )

                            NavigationBarItem(
                                selected = customerTabIndex == 2,
                                onClick = { customerTabIndex = 2 },
                                icon = { 
                                    Icon(
                                        imageVector = if (customerTabIndex == 2) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble,
                                        contentDescription = "Inbox"
                                    ) 
                                },
                                label = { Text("Inbox") },
                                modifier = Modifier.testTag("nav_chats")
                            )
                        } else {
                            NavigationBarItem(
                                selected = providerTabIndex == 0,
                                onClick = { providerTabIndex = 0 },
                                icon = { 
                                    Icon(
                                        imageVector = if (providerTabIndex == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Business Board"
                                    ) 
                                },
                                label = { Text("My Business") },
                                modifier = Modifier.testTag("nav_provider_portal")
                            )

                            NavigationBarItem(
                                selected = providerTabIndex == 1,
                                onClick = { providerTabIndex = 1 },
                                icon = { 
                                    Icon(
                                        imageVector = if (providerTabIndex == 1) Icons.Filled.Mail else Icons.Outlined.Mail,
                                        contentDescription = "Client Messages"
                                    ) 
                                },
                                label = { Text("Client Chats") },
                                modifier = Modifier.testTag("nav_provider_chats")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (currentRole == "CUSTOMER") {
                        when (customerTabIndex) {
                            0 -> CustomerDashboard(viewModel = viewModel)
                            1 -> CustomerBookingsTab(viewModel = viewModel)
                            2 -> CustomerChatsTab(viewModel = viewModel)
                        }
                    } else {
                        when (providerTabIndex) {
                            0 -> ProviderPortal(viewModel = viewModel)
                            1 -> CustomerChatsTab(viewModel = viewModel) // Unified Chat List
                        }
                    }
                }
            }
        }

        // Beautiful Simulated Push Notification Floating Banner
        AnimatedVisibility(
            visible = latestNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            latestNotification?.let { notif ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isForProvider) 
                            MaterialTheme.colorScheme.tertiaryContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearNotification() }
                        .testTag("push_notification_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (notif.isForProvider) 
                                    MaterialTheme.colorScheme.onTertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (notif.isForProvider) 
                                    MaterialTheme.colorScheme.onTertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notif.isForProvider) 
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f) 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        
                        IconButton(onClick = { viewModel.clearNotification() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (notif.isForProvider) 
                                    MaterialTheme.colorScheme.onTertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Automatically clear notification after 4.5 seconds
                LaunchedEffect(notif.id) {
                    kotlinx.coroutines.delay(4500)
                    if (viewModel.notification.value?.id == notif.id) {
                        viewModel.clearNotification()
                    }
                }
            }
        }
    }
}
