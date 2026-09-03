# Lime Music

Lime Music 是一个原生 Android 音乐播放器项目。

## 音乐源

默认扫描：

- GitHub: `lime10764/music`
- 分支：`main`

App 会通过 GitHub Contents API 自动递归扫描音乐文件，不要求维护 `manifest.json`。

支持的音乐扩展名：

`mp3 / flac / wav / ogg / m4a / aac / opus / wma / aiff / ape / wv / caf / webm`

## 构建

项目使用：

- Android Gradle Plugin 8.13.0
- Kotlin 2.2.20
- Java 17
- Compose
- Media3 ExoPlayer

GitHub Actions 工作流位于：

`.github/workflows/build-apk.yml`

推送到 GitHub 后，可以在 Actions 中手动运行 `Build Lime Music APK`，构建完成后下载 `LimeMusic-release` artifact。

## 注意

这是当前项目的基础可运行版本，后续可以继续完善 Liquid Glass UI、歌词、收藏、播放历史、专辑封面、设置等功能。
