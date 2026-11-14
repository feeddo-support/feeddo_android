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
import com.google.gson.Gson
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.FeeddoThemeHolder
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.ui.screens.FeeddoChatScreen

class FeeddoChatActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_CONVERSATION_JSON = "extra_conversation_json"
        private const val EXTRA_INITIAL_MESSAGE = "extra_initial_message"

        fun launch(context: Context, conversation: Conversation? = null, initialMessage: String? = null) {
            val intent = Intent(context, FeeddoChatActivity::class.java).apply {
                if (conversation != null) {
                    putExtra(EXTRA_CONVERSATION_JSON, Gson().toJson(conversation))
                }
                if (initialMessage != null) {
                    putExtra(EXTRA_INITIAL_MESSAGE, initialMessage)
                }
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

        val convJson = intent.getStringExtra(EXTRA_CONVERSATION_JSON)
        val initialMessage = intent.getStringExtra(EXTRA_INITIAL_MESSAGE)
        val conversation = if (convJson != null) {
            try {
                Gson().fromJson(convJson, Conversation::class.java)
            } catch (_: Exception) {
                null
            }
        } else null

        setContent {
            val theme = FeeddoThemeHolder.getTheme()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = theme.colors.background
            ) {
                FeeddoChatScreen(
                    conversation = conversation,
                    initialMessage = initialMessage,
                    theme = theme,
                    onBack = { finish() },
                    onTaskTap = { taskId ->
                        FeeddoCommunityActivity.launchWithTask(this, taskId)
                    }
                )
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

