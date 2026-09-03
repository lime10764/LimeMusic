package com.limemusic.app.ui

import android.content.Context
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limemusic.app.data.MusicItem
import com.limemusic.app.data.MusicLibraryState

private val Blue = Color(0xFF3478F6)
private val BlueSoft = Color(0xFFEAF2FF)

private val Background = Color(0xFFF8F9FC)
private val SurfaceColor = Color(0xFFFFFFFF)

private val TextPrimary = Color(0xFF181B22)
private val TextSecondary = Color(0xFF858B98)
private val DividerColor = Color(0xFFE9EBF0)

private fun openMusicFeedbackEmail(context: Context) {

    val email = "3327544159@qq.com"

    val subject = Uri.encode("Lime Music 歌曲反馈")

    val body = Uri.encode(
        """
您好，Lime Music：

我没有找到喜欢的歌曲，希望添加以下歌曲：

歌曲：
歌手：
歌曲链接/来源：
其他建议：

谢谢！
        """.trimIndent()
    )

    val uri = Uri.parse(
        "mailto:$email?subject=$subject&body=$body"
    )

    try {
        context.startActivity(
            Intent(
                Intent.ACTION_SENDTO,
                uri
            )
        )
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

    val context = androidx.compose.ui.platform.LocalContext.current

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
            .background(Background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {

            TopBar(
                count = when (libraryState) {
                    is MusicLibraryState.Success ->
                        (libraryState as MusicLibraryState.Success).count

                    else ->
                        musicList.size
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

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                when (val state = libraryState) {

                    MusicLibraryState.Idle -> {
                        LoadingView("准备音乐库")
                    }

                    MusicLibraryState.Loading -> {
                        LoadingView("正在同步音乐")
                    }

                    is MusicLibraryState.Error -> {

                        if (musicList.isNotEmpty()) {

                            MusicList(
                                musicList = musicList,
                                currentMusic = currentMusic,
                                isPlaying = isPlaying,
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

                            ErrorView(
                                message = state.message,
                                onRetry = musicViewModel::refreshMusic
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
                                isPlaying = isPlaying,
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

                currentMusic?.let { music ->
        MiniPlayer(
            currentMusic = music,
            isPlaying = isPlaying,
            progress = progress,
            onPrevious = playerViewModel::previous,
            onPlayPause = playerViewModel::togglePlayPause,
            onNext = playerViewModel::next,
            onSeek = playerViewModel::seekFraction
        )
    }
            }
        }

        AnimatedVisibility(
            visible = playerError != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = 12.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
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
private fun TopBar(
    count: Int,
    onRefresh: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(
                horizontal = 20.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Lime Music",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = if (count > 0) {
                    "$count 首歌曲"
                } else {
                    "音乐库"
                },
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = SurfaceColor,
                    shape = CircleShape
                )
                .clickable(
                    onClick = onRefresh
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新",
                tint = Blue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp
            )
            .height(48.dp)
            .background(
                color = SurfaceColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(
                horizontal = 13.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                decorationBox = { innerTextField ->

                    if (query.isEmpty()) {

                        Text(
                            text = "搜索歌曲或歌手",
                            fontSize = 14.sp,
                            color = Color(0xFF9AA0AB)
                        )
                    }

                    innerTextField()
                }
            )
        }

        if (query.isNotEmpty()) {

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable(
                        onClick = onClear
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicList(
    musicList: List<MusicItem>,
    currentMusic: MusicItem?,
    isPlaying: Boolean,
    context: Context,
    onMusicClick: (Int) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = 14.dp
        )
    ) {

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 3.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "全部歌曲",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "${musicList.size}",
                    fontSize = 11.sp,
                    color = Blue
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
                isCurrent = currentMusic?.id == music.id,
                isPlaying = isPlaying && currentMusic?.id == music.id,
                onClick = { onMusicClick(index) }
            )
        }

        item {

            FeedbackEntry(
                context = context
            )
        }
    }
}

@Composable
private fun MusicRow(
    music: MusicItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f
        ),
        label = "rowScale"
    )

    val background by animateColorAsState(
        targetValue = if (isCurrent)
            BlueSoft
        else
            Color.Transparent,
        animationSpec = tween(180),
        label = "rowBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isCurrent) BlueSoft
                    else Color(0xFFF1F3F7)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent && isPlaying) {

                val transition =
                    rememberInfiniteTransition(label = "playing")

                val pulse by transition.animateFloat(
                    initialValue = 0.82f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "playingPulse"
                )

                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "正在播放",
                    tint = Blue,
                    modifier = Modifier
                        .size(22.dp)
                )
            } else if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "已暂停",
                    tint = Blue,
                    modifier = Modifier.size(21.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Spacer(Modifier.width(11.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = music.displayTitle(),
                color = if (isCurrent) Blue else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrent)
                    FontWeight.SemiBold
                else
                    FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = music.displayArtist(),
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = music.extension
                ?.uppercase()
                ?.takeIf { it.isNotBlank() }
                ?: "AUDIO",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
private fun FeedbackEntry(
    context: Context
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable {

                Toast.makeText(
                    context,
                    "正在打开邮件反馈…",
                    Toast.LENGTH_SHORT
                ).show()

                openMusicFeedbackEmail(context)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "没有喜欢的歌？",
            fontSize = 13.sp,
            color = Blue,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = "反馈给我们",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MiniPlayer(
    currentMusic: MusicItem,
    isPlaying: Boolean,
    progress: Float,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit
) {
    val playInteraction = remember {
        MutableInteractionSource()
    }

    val pressed by playInteraction.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 600f
        ),
        label = "buttonScale"
    )

    val transition =
        rememberInfiniteTransition(label = "miniPlayer")

    val iconScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isPlaying) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        shape = RoundedCornerShape(17.dp),
        color = SurfaceColor,
        shadowElevation = 3.dp
    ) {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying)
                            Icons.Default.MusicNote
                        else
                            Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

                Spacer(Modifier.width(9.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentMusic.displayTitle(),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = currentMusic.displayArtist(),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(35.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .size(37.dp)
                        .clip(CircleShape)
                        .background(Blue)
                ) {
                    Icon(
                        imageVector =
                            if (isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                        contentDescription =
                            if (isPlaying) "暂停"
                            else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(35.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Blue,
                    activeTrackColor = Blue,
                    inactiveTrackColor = DividerColor
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 2.5.dp,
                color = Blue
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = text,
                fontSize = 13.sp,
                color = TextSecondary
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFB7BDC8)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = if (searching) {
                    "没有找到相关歌曲"
                } else {
                    "音乐库是空的"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = if (searching) {
                    "换个关键词试试看"
                } else {
                    "暂时没有可播放的歌曲"
                },
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Text(
                text = "反馈想听的歌曲",
                modifier = Modifier.clickable {
                    openMusicFeedbackEmail(context)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Blue
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {

            Text(
                text = "音乐库加载失败",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = message,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = "重新加载",
                modifier = Modifier.clickable(
                    onClick = onRetry
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Blue
            )
        }
    }
}

@Composable
private fun ErrorToast(
    message: String,
    onClose: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF292D35),
        shadowElevation = 8.dp
    ) {

        Row(
            modifier = Modifier
                .padding(
                    start = 14.dp,
                    end = 3.dp,
                    top = 3.dp,
                    bottom = 3.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                fontSize = 12.sp
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
