package com.fopgang.umpooling.safety

import com.fopgang.umpooling.coordination.CoordinationRepository
import com.fopgang.umpooling.coordination.TripRecord

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.data.TripOffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun LiveSosPanel(repository:CoordinationRepository,trip:TripRecord,offer:TripOffer,back:()->Unit,prepared:Boolean=false) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 val store=remember(repository.owner) {context.getSharedPreferences("emergency_"+repository.owner,0)}
 var contact by rememberSaveable(trip.id){mutableStateOf(store.getString("phone","").orEmpty())}
 var location by rememberSaveable(trip.id){mutableStateOf("")}
 var ready by rememberSaveable(trip.id) {mutableStateOf(prepared)}
 var error by remember {mutableStateOf<String?>(null)}
 Text("Emergency · Hold for three seconds")
 OutlinedTextField(contact,{contact=it;scope.launch {val saved=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){store.edit().putString("phone",it).commit()};if(!saved) error="Unable to save contact."}},label={Text("Emergency contact phone")})
 OutlinedTextField(location,{location=it},label={Text("Current location (if known)")})
 Surface(Modifier.fillMaxWidth().height(80.dp).pointerInput(trip.id) {
  detectTapGestures(onPress={
   val hold=scope.launch {delay(3000);ready=true}
   try {tryAwaitRelease()} finally {hold.cancel()}
  })
 },color=MaterialTheme.colorScheme.error) {Text("Hold SOS for 3 seconds",Modifier.padding(24.dp),color=MaterialTheme.colorScheme.onError)}
 error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 if(ready) AlertDialog(onDismissRequest={ready=false},title={Text("Prepare emergency message")},
  text={Text("This opens another application. Review the message and send it there. Opening the composer does not confirm delivery.")},
  dismissButton={TextButton({ready=false}){Text("Cancel")}},
  confirmButton={
   Row {
    fun open(whatsapp:Boolean) {
     ready=false
     if(contact.filter{it.isDigit()}.length<5){error="Enter an emergency contact.";return}
     val body="Emergency assistance requested. Trip ${trip.id}, ${offer.originZone.orEmpty()} to ${offer.destinationZone.orEmpty()}, vehicle ${trip.plate.orEmpty()}. Current location: ${location.ifBlank{"unavailable"}}."
     val intent=if(whatsapp) Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+contact.filter{it.isDigit()}+"?text="+Uri.encode(body)))
      else Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+contact.filter{it.isDigit() || it=='+'})).putExtra("sms_body",body)
     try {repository.checkOwner();context.startActivity(intent)}
     catch(_:Exception){error="Unable to open the composer. Check your session and installed messaging apps."}
    }
    TextButton({open(false)}){Text("SMS")}
    TextButton({open(true)}){Text("WhatsApp")}
   }
  })
}
