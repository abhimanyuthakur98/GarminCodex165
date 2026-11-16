package com.garmincodex.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PulseStyle(val title:String,val primary:Color,val secondary:Color,val violet:Color,val warm:Color,val background:Color,val surface:Color) {
    NEON_CYAN("Neon Cyan",Color(0xFF3DEBFF),Color(0xFF7AFFB2),Color(0xFFC0A0FF),Color(0xFFFFAA70),Color(0xFF070C12),Color(0xFF111D28)),
    NEON_LIME("Neon Lime",Color(0xFFC6FF42),Color(0xFF40F5CA),Color(0xFFE1A0FF),Color(0xFFFFA477),Color(0xFF090D09),Color(0xFF182016)),
    NEON_VIOLET("Neon Violet",Color(0xFFD0A0FF),Color(0xFF62E9FF),Color(0xFFFF8DD7),Color(0xFFFFB381),Color(0xFF100B17),Color(0xFF23192F)),
    MIDNIGHT("Midnight Blue",Color(0xFF88B5FF),Color(0xFF8FD8CE),Color(0xFFC0AFF5),Color(0xFFFFB28D),Color(0xFF0C1018),Color(0xFF171E2B));
    val muted:Color get()=Color(0xFFB0BACC)
    val line:Color get()=primary.copy(alpha=.18f)
    companion object { fun fromId(id:String?)=entries.firstOrNull { it.name==id } ?: NEON_CYAN }
}
internal val LocalPulseStyle=staticCompositionLocalOf { PulseStyle.NEON_CYAN }

@Composable internal fun AppearancePicker(selected:PulseStyle,onSelect:(PulseStyle)->Unit) {
    JournalCard("COLOUR SCHEME") {
        Text("Make Pulse yours",fontSize=24.sp)
        Text("Preview instantly. Your choice is saved on this phone.",fontSize=12.sp)
        PulseStyle.entries.forEach { style ->
            Row(Modifier.fillMaxWidth().background(style.surface,RoundedCornerShape(18.dp)).border(if(style==selected)2.dp else 1.dp,if(style==selected)style.primary else style.line,RoundedCornerShape(18.dp)).selectable(selected=style==selected,role=Role.RadioButton,onClick={onSelect(style)}).padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                listOf(style.primary,style.secondary,style.violet).forEach { color -> Box(Modifier.size(18.dp).background(color,CircleShape)) }
                Text(style.title,modifier=Modifier.weight(1f),color=Color.White,fontSize=14.sp)
                if(style==selected) Text("✓",color=style.primary)
            }
        }
    }
}
