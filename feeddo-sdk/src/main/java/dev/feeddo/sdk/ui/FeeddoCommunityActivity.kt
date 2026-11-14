package dev.feeddo.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.FeeddoThemeHolder
import dev.feeddo.sdk.ui.screens.FeeddoTasksScreen

class FeeddoCommunityActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"

        fun launch(context: Context) {
            val intent = Intent(context, FeeddoCommunityActivity::class.java).apply {
                if (context !is ComponentActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }

        fun launchWithTask(context: Context, taskId: String) {
            val intent = Intent(context, FeeddoCommunityActivity::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                if (context !is ComponentActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                FeeddoInternal.instance.ensureUserInitialized()
            } catch (_: Exception) {}
        }

        val initialTaskId = intent.getStringExtra(EXTRA_TASK_ID)

        setContent {
            val theme = FeeddoThemeHolder.getTheme()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = theme.colors.background
            ) {
                FeeddoTasksScreen(
                    theme = theme,
                    initialTaskId = initialTaskId,
                    onBack = { finish() },
                    onNavigateToConversation = { convId ->
                        val conv = FeeddoInternal.instance.conversationService.conversations.value
                            .firstOrNull { it.id == convId }
                        if (conv != null) {
                            FeeddoChatActivity.launch(this, conv)
                        }
                    },
                    onNewTask = {
                        FeeddoChatActivity.launch(
                            context = this,
                            conversation = null,
                            initialMessage = "Describe about any feature you want to see in our app or report a bug.\nI will create it for you!"
                        )
                    }
                )
            }
        }
    }
}

