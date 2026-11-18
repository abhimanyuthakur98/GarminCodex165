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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PulseStyle(val name:String,val title:String,val primary:Color,val secondary:Color,val violet:Color,val warm:Color,val background:Color,val surface:Color,val seed:Color=primary) {
    val muted:Color get()=Color(0xFFA3A3A3)
    val line:Color get()=primary.copy(alpha=.18f)
    companion object {
        val NEON_CYAN=PulseStyle("NEON_CYAN","Neon Cyan",Color(0xFF3DEBFF),Color(0xFF7AFFB2),Color(0xFFC0A0FF),Color(0xFFFFAA70),Color(0xFF000000),Color(0xFF181818))
        val NEON_LIME=PulseStyle("NEON_LIME","Neon Lime",Color(0xFFC6FF42),Color(0xFF40F5CA),Color(0xFFE1A0FF),Color(0xFFFFA477),Color(0xFF000000),Color(0xFF181818))
        val NEON_VIOLET=PulseStyle("NEON_VIOLET","Neon Violet",Color(0xFFD0A0FF),Color(0xFF62E9FF),Color(0xFFFF8DD7),Color(0xFFFFB381),Color(0xFF000000),Color(0xFF181818))
        val MIDNIGHT=PulseStyle("MIDNIGHT","Midnight Blue",Color(0xFF88B5FF),Color(0xFF8FD8CE),Color(0xFFC0AFF5),Color(0xFFFFB28D),Color(0xFF000000),Color(0xFF181818))
        val entries=listOf(NEON_CYAN,NEON_LIME,NEON_VIOLET,MIDNIGHT)
        fun custom(hex:String):PulseStyle? {
            val clean=hex.removePrefix("#").uppercase()
            if(!clean.matches(Regex("[0-9A-F]{6}"))) return null
            val seed=Color(0xFF000000 or clean.toLong(16))
            var accent=seed
            repeat(12){if(accent.luminance()<.4f) accent=lerp(accent,Color.White,.12f)}
            return PulseStyle("CUSTOM:"+clean,"Custom colour",accent,lerp(accent,Color(0xFF7AFFB2),.3f),lerp(accent,Color(0xFFD5A7FF),.5f),lerp(accent,Color(0xFFFFAA70),.65f),Color.Black,Color(0xFF181818),seed)
        }
        fun fromId(id:String?)=entries.firstOrNull{it.name==id} ?: id?.takeIf{it.startsWith("CUSTOM:")}?.let{custom(it.substringAfter(':'))} ?: NEON_CYAN
    }
}
internal fun tileInk(color:Color)=if(color.luminance()>.179f) Color(0xFF080B10) else Color.White
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

@Composable internal fun CustomColourPicker(selected:PulseStyle,onSelect:(PulseStyle)->Unit) {
    var hex by remember(selected.name){mutableStateOf("%06X".format(selected.seed.toArgb() and 0xFFFFFF))}
    val candidate=PulseStyle.custom(hex)
    val colour=candidate?.seed ?: selected.seed
    JournalCard("CUSTOM COLOUR") {
        Text("Every shade is yours",fontSize=22.sp)
        Box(Modifier.fillMaxWidth().height(64.dp).background(colour,RoundedCornerShape(18.dp))) {
            Text("Your personal Pulse",color=tileInk(colour),modifier=Modifier.padding(20.dp))
        }
        OutlinedTextField(hex,{hex=it.removePrefix("#").take(6)},label={Text("Hex colour")},prefix={Text("#")},singleLine=true,isError=candidate==null,modifier=Modifier.fillMaxWidth().testTag("custom-hex"))
        listOf("Red","Green","Blue").forEachIndexed { index,label ->
            val channels=listOf(colour.red,colour.green,colour.blue)
            Text(label,fontSize=12.sp)
            Slider(channels[index],{v->val c=channels.toMutableList();c[index]=v;hex="%02X%02X%02X".format((c[0]*255).toInt(),(c[1]*255).toInt(),(c[2]*255).toInt())},modifier=Modifier.semantics{contentDescription=label+" colour channel"})
        }
        Text("Your shade becomes the accent for numbers, charts and controls. Dark surfaces stay calm; accents adapt for readability.",fontSize=12.sp)
        Button(onClick={candidate?.let(onSelect)},enabled=candidate!=null,modifier=Modifier.fillMaxWidth()){Text("Apply custom colour")}
    }
}
