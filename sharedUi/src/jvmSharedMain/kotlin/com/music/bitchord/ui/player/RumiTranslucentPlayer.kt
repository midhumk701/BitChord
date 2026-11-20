package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.theme.rememberArtworkPalette
import com.music.bitchord.ui.theme.stableStatusBarInsets
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val Paper = Color(0xFFF7F7F5)
private val Ink = Color(0xFF615C66)

/** Independent light vinyl surface; Classic continues to use its original controls. */
@Composable
internal fun RumiTranslucentPlayer(
    song: Song,
    artworkUrl: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    shown: () -> Float,
    durationMs: Long,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onClassic: () -> Unit,
    onVinyl: () -> Unit,
    onSongOptions: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The same cached artwork palette drives the disc, label, buttons and seek bar.
    val palette = rememberArtworkPalette(artworkUrl, dark = false, artPx = HEADER_ART_PX)
    val accent = palette.accent
    val reduceMotion by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    var optionsExpanded by remember { mutableStateOf(false) }
    var editExpanded by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.background(Paper)) {
        // Album-art atmosphere belongs to Vinyl only; Classic never enters this composable.
        // A soft white glass wash keeps the text legible over every album palette.
        AsyncImage(
            model = artworkUrl.artworkAt(HEADER_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.25f
                scaleY = 1.25f
            }.blur(72.dp),
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(
                Paper.copy(alpha = 0.73f),
                lerp(Paper, accent, 0.12f).copy(alpha = 0.81f),
                Paper.copy(alpha = 0.88f),
            ))
        ))
        val artworkSize = minOf(maxWidth, maxHeight * 0.62f).coerceAtLeast(220.dp)
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(stableStatusBarInsets())
                .navigationBarsPadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.padding(top = 8.dp).width(38.dp).height(4.dp)
                .clip(CircleShape).background(Ink.copy(alpha = 0.18f)))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RUMI MUSIC", color = Ink.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelMedium)
                Box {
                    TextButton(onClick = { optionsExpanded = true }) {
                        Text("•••", color = Ink, style = MaterialTheme.typography.titleMedium)
                    }
                    DropdownMenu(expanded = optionsExpanded,
                        onDismissRequest = { optionsExpanded = false }) {
                        DropdownMenuItem(text = { Text("Song options (including playlists)") },
                            onClick = {
                                optionsExpanded = false
                                onSongOptions()
                            })
                        DropdownMenuItem(text = { Text("Edit Player") },
                            onClick = {
                                optionsExpanded = false
                                editExpanded = true
                            })
                    }
                    DropdownMenu(expanded = editExpanded,
                        onDismissRequest = { editExpanded = false }) {
                        DropdownMenuItem(text = { Text("✓ Vinyl") },
                            onClick = { editExpanded = false; onVinyl() })
                        DropdownMenuItem(text = { Text("Classic") },
                            onClick = { editExpanded = false; onClassic() })
                    }
                }
            }
            TranslucentRecord(song, artworkUrl, accent, isPlaying && !reduceMotion,
                Modifier.size(artworkSize))
            Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp)) {
                Spacer(Modifier.height(18.dp))
                Text(song.title, color = accent, style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(song.artist, color = accent.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                RumiSeekBar(shown, durationMs, accent, onScrub, onScrubFinished)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    RecordButton(Res.drawable.ic_player_previous,
                        stringResource(Res.string.widget_previous), accent, false,
                        previousEnabled, onPrevious)
                    if (isLoading) {
                        Box(Modifier.size(68.dp).clip(CircleShape)
                            .background(accent.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent, strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp))
                        }
                    } else {
                        RecordButton(if (isPlaying) Res.drawable.ic_player_pause else Res.drawable.ic_player_play,
                            stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                            accent, true, true, onPlayPause)
                    }
                    RecordButton(Res.drawable.ic_player_next,
                        stringResource(Res.string.widget_next), accent, false, nextEnabled, onNext)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onLyrics) { Text("Lyrics", color = Ink.copy(alpha = 0.65f)) }
                    TextButton(onClick = onQueue) { Text("Queue", color = Ink.copy(alpha = 0.65f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RecordButton(icon: DrawableResource, label: String, accent: Color,
    main: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(if (main) 68.dp else 54.dp)
            .clip(CircleShape).background(accent.copy(alpha = 0.055f))
            .border(1.dp, accent.copy(alpha = 0.13f), CircleShape)) {
        Icon(painterResource(icon), contentDescription = label,
            tint = accent.copy(alpha = if (enabled) 0.85f else 0.25f),
            modifier = Modifier.size(if (main) 28.dp else 22.dp))
    }
}

@Composable
private fun RumiSeekBar(shown: () -> Float, durationMs: Long, accent: Color,
    onScrub: (Float) -> Unit, onFinished: () -> Unit) {
    val progress = shown().coerceIn(0f, 1f)
    ThinSlider(value = progress, onValueChange = onScrub,
        onValueChangeFinished = onFinished, idleHeight = 3.dp, activeHeight = 6.dp,
        activeColor = accent.copy(alpha = 0.65f), inactiveColor = accent.copy(alpha = 0.12f))
    fun time(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(time((durationMs * progress).toLong()), color = Ink.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall)
        Text(time(durationMs), color = Ink.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall)
    }
}

/** All geometry scales together; the album sleeve and tonearm remain stationary. */
@Composable
private fun TranslucentRecord(song: Song, artworkUrl: String?, accent: Color,
    spinning: Boolean, modifier: Modifier) {
    val rotation = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    LaunchedEffect(spinning) {
        if (spinning) while (true) {
            rotation.animateTo(rotation.value + 360f, tween(18000, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    BoxWithConstraints(modifier.clipToBounds()) {
        val side = maxWidth
        val disc = side * 0.79f
        // Tilted sleeve partly visible behind the translucent record.
        AsyncImage(model = artworkUrl.artworkAt(HEADER_ART_PX), contentDescription = "Album sleeve",
            contentScale = ContentScale.Crop,
            modifier = Modifier.offset(x = -side * 0.12f, y = side * 0.05f)
                .size(side * 0.60f).graphicsLayer { rotationZ = -5f }
                .clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.12f)))
        Box(Modifier.offset(x = side * 0.14f, y = side * 0.21f).size(disc)
            .graphicsLayer { rotationZ = rotation.value }.clip(CircleShape)) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawCircle(Brush.linearGradient(listOf(
                    accent.copy(alpha = 0.18f), Color.White.copy(alpha = 0.64f),
                    accent.copy(alpha = 0.28f), Color.White.copy(alpha = 0.5f))))
                drawCircle(accent.copy(alpha = 0.16f), style = Stroke(radius * 0.007f))
                for (i in 0..23) {
                    drawCircle(accent.copy(alpha = if (i % 4 == 0) 0.12f else 0.05f),
                        radius = radius * (0.55f + i * 0.018f), style = Stroke(radius * 0.002f))
                }
                drawArc(Color.White.copy(alpha = 0.3f), 200f, 42f, true)
                drawArc(Color.White.copy(alpha = 0.22f), 20f, 42f, true)
                drawCircle(lerp(Paper, accent, 0.07f), radius = radius * 0.51f)
                drawCircle(accent.copy(alpha = 0.17f), radius = radius * 0.51f,
                    style = Stroke(radius * 0.005f))
            }
            val title = if (song.title.length > 26) song.title.take(25) + "…" else song.title
            val letters = title.map { character ->
                measurer.measure(character.toString(), TextStyle(
                    color = accent.copy(alpha = 0.65f), fontSize = (disc.value * 0.047f).sp))
            }
            Canvas(Modifier.fillMaxSize()) {
                val arc = (title.length * 5.5f).coerceAtMost(140f)
                letters.forEachIndexed { index, letter ->
                    val degrees = -90f - arc / 2f + arc * (index + 0.5f) / letters.size.coerceAtLeast(1)
                    val radians = Math.toRadians(degrees.toDouble())
                    val point = center + Offset(cos(radians).toFloat(), sin(radians).toFloat()) * (size.width * 0.19f)
                    rotate(degrees + 90f, point) {
                        drawText(letter, topLeft = point - Offset(letter.size.width / 2f, letter.size.height / 2f))
                    }
                }
            }
            Column(Modifier.align(Alignment.Center).fillMaxWidth(0.65f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(disc * 0.10f))
                Box(Modifier.size(disc * 0.065f).clip(CircleShape)
                    .border(2.dp, Color(0xFFC2C0C4), CircleShape).background(Color(0xFF242027)))
                Spacer(Modifier.height(disc * 0.07f))
                Text(song.artist, color = accent.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val pivot = Offset(w * 0.73f, w * 0.095f)
            val arm = Path().apply {
                moveTo(pivot.x, pivot.y)
                cubicTo(w * 0.78f, w * 0.19f, w * 0.88f, w * 0.20f, w * 0.88f, w * 0.35f)
                lineTo(w * 0.88f, w * 0.67f)
            }
            drawPath(arm, lerp(Color(0xFFCBC9CC), accent, 0.25f), style = Stroke(w * 0.022f))
            drawPath(arm, lerp(Color(0xFFFDFDFD), accent, 0.08f), style = Stroke(w * 0.012f))
            drawCircle(lerp(Color(0xFFEDECEE), accent, 0.12f), w * 0.087f, pivot)
            drawCircle(Brush.linearGradient(listOf(Color.White, lerp(Color(0xFFADABB0), accent, 0.3f), Color.White)),
                w * 0.059f, pivot)
            drawCircle(Color.White.copy(alpha = 0.85f), w * 0.060f, pivot,
                style = Stroke(w * 0.004f))
            drawLine(Brush.horizontalGradient(listOf(lerp(Color(0xFFB0AEB3), accent, 0.3f), Color.White, lerp(Color(0xFFBBB9BD), accent, 0.2f))),
                Offset(w * 0.88f, w * 0.64f), Offset(w * 0.88f, w * 0.71f),
                strokeWidth = w * 0.038f)
        }
    }
}
