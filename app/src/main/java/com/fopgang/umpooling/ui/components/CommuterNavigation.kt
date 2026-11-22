package com.fopgang.umpooling.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R

/** Shared floating navigation; compact windows keep every destination accessible. */
@Composable
fun CommuterNavigation(selected:String,navigate:(String)->Unit,modifier:Modifier=Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact=this.maxWidth<340.dp || LocalDensity.current.fontScale>1.15f
        Surface(shape=CircleShape,color=Color(0xFFFFFBF9).copy(alpha=.96f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                @Composable
                fun item(label:String,icon:Int,route:String) {
                    val active=route==selected
                    Surface(onClick={navigate(route)},modifier=Modifier.weight(when {active && !compact->1.8f;route=="notifications" && !compact->1.6f;else->1f})
                        .heightIn(min=if(active) 48.dp else 50.dp).semantics {this.selected=active;contentDescription=label},
                        shape=CircleShape,color=if(active) Color(0xFFFBD3CE) else Color.Transparent) {
                        if(active) Row(Modifier.padding(horizontal=8.dp,vertical=10.dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                            FigmaIcon(icon,Modifier.size(18.dp))
                            if(!compact) {
                                Spacer(Modifier.width(7.dp))
                                Text(label,fontSize=if(route=="notifications") 10.sp else 13.sp,lineHeight=16.sp,maxLines=1)
                            }
                        } else Column(Modifier.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.Center) {
                            FigmaIcon(icon,Modifier.size(19.dp))
                            Text(if(compact && route=="notifications") stringResource(R.string.nav_alerts) else label,fontSize=11.sp,lineHeight=15.sp,maxLines=1)
                        }
                    }
                }
                item(stringResource(R.string.home),R.drawable.profile_vector,"home")
                item(stringResource(R.string.trips),R.drawable.home_frame12,"my_trips")
                Box(Modifier.weight(1.1f),contentAlignment=Alignment.Center) {
                    Surface(onClick={navigate("voice")},shape=CircleShape,color=Color.Black) {
                        Box(Modifier.size(50.dp),contentAlignment=Alignment.Center) {
                            Image(painterResource(R.drawable.home_group),stringResource(R.string.voice_request),Modifier.size(18.dp,26.dp))
                        }
                    }
                }
                item(stringResource(R.string.notifications),R.drawable.home_frame13,"notifications")
                item(stringResource(R.string.profile),R.drawable.home_frame14,"profile")
            }
        }
    }
}
