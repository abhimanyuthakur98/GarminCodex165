package com.garmincodex.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal fun DashboardPreferences.placeHome(id:String,target:String):DashboardPreferences {
    val current=normalized()
    val order=current.homeOrder.toMutableList()
    val destination=order.indexOf(target)
    if(id==target || id !in order || destination<0)return current
    order.remove(id);order.add(destination,id)
    return current.copy(homeOrder=order)
}

/** Long-press recognition sits above child click handlers; ordinary taps still open details. */
@Composable internal fun HoldToArrange(onArrange:()->Unit,content:@Composable ColumnScope.()->Unit) {
    val arrange by rememberUpdatedState(onArrange)
    val haptic=LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().semantics {
        customActions=listOf(CustomAccessibilityAction("Arrange Home"){arrange();true})
    }.pointerInput(Unit) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            if(awaitLongPressOrCancellation(down.id)!=null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress);arrange()
                do {
                    val event=awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach{it.consume()}
                } while(event.changes.any{it.pressed})
            }
        }
    },verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}

@Composable internal fun HomeArrange(preferences:DashboardPreferences,day:ConnectedDay?,onSave:(DashboardPreferences)->Unit,onManage:()->Unit,onClose:()->Unit) {
    val p=LocalPulseStyle.current
    val prefs=preferences.normalized()
    val latest by rememberUpdatedState(prefs)
    val save by rememberUpdatedState(onSave)
    val cards=prefs.homeOrder.filter{it !in prefs.homeHidden}
    val grid=rememberLazyGridState()
    val haptic=LocalHapticFeedback.current
    val density=LocalDensity.current
    var dragging by remember{mutableStateOf<String?>(null)}
    var pointer by remember{mutableStateOf(Offset.Zero)}
    var anchor by remember{mutableStateOf(Offset.Zero)}
    var dragWidth by remember{mutableIntStateOf(0)}
    var target by remember{mutableStateOf<String?>(null)}
    val edge=with(density){56.dp.toPx()}
    val motion=rememberInfiniteTransition(label="arrange")
    val wiggle by motion.animateFloat(-.9f,.9f,infiniteRepeatable(tween(420,easing=LinearEasing),RepeatMode.Reverse),label="card-wiggle")
    fun targetAt(position:Offset):String?=grid.layoutInfo.visibleItemsInfo.firstOrNull {
        position.x>=it.offset.x && position.x<=it.offset.x+it.size.width && position.y>=it.offset.y && position.y<=it.offset.y+it.size.height
    }?.key as? String
    fun finish() { dragging=null;target=null }
    LaunchedEffect(dragging) {
        while(dragging!=null) {
            val bottom=grid.layoutInfo.viewportEndOffset.toFloat()
            val speed=when {pointer.y<edge->-12f;pointer.y>bottom-edge->12f;else->0f}
            if(speed!=0f){grid.scrollBy(speed);target=targetAt(pointer)}
            delay(16)
        }
    }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(),color=p.background) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Arrange Home",fontSize=26.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    TextButton(onClick=onClose){Text("Done")}
                }
                Text("Drag a card to move it. Steps and calories stay together. Changes save when you drop.",fontSize=12.sp,color=p.muted)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(columns=GridCells.Fixed(2),state=grid,userScrollEnabled=dragging==null,
                        modifier=Modifier.fillMaxSize().testTag("arrange-grid").pointerInput(Unit) {
                            detectDragGestures(onDragStart={position->
                                val item=grid.layoutInfo.visibleItemsInfo.firstOrNull { position.x>=it.offset.x && position.x<=it.offset.x+it.size.width && position.y>=it.offset.y && position.y<=it.offset.y+it.size.height }
                                if(item!=null){dragging=item.key as String;pointer=position;anchor=position-Offset(item.offset.x.toFloat(),item.offset.y.toFloat());dragWidth=item.size.width;haptic.performHapticFeedback(HapticFeedbackType.LongPress)}
                            },onDragEnd={val id=dragging;val to=target;if(id!=null && to!=null)save(latest.placeHome(id,to));finish()},onDragCancel={finish()}) { change,amount ->
                                if(dragging!=null){change.consume();pointer+=amount;target=targetAt(pointer)}
                            }
                        },contentPadding=PaddingValues(5.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(cards,key={it}) { id ->
                            Box(Modifier.animateItem().graphicsLayer{rotationZ=wiggle*(if(cards.indexOf(id)%2==0)1 else -1);alpha=if(dragging==id).15f else 1f}.testTag("arrange-$id").semantics {
                                contentDescription="Arrange ${DashboardPreferences.homeName(id)}"
                                customActions=listOf(
                                    CustomAccessibilityAction("Move earlier"){val i=cards.indexOf(id);if(i>0)onSave(prefs.placeHome(id,cards[i-1]));i>0},
                                    CustomAccessibilityAction("Move later"){val i=cards.indexOf(id);if(i<cards.lastIndex)onSave(prefs.placeHome(id,cards[i+1]));i<cards.lastIndex}
                                )
                            }) {
                                ArrangePreview(id,day,Modifier.fillMaxWidth(),target==id && dragging!=id)
                                IconButton(onClick={onSave(prefs.copy(homeHidden=prefs.homeHidden+id))},modifier=Modifier.align(Alignment.TopEnd).size(48.dp)) {
                                    Text("−",color=p.primary,fontSize=24.sp,modifier=Modifier.semantics{contentDescription="Hide ${DashboardPreferences.homeName(id)}"})
                                }
                            }
                        }
                    }
                    dragging?.let { id ->
                        ArrangePreview(id,day,Modifier.offset { IntOffset((pointer.x-anchor.x).roundToInt(),(pointer.y-anchor.y).roundToInt()) }.width(with(density){dragWidth.toDp()}).graphicsLayer{scaleX=1.035f;scaleY=1.035f;shadowElevation=18.dp.toPx()},true)
                    }
                    if(cards.isEmpty())Text("No cards on Home. Add some below.",modifier=Modifier.align(Alignment.Center),color=p.muted)
                }
                OutlinedButton(onClick=onManage,modifier=Modifier.fillMaxWidth()){Text("Add or restore cards")}
            }
        }
    }
}

