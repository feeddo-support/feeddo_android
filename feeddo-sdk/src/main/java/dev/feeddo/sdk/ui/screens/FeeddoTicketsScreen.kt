package dev.feeddo.sdk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Ticket
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.components.TicketCard
import dev.feeddo.sdk.ui.components.TicketDetailsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoTicketsScreen(
    theme: FeeddoTheme,
    onBack: () -> Unit,
    onNavigateToConversation: ((String) -> Unit)? = null
) {
    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTicketId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val userId = FeeddoInternal.instance.cachedUserId
            if (userId != null) {
                val list = FeeddoInternal.instance.apiService.getTickets(userId)
                tickets = list
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    if (selectedTicketId != null) {
        TicketDetailsSheet(
            ticketId = selectedTicketId!!,
            theme = theme,
            onDismiss = { selectedTicketId = null },
            onNavigateToConversation = onNavigateToConversation
        )
    }

    Scaffold(
        containerColor = theme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tickets",
                        color = theme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.colors.iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.colors.appBarBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = theme.colors.primary
                )
            } else if (tickets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = theme.colors.textSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tickets yet",
                        color = theme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When complex issues require tracking, support tickets will appear here.",
                        color = theme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tickets) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            theme = theme,
                            onTap = { selectedTicketId = ticket.id }
                        )
                    }
                }
            }
        }
    }
}

