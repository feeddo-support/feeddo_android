package dev.feeddo.sdk.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.components.FeeddoNotificationBadge

object FeeddoInAppNotificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentOverlayView: ComposeView? = null
    private var dismissRunnable: Runnable? = null

    fun showNotification(
        activity: Activity,
        conversation: Conversation,
        theme: FeeddoTheme,
        durationMs: Long = 10000L
    ) {
        showBanner(
            activity = activity,
            title = conversation.displayName ?: "Support",
            body = conversation.lastMessagePreview ?: "New message received",
            theme = theme,
            durationMs = durationMs,
            onTap = {
                FeeddoChatActivity.launch(activity, conversation)
            }
        )
    }

    fun showSimpleNotification(
        activity: Activity,
        title: String,
        body: String,
        theme: FeeddoTheme,
        durationMs: Long = 10000L,
        onTap: (() -> Unit)? = null
    ) {
        showBanner(
            activity = activity,
            title = title,
            body = body,
            theme = theme,
            durationMs = durationMs,
            onTap = onTap
        )
    }

    private fun showBanner(
        activity: Activity,
        title: String,
        body: String,
        theme: FeeddoTheme,
        durationMs: Long,
        onTap: (() -> Unit)?
    ) {
        mainHandler.post {
            clearAll()

            if (activity.isFinishing || activity.isDestroyed) return@post

            // Target window decorView to overlay on top of all views/app bars, fallback to content FrameLayout
            val decorView = (activity.window.decorView as? ViewGroup)
                ?: activity.findViewById<ViewGroup>(android.R.id.content)
                ?: return@post

            val composeView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    var isVisible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        isVisible = true
                    }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 350)
                        ) + fadeIn(animationSpec = tween(durationMillis = 350)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 250)
                        ) + fadeOut(animationSpec = tween(durationMillis = 250))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            FeeddoNotificationBadge(
                                title = title,
                                body = body,
                                time = "Now",
                                theme = theme,
                                onTap = {
                                    isVisible = false
                                    mainHandler.postDelayed({ clearAll() }, 250)
                                    onTap?.invoke()
                                },
                                onDismiss = {
                                    isVisible = false
                                    mainHandler.postDelayed({ clearAll() }, 250)
                                }
                            )
                        }
                    }
                }
            }

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
            composeView.layoutParams = params
            ViewCompat.setElevation(composeView, 100f)

            decorView.addView(composeView, params)
            currentOverlayView = composeView

            val runnable = Runnable {
                clearAll()
            }
            dismissRunnable = runnable
            mainHandler.postDelayed(runnable, durationMs)
        }
    }

    fun clearAll() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = null
        currentOverlayView?.let { view ->
            val parent = view.parent as? ViewGroup
            parent?.removeView(view)
        }
        currentOverlayView = null
    }
}
