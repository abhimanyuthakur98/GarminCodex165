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
    OutlinedButton(onClick=onClick,modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp),
        border=BorderStroke(1.dp,Color(0xFF72E8BE).copy(alpha=if(selected) .6f else .22f)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=Color(0xFF72E8BE).copy(alpha=if(selected) .16f else .07f),contentColor=Color(0xFF72E8BE))) {
        Canvas(Modifier.size(16.dp)) {
            val s=size.width;val stroke=1.3.dp.toPx()
            drawRoundRect(Color(0xFF72E8BE),Offset(s*.15f,s*.08f),Size(s*.72f,s*.84f),androidx.compose.ui.geometry.CornerRadius(s*.1f),style=Stroke(stroke))
            drawLine(Color(0xFF72E8BE),Offset(s*.33f,s*.08f),Offset(s*.33f,s*.92f),stroke)
            repeat(2){i->drawLine(Color(0xFF72E8BE),Offset(s*.46f,s*(.38f+i*.22f)),Offset(s*.72f,s*(.38f+i*.22f)),stroke)}
        }
        Spacer(Modifier.width(6.dp));Text("Journal",fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
    }
}

@Composable internal fun PulseDock(page:Int,onPage:(Int)->Unit) {
    Surface(Modifier.navigationBarsPadding().padding(horizontal=12.dp,vertical=8.dp),shape=RoundedCornerShape(28.dp),color=Color(0xFF171F1D),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.08f)),shadowElevation=12.dp) {
        Row(Modifier.fillMaxWidth().padding(5.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf("Today","Trends","Fitness Age","Connect","Workouts").forEachIndexed { index,label ->
                val tint by animateColorAsState(if(page==index) Lime else Color(0xFF8F9E98),label="dock-tint")
                Column(Modifier.weight(1f).background(if(page==index) Lime.copy(alpha=.09f) else Color.Transparent,RoundedCornerShape(22.dp)).selectable(page==index,role=Role.Tab,onClick={onPage(index)}).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    DockIcon(index,tint)
                    BasicText(label,modifier=Modifier.fillMaxWidth(),maxLines=1,autoSize=TextAutoSize.StepBased(8.sp,10.sp),style=TextStyle(color=tint,textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontWeight=if(page==index) FontWeight.Bold else FontWeight.Medium))
                    Box(Modifier.size(width=14.dp,height=2.dp).background(if(page==index)Lime else Color.Transparent,RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
@Composable private fun DockIcon(index:Int,color:Color) {
    Canvas(Modifier.size(22.dp)) {
        val s=size.width; val stroke=1.6.dp.toPx()
        fun line(x:Float,y:Float,xx:Float,yy:Float)=drawLine(color,Offset(x*s,y*s),Offset(xx*s,yy*s),stroke,StrokeCap.Round)
        when(index) {
            0 -> { drawCircle(color,s*.36f,style=Stroke(stroke));line(.28f,.52f,.43f,.52f);line(.43f,.52f,.49f,.32f);line(.49f,.32f,.58f,.68f);line(.58f,.68f,.65f,.49f);line(.65f,.49f,.76f,.49f) }
            1 -> { line(.16f,.82f,.16f,.6f);line(.39f,.82f,.39f,.43f);line(.62f,.82f,.62f,.55f);line(.85f,.82f,.85f,.18f) }
            2 -> { drawCircle(color,s*.37f,style=Stroke(stroke));drawCircle(color,s*.19f,style=Stroke(stroke));drawCircle(color,s*.035f) }
            3 -> { drawRoundRect(color,Offset(s*.28f,s*.18f),Size(s*.44f,s*.64f),androidx.compose.ui.geometry.CornerRadius(s*.1f),style=Stroke(stroke));line(.4f,.05f,.6f,.05f);line(.4f,.95f,.6f,.95f);drawCircle(color,s*.06f) }
            4 -> { line(.2f,.26f,.2f,.74f);line(.35f,.15f,.35f,.85f);line(.35f,.5f,.65f,.5f);line(.65f,.15f,.65f,.85f);line(.8f,.26f,.8f,.74f) }
        }
    }
}
@Composable internal fun JournalCard(title:String,accent:Color=Lime,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.copy(alpha=.08f).compositeOver(Color(0xFF15201B)),Color(0xFF141B18))),RoundedCornerShape(26.dp)).border(1.dp,accent.copy(alpha=.10f),RoundedCornerShape(26.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp);content()
    }
}
