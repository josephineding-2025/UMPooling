package com.fopgang.umpooling.chat

import com.fopgang.umpooling.coordination.ChatGateway
import com.fopgang.umpooling.coordination.ConversationContext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fopgang.umpooling.data.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class LiveChatState(
 val messages:List<Message> = emptyList(),val sending:Boolean=false,val loading:Boolean=false,
 val error:String?=null,val failedId:String?=null,val failedText:String?=null,val acknowledgement:Int=0)
class ChatViewModel(private val gateway:ChatGateway,private val context:ConversationContext,scopeOverride:CoroutineScope?=null):ViewModel() {
 private val scope=scopeOverride ?: viewModelScope
 private val mutable=MutableStateFlow(LiveChatState())
 val state=mutable.asStateFlow()
 suspend fun refresh() {
  if(mutable.value.loading) return
  mutable.update{it.copy(loading=true)}
  try {gateway.messages(context).fold({rows->mutable.update{it.copy(messages=rows,loading=false,error=if(it.failedText==null)null else it.error)}},
   {mutable.update{it.copy(loading=false,error="Unable to refresh messages. Check your connection.")}})
  } finally {mutable.update{it.copy(loading=false)}}
 }
 fun send(text:String) {
  if(mutable.value.sending || text.isBlank()) return
  val old=mutable.value
  val id=if(old.failedText==text && old.failedId!=null) old.failedId else UUID.randomUUID().toString()
  mutable.update{it.copy(sending=true,error=null)}
  scope.launch {
   try {
    gateway.send(context,id,text).fold({
     mutable.update{it.copy(failedId=null,failedText=null,sending=false,error=null,acknowledgement=it.acknowledgement+1)}
     refresh()
    },{mutable.update{it.copy(sending=false,failedId=id,failedText=text,error="Message not sent. Tap Send to retry.")}})
   } catch(e:CancellationException){throw e}
   catch(_:Exception){mutable.update{it.copy(sending=false,failedId=id,failedText=text,error="Message not sent. Tap Send to retry.")}}
  }
 }
}
