package com.limemusic.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limemusic.app.data.MusicItem
import com.limemusic.app.data.MusicLibraryState

private val LimeBlue = Color(0xFF3478F6)
private val LimeBlueLight = Color(0xFF6EA8FF)
private val LimeBlueSoft = Color(0xFFEAF2FF)
private val LimeBluePale = Color(0xFFF4F8FF)

private val TextPrimary = Color(0xFF182033)
private val TextSecondary = Color(0xFF737C8F)
private val GlassWhite = Color(0xFFFFFFFF)
private val BackgroundTop = Color(0xFFF4F8FF)
private val BackgroundBottom = Color(0xFFF9FAFD)

private fun openMusicFeedbackEmail(context: Context) {
    val email = "3327544159@qq.com"

    val subject = Uri.encode("Lime Music 歌曲反馈")

    val body = Uri.encode(
        """
您好，Lime Music：

我在使用 Lime Music 时没有找到喜欢的歌曲，想反馈一些歌曲。

希望添加的歌曲：
歌手：
歌曲链接/来源：
其他建议：

谢谢！
        """.trimIndent()
    )

    val uri = Uri.parse(
        "mailto:$email?subject=$subject&body=$body"
    )

    val intent = Intent(Intent.ACTION_SENDTO, uri)

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "手机没有可用的邮件应用",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    playerViewModel: MusicPlayerViewModel
) {
    val context = LocalContext.current

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
                    colors = listOf(
                        BackgroundTop,
                        Color(0xFFF8FAFF),
                        BackgroundBottom
                    )
                )
            )
    ) {

        // 背景流光
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-70).dp)
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LimeBlueLight.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9BC7FF).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

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
                onRefresh = {
                    musicViewModel.refreshMusic()
                }
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
                        LoadingView("准备音乐库…")

                    MusicLibraryState.Loading ->
                        LoadingView("正在同步音乐…")

                    is MusicLibraryState.Error -> {

                        if (musicList.isNotEmpty()) {

                            MusicList(
                                musicList = musicList,
                                currentMusic = currentMusic,
                                context = context,
                                onMusicClick = { index ->
                                    playerViewModel.setQueue(musicList)
                                    playerViewModel.playQueue(
                                        musicList,
                                        index
                                    )
                                }
                            )

                        } else {

                            EmptyView(
                                searching = searchQuery.isNotBlank(),
                                context = context
                            )
                        }
                    }

                    is MusicLibraryState.Success -> {

                        if (musicList.isEmpty()) {

                            EmptyView(
                                searching = searchQuery.isNotBlank(),
                                context = context
                            )

                        } else {

                            MusicList(
                                musicList = musicList,
                                currentMusic = currentMusic,
                                context = context,
                                onMusicClick = { index ->
                                    playerViewModel.setQueue(musicList)
                                    playerViewModel.playQueue(
                                        musicList,
                                        index
                                    )
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
                .padding(top = 12.dp),
            enter = fadeIn(),
            exit = fadeOut()
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
            .padding(
                start = 22.dp,
                end = 18.dp,
                top = 22.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    LimeBlue,
                                    LimeBlueLight
                                )
                            ),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Text(
                    text = "Lime",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-1.1).sp
                )

                Text(
                    text = " Music",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LimeBlue,
                    letterSpacing = (-1.1).sp
                )
            }

            Spacer(Modifier.height(5.dp))

            Text(
                text = if (musicCount > 0) {
                    "$musicCount 首歌曲 · 随时播放"
                } else {
                    "你的音乐空间"
                },
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Surface(
            modifier = Modifier
                .size(46.dp)
                .clickable(onClick = onRefresh),
            shape = CircleShape,
            color = GlassWhite.copy(alpha = 0.78f),
            shadowElevation = 3.dp
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新音乐库",
                    tint = LimeBlue,
                    modifier = Modifier.size(21.dp)
                )
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
        shape = RoundedCornerShape(22.dp),
        color = GlassWhite.copy(alpha = 0.76f),
        shadowElevation = 3.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.Search,
                contentDescription = "搜索",
                tint = LimeBlue,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(10.dp))

            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->

                    Box {

                        if (query.isBlank()) {

                            Text(
                                text = "搜索歌曲、歌手或文件名",
                                color = Color(0xFF9AA2B2),
                                fontSize = 15.sp
                            )
                        }

                        innerTextField()
                    }
                }
            )

            if (query.isNotBlank()) {

                IconButton(
                    onClick = onClear
                ) {

                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除搜索",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicList(
    musicList: List<MusicItem>,
    currentMusic: MusicItem?,
    context: Context,
    onMusicClick: (Int) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "全部歌曲",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    text = "${musicList.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimeBlue,
                    modifier = Modifier
                        .background(
                            LimeBlueSoft,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        )
                )
            }
        }

        itemsIndexed(
            items = musicList,
            key = { _, music ->
                music.id
            }
        ) { index, music ->

            MusicRow(
                music = music,
                playing = currentMusic?.id == music.id,
                onClick = {
                    onMusicClick(index)
                }
            )
        }

        item {

            Spacer(Modifier.height(4.dp))

            FeedbackCard(
                context = context
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

    val titleColor by animateColorAsState(
        targetValue = if (playing) LimeBlue else TextPrimary,
        label = "titleColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (playing) {
            Color(0xFFE8F0FF).copy(alpha = 0.94f)
        } else {
            GlassWhite.copy(alpha = 0.73f)
        },
        shadowElevation = if (playing) 4.dp else 1.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (playing) {
                            Brush.linearGradient(
                                listOf(
                                    LimeBlue,
                                    LimeBlueLight
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFEAF0FA),
                                    Color(0xFFF5F7FB)
                                )
                            )
                        },
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    if (playing) {
                        Icons.Default.GraphicEq
                    } else {
                        Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = if (playing) {
                        Color.White
                    } else {
                        LimeBlue
                    },
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = music.displayTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = if (playing) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                    color = titleColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = music.displayArtist(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = if (playing) {
                        LimeBlue.copy(alpha = 0.78f)
                    } else {
                        TextSecondary
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (playing) {
                    LimeBlueSoft
                } else {
                    Color(0xFFF0F2F6)
                }
            ) {

                Text(
                    text = music.extension.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playing) {
                        LimeBlue
                    } else {
                        Color(0xFF8C94A4)
                    },
                    modifier = Modifier.padding(
                        horizontal = 7.dp,
                        vertical = 4.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    context: Context
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                Toast.makeText(
                    context,
                    "正在打开邮件反馈…",
                    Toast.LENGTH_SHORT
                ).show()

                openMusicFeedbackEmail(context)
            },
        shape = RoundedCornerShape(23.dp),
        color = LimeBluePale.copy(alpha = 0.88f),
        shadowElevation = 2.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 17.dp,
                    vertical = 15.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(43.dp)
                    .background(
                        LimeBlue,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "没有喜欢的歌？",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimeBlue
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "告诉我们想听什么歌曲",
                    fontSize = 12.sp,
                    color = LimeBlue.copy(alpha = 0.68f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "反馈",
                tint = LimeBlue,
                modifier = Modifier.size(22.dp)
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
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = RoundedCornerShape(27.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 10.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 13.dp,
                    vertical = 9.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(49.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    LimeBlue,
                                    LimeBlueLight
                                )
                            ),
                            RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        if (isPlaying) {
                            Icons.Default.GraphicEq
                        } else {
                            Icons.Default.MusicNote
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = music.displayTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = music.displayArtist(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = LimeBlue.copy(alpha = 0.78f)
                    )
                }

                IconButton(
                    onClick = onPrevious
                ) {

                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        tint = TextPrimary
                    )
                }

                Surface(
                    modifier = Modifier.size(43.dp),
                    shape = CircleShape,
                    color = LimeBlue,
                    shadowElevation = 4.dp
                ) {

                    IconButton(
                        onClick = onPlayPause
                    ) {

                        Icon(
                            if (isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isPlaying) {
                                "暂停"
                            } else {
                                "播放"
                            },
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onNext
                ) {

                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        tint = TextPrimary
                    )
                }
            }

            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = onSeek,
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = LimeBlue,
                    activeTrackColor = LimeBlue,
                    inactiveTrackColor = Color(0xFFDCE5F6)
                )
            )
        }
    }
}

