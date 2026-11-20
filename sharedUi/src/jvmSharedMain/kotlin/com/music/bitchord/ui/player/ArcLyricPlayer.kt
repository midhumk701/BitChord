package com.music.bitchord.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.theme.rememberArtworkPalette
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

private val white = Color.White
private fun artworkEdge(x: Float) = 0.535f - 0.176f * (x - 0.08f) * (x - 0.08f)
private fun seekHeight(x: Float) = artworkEdge(x) + 0.032f
private fun timestamp(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
internal fun ArcLyricPlayer(
    song: Song, artwork: String?, lines: List<LyricLine>, position: PlaybackPosition,
    durationMs: Long, playing: Boolean, liked: Boolean = false,
    onLike: (() -> Unit)? = null, onPrevious: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = null, onNext: (() -> Unit)? = null,
    onSeek: ((Float) -> Unit)? = null, onCollapse: (() -> Unit)? = null,
) {
    val palette = rememberArtworkPalette(artwork, dark = true)
    val clock = rememberLyricClock(song.videoId, rememberLyricPlayhead(position), playing)
    val timed = remember(lines) { lines.any { it.timeMs > 0 || it.isWordSynced } }
    val active by remember(lines, timed, clock) {
        derivedStateOf {
            if (!timed) null else lines.lastOrNull { it.timeMs <= clock.longValue }
                ?.takeIf { !it.isGap && clock.longValue < (it.sungUntilMs ?: Long.MAX_VALUE) }
        }
    }
    val shape = remember { GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height * artworkEdge(1f))
        quadraticBezierTo(size.width / 2, size.height * (artworkEdge(0f) + 0.01408f),
            0f, size.height * artworkEdge(0f))
        close()
    } }
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        palette.background, lerp(palette.accent, Color.Black, 0.72f), palette.background)))) {
        val w = maxWidth
        val h = maxHeight
        Box(Modifier.fillMaxSize().clip(shape)) {
            AsyncImage(artwork, null, Modifier.fillMaxWidth().fillMaxHeight(0.54f),
                contentScale = ContentScale.Crop)
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = h * 0.05f)
            .size(64.dp, 3.dp).background(white.copy(alpha = 0.65f), RoundedCornerShape(2.dp)))
        if (onCollapse != null) IconButton(onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = h * 0.055f, end = 12.dp)
                .semantics { contentDescription = "Collapse player" }) { Text("⌄", color = white) }

        val seekAction by rememberUpdatedState(onSeek)
        Canvas(Modifier.fillMaxSize().pointerInput(onSeek != null, durationMs) {
            if (onSeek == null || durationMs <= 0) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                val start = down.position.x / size.width
                if (start !in 0.14f..0.90f || abs(down.position.y - seekHeight(start) * size.height) > 18.dp.toPx())
                    return@awaitEachGesture
                down.consume()
                var x = start
                do {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    x = change.position.x / size.width
                    change.consume()
                } while (change.pressed)
                seekAction?.invoke(((x - 0.18f) / 0.68f).coerceIn(0f, 1f))
            }
        }) {
            fun drawTrack(end: Float, color: Color) {
                val start = 0.18f
                if (end <= start) return
                val path = Path().apply {
                    moveTo(size.width * start, size.height * seekHeight(start))
                    for (i in 1..64) {
                        val x = start + (end - start) * i / 64
                        lineTo(size.width * x, size.height * seekHeight(x))
                    }
                }
                drawPath(path, color, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
            drawTrack(0.86f, white.copy(alpha = 0.4f))
            val fraction = if (durationMs > 0) position.positionMs.toFloat() / durationMs else 0f
            drawTrack(0.18f + 0.68f * fraction.coerceIn(0f, 1f), white)
        }
        Text(timestamp(position.positionMs), Modifier.offset(w * 0.055f, h * seekHeight(0.18f) - 8.dp),
            color = white, fontSize = 11.sp)
        Text(timestamp(durationMs), Modifier.offset(w * 0.87f, h * seekHeight(0.86f) - 18.dp).rotate(-35f),
            color = white, fontSize = 11.sp)
        Crossfade(active, modifier = Modifier.offset(w * 0.075f, h * 0.62f).width(w * 0.64f),
            animationSpec = tween(160), label = "Current lyric") { line ->
            if (line != null) {
                val chars = line.revealedChars(clock.longValue).toInt().coerceIn(0, line.text.length)
                Text(buildAnnotatedString {
                    append(line.text)
                    addStyle(SpanStyle(color = white), 0, chars)
                }, color = white.copy(alpha = 0.48f), fontSize = (w.value * 0.077f).coerceIn(14f, 34f).sp,
                    lineHeight = (w.value * 0.088f).coerceIn(18f, 40f).sp, maxLines = 4,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = w * 0.075f, bottom = h * 0.05f)
            .width(w * 0.65f)) {
            Text(song.artist, color = white.copy(alpha = 0.7f), maxLines = 1)
            Text(song.title, color = white, style = MaterialTheme.typography.titleLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = w * 0.06f, bottom = h * 0.04f),
            verticalArrangement = Arrangement.spacedBy(h * 0.009f)) {
            val button = Modifier.size(w * 0.16f, (h * 0.055f).coerceAtMost(48.dp))
                .background(white.copy(alpha = 0.12f), RoundedCornerShape(50))
            IconButton(onClick = { onLike?.invoke() }, enabled = onLike != null,
                modifier = button.semantics { contentDescription = if (liked) "Unlike" else "Like" }) {
                Text(if (liked) "♥" else "♡", color = white)
            }
            listOf(Triple(Res.drawable.ic_player_previous, "Previous", onPrevious),
                Triple(if (playing) Res.drawable.ic_player_pause else Res.drawable.ic_player_play,
                    if (playing) "Pause" else "Play", onToggle),
                Triple(Res.drawable.ic_player_next, "Next", onNext)).forEach { (icon, label, action) ->
                IconButton(onClick = { action?.invoke() }, enabled = action != null, modifier = button) {
                    Icon(painterResource(icon), label, Modifier.size(20.dp), tint = white)
                }
            }
        }
    }
}
