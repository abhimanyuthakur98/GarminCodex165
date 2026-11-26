package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun PageHeading(title:String,eyebrow:String,subtitle:String?=null,modifier:Modifier=Modifier) {
    val p=LocalPulseStyle.current
    Column(modifier.padding(top=6.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(eyebrow,color=p.primary,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.8.sp)
        Text(title,fontSize=32.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-1).sp)
        if(subtitle!=null)Text(subtitle,fontSize=12.sp,color=p.muted)
    }
}

@Composable internal fun PulseTabs(options:List<String>,selected:String,onSelect:(String)->Unit,scrollable:Boolean=true) {
    val p=LocalPulseStyle.current
    Row(Modifier.fillMaxWidth().then(if(scrollable)Modifier.horizontalScroll(rememberScrollState()) else Modifier),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val active=option==selected
            Column(Modifier.then(if(scrollable)Modifier else Modifier.weight(1f)).heightIn(min=48.dp).selectable(active,role=Role.Tab,onClick={onSelect(option)}).padding(horizontal=if(scrollable)12.dp else 6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.heightIn(min=42.dp),contentAlignment=Alignment.Center){Text(option,fontSize=12.sp,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Normal,color=if(active)Color.White else p.muted)}
                Box(Modifier.width(20.dp).height(3.dp).background(if(active)p.primary else Color.Transparent,RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable internal fun SettingsMenuRow(title:String,subtitle:String,icon:Int,onClick:()->Unit) {
    val p=LocalPulseStyle.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.surface).clickable(role=Role.Button,onClick=onClick).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(p.primary.copy(alpha=.10f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(painterResource(icon),null,tint=p.primary,modifier=Modifier.size(21.dp))}
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium);Text(subtitle,fontSize=11.sp,color=p.muted)}
        Text("›",color=p.muted,fontSize=22.sp)
    }
}
