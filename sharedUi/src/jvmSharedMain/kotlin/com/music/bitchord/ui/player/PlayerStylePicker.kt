package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.theme.rememberArtworkPalette
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlayerStylePicker(
    song: Song?, playing: Boolean, position: PlaybackPosition, durationMs: Long,
    contentPadding: PaddingValues, onSet: () -> Unit,
    lyrics: List<com.music.bitchord.data.lyrics.LyricLine> = emptyList(),
) {
    val selected by PlayerSettings.playerStyle.collectAsStateWithLifecycle()
    val reduceMotion by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val pager = rememberPagerState(initialPage = selected.ordinal) { PlayerStyle.entries.size }
    val art = rememberRemoteArtworkUrl(song)
    val previewSong = song ?: remember { Song("", "No song playing", "", null) }
    Column(Modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(PlayerStyle.entries[pager.currentPage].label,
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = 38.dp),
            pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            if (PlayerStyle.entries[page] == PlayerStyle.Lyric) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                    ArcLyricPlayer(previewSong, art, lyrics, position, durationMs, playing)
                }
                return@HorizontalPager
            }
            val record = page == 0
            val palette = rememberArtworkPalette(art, dark = !record)
            val ink = if (record) Color(0xFF534D56) else Color.White
            Column(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                .background(if (record) Color(0xFFF6F4F4) else Color(0xFF242129))
                .padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center) {
                    val size = minOf(maxWidth, maxHeight)
                    if (record) TranslucentRecord(previewSong, art, palette.accent,
                        playing && !reduceMotion, Modifier.size(size))
                    else AsyncImage(model = art, contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop, modifier = Modifier.size(size)
                            .clip(RoundedCornerShape(12.dp)).background(ink.copy(alpha = 0.12f)))
                }
                Text(previewSong.title, color = ink, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(previewSong.artist, color = ink.copy(alpha = 0.72f), maxLines = 1)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = {
                    if (durationMs > 0) (position.positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                }, modifier = Modifier.fillMaxWidth(), color = palette.accent)
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(Res.drawable.ic_player_previous,
                        if (playing) Res.drawable.ic_player_pause else Res.drawable.ic_player_play,
                        Res.drawable.ic_player_next).forEach { icon ->
                        Icon(painterResource(icon), null, Modifier.size(28.dp), tint = ink)
                    }
                }
            }
        }
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(PlayerStyle.entries.size) { index ->
                Box(Modifier.size(7.dp).background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (pager.currentPage == index) 1f else 0.3f),
                    CircleShape))
            }
        }
        Button(onClick = {
            PlayerSettings.setPlayerStyle(PlayerStyle.entries[pager.settledPage])
            onSet()
        }, enabled = !pager.isScrollInProgress) { Text("Set") }
    }
}
