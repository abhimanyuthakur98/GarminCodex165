package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable internal fun JournalAction(selected:Boolean,onClick:()->Unit) {
    val palette = LocalPulseStyle.current

    OutlinedButton(onClick=onClick,modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp),
        border=BorderStroke(1.dp,palette.primary.copy(alpha=if(selected) .6f else .22f)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=palette.primary.copy(alpha=if(selected) .16f else .07f),contentColor=palette.primary)) {
        Icon(androidx.compose.ui.res.painterResource(R.drawable.symbol_book),null,tint=palette.primary,modifier=Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp));Text("Journal",fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
    }
}

@Composable internal fun PulseDock(page:Int,onPage:(Int)->Unit) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary

    Surface(Modifier.navigationBarsPadding().padding(horizontal=18.dp,vertical=8.dp),shape=RoundedCornerShape(24.dp),color=palette.surface,border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.08f)),shadowElevation=4.dp) {
        Row(Modifier.fillMaxWidth().padding(5.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(0 to "Today",1 to "Trends",2 to "Fitness Age",4 to "Workouts").forEach { (index,label) ->
                val selectedBackground by animateColorAsState(if(page==index) Lime.copy(alpha=.15f) else Color.Transparent,label="dock-selection")
                val tint by animateColorAsState(if(page==index) Lime else palette.muted,label="dock-tint")
                Column(Modifier.weight(1f).background(selectedBackground,RoundedCornerShape(22.dp)).selectable(page==index,role=Role.Tab,onClick={onPage(index)}).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    DockIcon(index,tint)
                    BasicText(label,modifier=Modifier.fillMaxWidth(),maxLines=1,autoSize=TextAutoSize.StepBased(10.sp,12.sp),style=TextStyle(color=tint,textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontWeight=if(page==index) FontWeight.Bold else FontWeight.Medium))
                    Box(Modifier.size(width=14.dp,height=2.dp).background(if(page==index)Lime else Color.Transparent,RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
@Composable internal fun DockIcon(index:Int,color:Color) {
    val icon=when(index){0->R.drawable.symbol_home;1->R.drawable.symbol_monitoring;2->R.drawable.symbol_favorite;3->R.drawable.symbol_settings;else->R.drawable.symbol_fitness_center}
    Icon(androidx.compose.ui.res.painterResource(icon),contentDescription=null,tint=color,modifier=Modifier.size(24.dp))
}
@Composable internal fun JournalCard(title:String,accent:Color=LocalPulseStyle.current.primary,content:@Composable ColumnScope.()->Unit) {
    val palette = LocalPulseStyle.current

    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.copy(alpha=.08f).compositeOver(palette.surface),palette.surface)),RoundedCornerShape(28.dp)).border(1.dp,accent.copy(alpha=.10f),RoundedCornerShape(26.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp);content()
    }
}