@Composable
private fun LoadingView(
    text: String
) {

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = LimeBlue
            )

            Spacer(Modifier.height(15.dp))

            Text(
                text = text,
                fontSize = 14.sp,
                color = LimeBlue
            )
        }
    }
}

@Composable
private fun EmptyView(
    searching: Boolean,
    context: Context
) {

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(
                        LimeBlueSoft,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = LimeBlue
                )
            }

            Spacer(Modifier.height(17.dp))

            Text(
                text = if (searching) {
                    "没有找到相关歌曲"
                } else {
                    "还没有音乐"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (searching) {
                    "换一个关键词试试看"
                } else {
                    "音乐库里暂时没有歌曲"
                },
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .clickable {
                        openMusicFeedbackEmail(context)
                    },
                shape = RoundedCornerShape(18.dp),
                color = LimeBlue
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 11.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "反馈想听的歌曲",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Color(0xFFFFEEF0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFE45D6B),
                    modifier = Modifier.size(29.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = "音乐库加载失败",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(17.dp))

            Surface(
                modifier = Modifier.clickable(
                    onClick = onRetry
                ),
                shape = RoundedCornerShape(16.dp),
                color = LimeBlue
            ) {

                Text(
                    text = "重新加载",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(
                        horizontal = 23.dp,
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
        shape = RoundedCornerShape(19.dp),
        color = Color(0xFF252B38),
        shadowElevation = 10.dp
    ) {

        Row(
            modifier = Modifier.padding(
                start = 15.dp,
                end = 5.dp,
                top = 5.dp,
                bottom = 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = message,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )

            IconButton(
                onClick = onClose
            ) {

                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}