@Composable private fun ArrangePreview(id:String,day:ConnectedDay?,modifier:Modifier,highlight:Boolean) {
    val p=LocalPulseStyle.current
    val ink=when(id){"sleep"->p.violet;"energy"->p.warm;"recovery","hrv"->p.secondary;else->p.primary}
    val icon=when(id){"activity"->R.drawable.symbol_directions_walk;"sleep"->R.drawable.symbol_bedtime;"week"->R.drawable.symbol_calendar;"energy"->R.drawable.symbol_local_fire_department;else->R.drawable.symbol_favorite}
    val metric=detailMetric(if(id=="heart")"hr" else id)
    val value=when(id){"activity"->day?.steps?.let{"%,d steps".format(it)} ?: "No steps";"week"->"7-day overview";else->day?.let(metric::value)?.let(metric::format) ?: "Not shared"}
    Column(modifier.heightIn(min=152.dp).background(ink.copy(alpha=.16f).compositeOver(p.surface),RoundedCornerShape(22.dp)).border(if(highlight)2.dp else 1.dp,ink.copy(alpha=if(highlight).9f else .3f),RoundedCornerShape(22.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon),null,tint=ink,modifier=Modifier.size(24.dp))
        Text(DashboardPreferences.homeName(id),fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(end=12.dp))
        Text(value,fontSize=16.sp,color=ink)
        if(id=="activity")Text(calorieTileValue(day).let{(it.value?.roundToInt()?.toString() ?: "—")+if(it.isActive)" active kcal" else " total kcal"},fontSize=11.sp,color=p.muted)
    }
}
