package org.uciasistant.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.uciasistant.core.designsystem.UciAssistantTheme
import org.uciasistant.feature.alerts.AlertsOverviewScreen
import org.uciasistant.feature.calculators.CalculatorsHomeScreen
import org.uciasistant.feature.home.HomeScreen
import org.uciasistant.feature.handoff.HandoffScreen
import org.uciasistant.feature.protocols.ProtocolLibraryScreen

private enum class Destination {
    Home,
    Protocols,
    Alerts,
    Calculators,
    Handoff,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UciAssistantTheme {
                var destination by remember { mutableStateOf(Destination.Home) }

                SideEffect {
                    if (destination == Destination.Handoff) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                BackHandler(enabled = destination != Destination.Home) {
                    destination = Destination.Home
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (destination) {
                        Destination.Home -> HomeScreen(
                            onOpenProtocols = { destination = Destination.Protocols },
                            onOpenAlerts = { destination = Destination.Alerts },
                            onOpenCalculator = { destination = Destination.Calculators },
                            onOpenHandoff = { destination = Destination.Handoff },
                        )

                        Destination.Protocols -> ProtocolLibraryScreen(
                            onBack = { destination = Destination.Home },
                        )

                        Destination.Alerts -> AlertsOverviewScreen(
                            onBack = { destination = Destination.Home },
                        )

                        Destination.Calculators -> CalculatorsHomeScreen(
                            onBack = { destination = Destination.Home },
                        )

                        Destination.Handoff -> HandoffScreen(
                            onBack = { destination = Destination.Home },
                        )
                    }
                }
            }
        }
    }
}
