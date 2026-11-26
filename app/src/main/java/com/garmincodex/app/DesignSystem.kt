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

    IconButton(onClick=onClick,modifier=Modifier.size(48.dp)) {
        Icon(androidx.compose.ui.res.painterResource(R.drawable.symbol_book),contentDescription="Journal",tint=palette.muted,modifier=Modifier.size(23.dp))
    }
}

@Composable internal fun PulseDock(page:Int,onPage:(Int)->Unit) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary

    Surface(Modifier.navigationBarsPadding(),color=palette.background,shadowElevation=0.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(0 to "Today",1 to "Trends",2 to "Fitness Age",4 to "Workouts").forEach { (index,label) ->
                val selectedBackground by animateColorAsState(if(page==index) Lime.copy(alpha=.13f) else Color.Transparent,label="dock-selection")
                val tint by animateColorAsState(if(page==index) Lime else palette.muted,label="dock-tint")
                Column(Modifier.weight(1f).selectable(page==index,role=Role.Tab,onClick={onPage(index)}).padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(48.dp).height(28.dp).background(selectedBackground,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){DockIcon(index,tint)}
                    BasicText(label,modifier=Modifier.fillMaxWidth(),maxLines=1,autoSize=TextAutoSize.StepBased(10.sp,12.sp),style=TextStyle(color=tint,textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontWeight=if(page==index) FontWeight.Bold else FontWeight.Medium))

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

    Column(Modifier.fillMaxWidth().background(palette.surface,RoundedCornerShape(24.dp)).border(1.dp,Color.White.copy(alpha=.055f),RoundedCornerShape(24.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(5.dp).background(accent,RoundedCornerShape(4.dp)))
            Text(title,color=palette.muted,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)
        };content()
    }
}
