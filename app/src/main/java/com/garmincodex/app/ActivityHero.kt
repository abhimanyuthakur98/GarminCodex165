package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable internal fun ActivityHero(day:ConnectedDay?,goals:PersonalGoals,reveal:Float,onDetail:(String)->Unit) {
    val palette=LocalPulseStyle.current
    val steps=day?.steps
    val goal=goals.steps.coerceAtLeast(1)
    val progress=((steps ?: 0).toFloat()/goal).coerceIn(0f,1f)
    val ink=palette.background
    Column(Modifier.fillMaxWidth().testTag("goal-Steps").clip(RoundedCornerShape(30.dp))
        .background(Brush.linearGradient(listOf(palette.primary,palette.secondary)))
        .clickable(role=Role.Button,onClickLabel="Steps details and goal"){onDetail("steps")}
        .padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Your movement",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=ink)
            Text("↗",fontSize=24.sp,color=ink)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.text.BasicText(steps?.let{String.format(Locale.getDefault(),"%,d",it)} ?: "—",
                    autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(30.sp,46.sp),maxLines=1,
                    style=androidx.compose.ui.text.TextStyle(color=ink,fontWeight=FontWeight.Bold,letterSpacing=(-1.5).sp))
                Text("steps",fontSize=14.sp,color=ink.copy(alpha=.75f))
            }
            Box(Modifier.size(96.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                    drawArc(ink.copy(alpha=.13f),-90f,360f,false,style=Stroke(8.dp.toPx()))
                    drawArc(ink,-90f,progress*360f*reveal,false,style=Stroke(8.dp.toPx(),cap=StrokeCap.Round))
                }
                Text(if(steps==null) "—" else "${(progress*100).toInt()}%",color=ink,fontSize=22.sp,fontWeight=FontWeight.Bold)
            }
        }
        HorizontalDivider(color=ink.copy(alpha=.15f))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Goal ${String.format(Locale.getDefault(),"%,d",goal)}",color=ink,fontSize=12.sp)
            Text(when{steps==null->"No steps shared";steps>=goal->"Goal reached ✓";else->"${String.format(Locale.getDefault(),"%,d",goal-steps)} to go"},color=ink,fontWeight=FontWeight.SemiBold,fontSize=12.sp)
        }
    }
}
