package com.fopgang.umpooling.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.time.Clock
import java.time.Duration

/** Presentation selection never substitutes for route/capacity matching. */
fun FindRideCriteria.isImmediate(clock: Clock = Clock.system(CAMPUS_ZONE)): Boolean {
    val seconds = Duration.between(clock.instant(), departure().toInstant())
    return !seconds.isNegative && seconds <= Duration.ofMinutes(30)
}

@Composable
internal fun FlowDestination(title: String, back: () -> Unit, description: String, actions: List<Pair<String,()->Unit>>, navigation: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize()) {
    RidePage {
        Column(Modifier.padding(horizontal=24.dp).padding(bottom=if(navigation==null) 32.dp else 120.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            RideHeader(title,back)
            Glass { Text(description) }
            actions.forEach { (label, action) -> RideAction(label, action=action) }
        }
    }
    if(navigation!=null) Box(Modifier.align(Alignment.BottomCenter).padding(horizontal=14.dp).padding(bottom=30.dp)) { navigation() }
    }
}
