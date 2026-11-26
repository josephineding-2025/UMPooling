package com.fopgang.umpooling.coordination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class CoordinationState(val data:CoordinationData?=null,val loading:Boolean=false,val busy:Boolean=false,val error:String?=null)
class CoordinationViewModel(private val repository:CoordinationRepository):ViewModel() {
 private val mutable=MutableStateFlow(CoordinationState())
 val state=mutable.asStateFlow()
 fun refresh() {
  if(mutable.value.loading || mutable.value.busy) return
  viewModelScope.launch {
   mutable.update {it.copy(loading=true,error=null)}
   repository.load().fold({data->mutable.update {it.copy(data=data,loading=false)}},
    {mutable.update {it.copy(loading=false,error="Unable to load trip records. Check your connection and retry.")}})
  }
 }
 fun act(action:TripAction,id:String) {
  if(mutable.value.busy) return
  mutable.update {it.copy(busy=true,error=null)}
  viewModelScope.launch {
   repository.act(action,id).fold({
    mutable.update {it.copy(busy=false)}
    refresh()
   },{mutable.update {it.copy(busy=false,error="Trip update failed. The server may reject this transition or its lifecycle RPC may not be installed. Refresh and retry.")}})
  }
 }
 fun cancelBooking(id:String) {
  if(mutable.value.busy) return
  mutable.update {it.copy(busy=true,error=null)}
  viewModelScope.launch {
   repository.cancelBooking(id).fold({
    mutable.update {it.copy(busy=false)};refresh()
   },{mutable.update {it.copy(busy=false,error="Cancellation failed. Refresh the ticket and retry.")}})
  }
 }

}
