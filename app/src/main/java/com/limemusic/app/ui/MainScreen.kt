package com.limemusic.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.limemusic.app.data.MusicItem
import com.limemusic.app.data.MusicLibraryState

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    playerViewModel: MusicPlayerViewModel
) {
    val libraryState by musicViewModel.libraryState.collectAsState()
    val musicList by musicViewModel.displayMusic.collectAsState()
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val currentMusic by playerViewModel.currentMusic.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val playerError by playerViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        musicViewModel.loadMusic()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF7F9FF),
                        Color(0xFFEFF3FF),
                        Color(0xFFF8F9FC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Header(
                musicCount = when (libraryState) {
                    is MusicLibraryState.Success ->
                        (libraryState as MusicLibraryState.Success).count
                    else -> musicList.size
                },
                onRefresh = { musicViewModel.refreshMusic() }
            )

            SearchBar(
                query = searchQuery,
                onQueryChange = musicViewModel::setSearchQuery,
                onClear = musicViewModel::clearSearch
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = libraryState) {
                    MusicLibraryState.Idle ->
                        LoadingView("准备音乐库...")

                    MusicLibraryState.Loading ->
                        LoadingView("正在同步音乐...")

                    is MusicLibraryState.Error -> {
                        if (musicList.isNotEmpty()) {
                            MusicList(
                                musicList = musicList,
                                currentMusic = currentMusic,
                                onMusicClick = { index ->
                                    playerViewModel.setQueue(musicList)
                                    playerViewModel.playQueue(musicList, index)
                                }
                            )
                        } else {
                            ErrorView(
                                message = state.message,
                                onRetry = musicViewModel::refreshMusic
                            )
                        }
                    }

                    is MusicLibraryState.Success -> {
                        if (musicList.isEmpty()) {
                            EmptyView(searchQuery.isNotBlank())
                        } else {
                            MusicList(
                                musicList = musicList,
                                currentMusic = currentMusic,
                                onMusicClick = { index ->
                                    playerViewModel.setQueue(musicList)
                                    playerViewModel.playQueue(musicList, index)
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = currentMusic != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MiniPlayer(
                    music = currentMusic,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPrevious = playerViewModel::previous,
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    onSeek = playerViewModel::seekFraction,
                    onSeekFinished = playerViewModel::endSeek
                )
            }
        }

        AnimatedVisibility(
            visible = playerError != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            ErrorToast(
                message = playerError ?: "",
                onClose = playerViewModel::clearError
            )
        }
    }
}

@Composable
private fun Header(
    musicCount: Int,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Lime Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (musicCount > 0) "$musicCount 首歌曲" else "音乐库",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6E7480)
            )
        }

        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.72f),
            shadowElevation = 2.dp
        ) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "刷新音乐库")
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.70f),
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "搜索",
                tint = Color(0xFF747985)
            )

            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF20242C)
                ),
                decorationBox = { inner ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                "搜索歌曲或歌手",
                                color = Color(0xFF969BA5)
                            )
                        }
                        inner()
                    }
                }
            )

            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, "清除搜索")
                }
            }
        }
    }
}

@Composable
private fun MusicList(
    musicList: List<MusicItem>,
    currentMusic: MusicItem?,
    onMusicClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = musicList,
            key = { _, music -> music.id }
        ) { index, music ->
            MusicRow(
                music = music,
                playing = currentMusic?.id == music.id,
                onClick = { onMusicClick(index) }
            )
        }
    }
}

@Composable
private fun MusicRow(
    music: MusicItem,
    playing: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (playing) {
            Color(0xFFDDE7FF)
        } else {
            Color.White.copy(alpha = 0.68f)
        },
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (playing) Color(0xFF6D8FFF)
                        else Color(0xFFE9ECF3)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = if (playing) "正在播放" else null,
                    tint = if (playing) Color.White else Color(0xFF6E7480)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    music.displayTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight =
                        if (playing) FontWeight.Bold else FontWeight.Medium,
                    color = Color(0xFF1F232B)
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    music.displayArtist(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF777D88)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                music.extension.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9298A3)
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    music: MusicItem?,
    isPlaying: Boolean,
    progress: Float,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    if (music == null) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE4E9F8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF647BD2)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        music.displayTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        music.displayArtist(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF777D88)
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, "上一首")
                }

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFF20242C)
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause
                            else Icons.Default.PlayArrow,
                            if (isPlaying) "暂停" else "播放",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, "下一首")
                }
            }

            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = onSeek,
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

@Composable
private fun LoadingView(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(text, color = Color(0xFF747985))
        }
    }
}

@Composable
private fun EmptyView(searching: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF9AA1AE)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (searching) "没有找到相关歌曲" else "音乐库是空的",
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                if (searching) "换个关键词试试看" else "请检查 GitHub 音乐仓库",
                color = Color(0xFF7D838E)
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "音乐库加载失败",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFF777D88))
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.clickable(onClick = onRetry),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF20242C)
            ) {
                Text(
                    "重新加载",
                    color = Color.White,
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 11.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ErrorToast(
    message: String,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF292D35),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 6.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}
