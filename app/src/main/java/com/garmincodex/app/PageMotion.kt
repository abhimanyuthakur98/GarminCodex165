package com.garmincodex.app
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Short entrance transition; Compose honours the system animation duration scale. */
@Composable internal fun pageMotion(identity:Any):Modifier {
    val progress=remember(identity){Animatable(0f)}
    val offset=with(LocalDensity.current){18.dp.toPx()}
    LaunchedEffect(identity){progress.animateTo(1f,tween(380,easing=CubicBezierEasing(.16f,1f,.3f,1f)))}
    return Modifier.graphicsLayer { alpha=progress.value;translationY=offset*(1-progress.value);scaleX=.985f+.015f*progress.value;scaleY=scaleX }
}
