package com.fopgang.umpooling.safety

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.booking.*
import kotlinx.coroutines.*

@Composable
fun EmergencyContactScreen(owner:String,back:()->Unit) {
 val context=LocalContext.current
 val prefs=remember(owner){context.getSharedPreferences("emergency_"+owner,0)}
 var phone by rememberSaveable(owner){mutableStateOf(prefs.getString("phone","").orEmpty())}
 var notice by remember{mutableStateOf<String?>(null)}
 var saving by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 RidePage {
  Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   RideHeader("Emergency contact",back)
   Text("Saved privately on this device for this account. SOS opens a message for you to review and send.")
   OutlinedTextField(phone,{phone=it},label={Text("Phone number, including country code")},enabled=!saving)
   RideAction("Save contact",enabled=!saving) {
    val clean=phone.filter{it.isDigit() || it=='+'}
    if(clean.count{it.isDigit()}<5) notice="Enter a valid phone number."
    else {
     saving=true
     scope.launch {
      try {
       val saved=withContext(Dispatchers.IO){prefs.edit().putString("phone",clean).commit()}
       notice=if(saved) "Contact saved." else "Unable to save contact."
      } finally {saving=false}
     }
    }
   }
   notice?.let{Text(it)}
  }
 }
}
