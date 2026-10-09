package com.nagmo.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.data.NagRepository
import com.nagmo.app.ui.screens.EditNagScreen
import com.nagmo.app.ui.screens.Filter
import com.nagmo.app.ui.screens.HomeScreen
import com.nagmo.app.ui.screens.SettingsScreen
import com.nagmo.app.ui.screens.StatsScreen
import com.nagmo.app.ui.theme.NagmoTheme
import com.nagmo.app.ui.theme.SyncSystemBars

sealed interface Route {
    data object Home : Route
    data class Edit(val nagId: Int? = null, val prefill: String = "") : Route
    data object Settings : Route
    data object Stats : Route
}

class MainActivity : ComponentActivity() {
    private var route by mutableStateOf<Route>(Route.Home)
    private var filter by mutableStateOf(Filter.TODO)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NagRepository.init(this)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            NagmoTheme {
                SyncSystemBars()
                val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    NagRepository.refreshSurfaces()
                }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                BackHandler(enabled = route != Route.Home) { route = Route.Home }

                AnimatedContent(
                    targetState = route,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "route",
                ) { r ->
                    when (r) {
                        Route.Home -> HomeScreen(
                            filter = filter,
                            onFilterChange = { filter = it },
                            onAdd = { route = Route.Edit() },
                            onOpen = { route = Route.Edit(it) },
                            onSettings = { route = Route.Settings },
                            onStats = { route = Route.Stats },
                        )
                        is Route.Edit -> EditNagScreen(
                            nagId = r.nagId,
                            prefill = r.prefill,
                            onClose = { route = Route.Home },
                        )
                        Route.Settings -> SettingsScreen(onBack = { route = Route.Home })
                        Route.Stats -> StatsScreen(onBack = { route = Route.Home })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh "in 5 min" style text on widgets and the lock-screen list.
        NagRepository.refreshSurfaces()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_ADD_NAG -> route = Route.Edit()
            ACTION_OPEN_NAG -> {
                val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
                route = if (NagRepository.get(id) != null) Route.Edit(id) else Route.Home
            }
            ACTION_SHOW_TODAY -> {
                filter = Filter.TODAY
                route = Route.Home
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                route = Route.Edit(prefill = subject.ifBlank { text }.take(200) +
                    if (subject.isNotBlank() && text.isNotBlank()) "\n$text" else "")
            }
        }
    }

    companion object {
        const val ACTION_ADD_NAG = "com.nagmo.app.action.ADD_NAG"
        const val ACTION_OPEN_NAG = "com.nagmo.app.action.OPEN_NAG"
        const val ACTION_SHOW_TODAY = "com.nagmo.app.action.SHOW_TODAY"
    }
}
