package dev.feeddo.sdk.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.FeeddoThemeHolder
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.ui.screens.*

sealed class FeeddoScreenState {
    object Home : FeeddoScreenState()
    data class Chat(val conversation: Conversation? = null, val initialMessage: String? = null) : FeeddoScreenState()
    object Messages : FeeddoScreenState()
    object Tickets : FeeddoScreenState()
    data class Tasks(val initialTaskId: String? = null) : FeeddoScreenState()
}

class FeeddoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                FeeddoInternal.instance.ensureUserInitialized()
            } catch (_: Exception) {}
        }
        FeeddoInternal.instance.connectWebSocket()

        setContent {
            val theme = FeeddoThemeHolder.getTheme()
            val navigationStack = remember { mutableStateListOf<FeeddoScreenState>(FeeddoScreenState.Home) }

            val currentScreen = navigationStack.lastOrNull() ?: FeeddoScreenState.Home

            BackHandler(enabled = navigationStack.size > 1) {
                navigationStack.removeAt(navigationStack.size - 1)
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = theme.colors.background
            ) {
                when (val screen = currentScreen) {
                    is FeeddoScreenState.Home -> {
                        FeeddoHomeScreen(
                            theme = theme,
                            onClose = { finish() },
                            onSendMessageTap = {
                                navigationStack.add(FeeddoScreenState.Chat(null))
                            },
                            onConversationTap = { conv ->
                                navigationStack.add(FeeddoScreenState.Chat(conv))
                            },
                            onMessagesTap = {
                                navigationStack.add(FeeddoScreenState.Messages)
                            },
                            onTicketsTap = {
                                navigationStack.add(FeeddoScreenState.Tickets)
                            },
                            onCommunityBoardTap = {
                                navigationStack.add(FeeddoScreenState.Tasks())
                            },
                            onTaskTap = { taskId ->
                                navigationStack.add(FeeddoScreenState.Tasks(taskId))
                            },
                            onTicketTap = {
                                navigationStack.add(FeeddoScreenState.Tickets)
                            }
                        )
                    }

                    is FeeddoScreenState.Chat -> {
                        FeeddoChatScreen(
                            conversation = screen.conversation,
                            initialMessage = screen.initialMessage,
                            theme = theme,
                            onBack = {
                                if (navigationStack.size > 1) {
                                    navigationStack.removeAt(navigationStack.size - 1)
                                } else {
                                    finish()
                                }
                            },
                            onTaskTap = { taskId ->
                                navigationStack.add(FeeddoScreenState.Tasks(taskId))
                            },
                            onTicketTap = {
                                navigationStack.add(FeeddoScreenState.Tickets)
                            }
                        )
                    }

                    is FeeddoScreenState.Messages -> {
                        FeeddoMessagesScreen(
                            theme = theme,
                            onBack = {
                                navigationStack.removeAt(navigationStack.size - 1)
                            },
                            onConversationTap = { conv ->
                                navigationStack.add(FeeddoScreenState.Chat(conv))
                            },
                            onNewConversation = {
                                navigationStack.add(FeeddoScreenState.Chat(null))
                            }
                        )
                    }

                    is FeeddoScreenState.Tickets -> {
                        FeeddoTicketsScreen(
                            theme = theme,
                            onBack = {
                                navigationStack.removeAt(navigationStack.size - 1)
                            },
                            onNavigateToConversation = { convId ->
                                val conv = FeeddoInternal.instance.conversationService.conversations.value
                                    .firstOrNull { it.id == convId }
                                navigationStack.add(FeeddoScreenState.Chat(conv))
                            }
                        )
                    }

                    is FeeddoScreenState.Tasks -> {
                        FeeddoTasksScreen(
                            theme = theme,
                            initialTaskId = screen.initialTaskId,
                            onBack = {
                                navigationStack.removeAt(navigationStack.size - 1)
                            },
                            onNavigateToConversation = { convId ->
                                val conv = FeeddoInternal.instance.conversationService.conversations.value
                                    .firstOrNull { it.id == convId }
                                navigationStack.add(FeeddoScreenState.Chat(conv))
                            },
                            onNewTask = {
                                navigationStack.add(
                                    FeeddoScreenState.Chat(
                                        conversation = null,
                                        initialMessage = "Describe about any feature you want to see in our app or report a bug.\nI will create it for you!"
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && FeeddoInternal.isInitialized) {
            FeeddoInternal.instance.disconnectWebSocket()
        }
    }
}

