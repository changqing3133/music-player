# 本地音乐播放器（MusicPlayer）

一个使用 **Java** 编写的 Android 本地音乐播放器课程项目。应用基于 **Media3 ExoPlayer + MediaSessionService** 实现后台播放，使用 **Room** 保存曲库索引，采用 **MVVM** 组织界面与状态。

> 当前状态：`./gradlew testDebugUnitTest` 与 `./gradlew assembleDebug` 均已通过（JDK 25 + Gradle 9.6.0 + AGP 9.4.0）。真机上的播放、通知栏、蓝牙耳机按键等行为需要在设备上做最终验收。

## 功能清单

| 编号 | 功能 | 实现情况 |
| --- | --- | --- |
| F1 | 支持 MP3 / AMR / AAC / OGG 等格式 | Media3 ExoPlayer + 系统编解码器；曲库扫描时按扩展名与 MIME 双重过滤，额外兼容 FLAC / WAV |
| F2 | 播放 / 暂停 / 快进 / 快退 | 播放页按钮 + 可拖动进度条；快进/快退步长 10 秒 |
| F3 | 显示歌曲信息 | 标题、艺术家、专辑、时长、内嵌封面；列表页同步展示 |
| F4 | 同步歌词（可选） | 解析 LRC 时间标签，按播放位置高亮并自动滚动 |
| F5 | 播放列表管理 | MediaStore 扫描、自动入库、循环模式切换、随机播放 |
| F6 | 上一首 / 下一首 | 支持手动切歌；上一首播放超过 3 秒时先回到本曲开头 |
| F7 | 循环模式切换 | 顺序播放 → 列表循环 → 单曲循环，状态持久化 |

此外还包括：

- `MediaSessionService` 前台服务，退到后台、锁屏后继续播放；
- 系统媒体通知、锁屏控制、蓝牙耳机 / 有线耳机按键；
- 音频焦点处理、拔耳机自动暂停（audio becoming noisy）；
- 播放位置、循环模式、随机状态、上次曲目持久化；
- 队列级随机重排，保证随机模式下"上一首 / 下一首"行为可预期；
- 单曲损坏时自动跳过（最多连续 3 首），避免卡死。

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 语言 | Java 17（无 Kotlin） |
| 构建 | Gradle 9.6.0、Android Gradle Plugin 9.4.0 |
| SDK | compileSdk / targetSdk 37，minSdk 24 |
| 播放 | androidx.media3 1.11.1（`media3-exoplayer`、`media3-session`） |
| UI | AppCompat、Material 3、ViewBinding、RecyclerView、Fragment |
| 架构 | MVVM：`PlayerViewModel` + `LiveData` |
| 数据库 | Room 2.8.5（Java `annotationProcessor`） |
| 偏好设置 | SharedPreferences |
| 测试 | JUnit 4.13.2 |

## 目录结构

```text
.
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/musicplayer/
│       │   │   ├── MusicApp.java              # Application：数据库、线程池、仓库单例
│       │   │   ├── data/                      # Room、MediaStore 扫描、SharedPreferences
│       │   │   ├── lyrics/                    # LRC 解析与歌词文件查找
│       │   │   ├── model/                     # LyricLine、PlayerUiState
│       │   │   ├── playback/                  # PlaybackService、ShuffleHelper
│       │   │   ├── ui/                        # Activity、Fragment、Adapter、ViewModel
│       │   │   └── util/                      # 时间格式化、封面加载
│       │   └── res/                           # 布局、菜单、矢量图标、字符串
│       └── test/java/com/example/musicplayer/ # LRC / 洗牌 / 时间格式单元测试
├── gradle/wrapper/                            # Gradle Wrapper
├── .githooks/commit-msg                       # 提交信息校验钩子
├── .github/                                   # CI、PR 与 Issue 模板
├── 技术实施方案.md                             # 课程技术方案文档
├── CHANGELOG.md
├── CONTRIBUTING.md
└── README.md
```

## 架构说明

```text
LibraryFragment / PlayerFragment
            │  (共享 Activity 作用域 ViewModel)
            ▼
       PlayerViewModel  ──────────────►  Room（曲库索引）
            │        │                    ▲
            │        └──── MediaStore 扫描 ┘
            │
            ▼  MediaController（异步连接）
     PlaybackService（MediaSessionService）
            │
            ▼
        ExoPlayer（唯一实例，位于服务进程内）
```

关键设计：

1. **播放实例只存在于服务中**。UI 通过 `MediaController` 控制播放，Activity 销毁不会中断播放，也避免多处持有 `ExoPlayer` 导致状态不一致。
2. **随机播放通过队列重排实现**。启用随机时使用 Fisher–Yates 洗牌，但保持当前曲目下标不变；不使用 ExoPlayer 的 `shuffleModeEnabled`。这样手动"下一首 / 上一首"始终是队列下标 +1/-1，在单曲循环下也能正常切歌。
3. **循环模式直接映射引擎**。顺序 `REPEAT_MODE_OFF`、列表循环 `REPEAT_MODE_ALL`、单曲循环 `REPEAT_MODE_ONE`，切换顺序为 OFF → ALL → ONE → OFF。
4. **歌词按位置二分查找**。`LrcParser.indexAt()` 返回最后一条时间不大于当前播放位置的歌词行，避免每帧线性扫描。
5. **封面异步加载并缓存**。`MediaMetadataRetriever` 读取内嵌图片，按 `inSampleSize` 降采样后放入 `LruCache`，回调统一切回主线程。

## 数据库设计

Room 数据库文件名 `music.db`，当前版本 `1`。

### `tracks`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `uri` | TEXT（主键） | `content://` 音频 URI |
| `media_id` | INTEGER | MediaStore `_ID`，建立索引 |
| `title` / `artist` / `album` | TEXT | 歌曲信息 |
| `album_id` | INTEGER | 专辑 ID |
| `duration_ms` | INTEGER | 时长（毫秒） |
| `mime_type` | TEXT | MIME 类型 |
| `size_bytes` | INTEGER | 文件大小 |
| `date_modified` | INTEGER | 修改时间 |
| `display_name` / `data_path` | TEXT | 文件名与真实路径（用于查找同名歌词） |

### `playlists` / `playlist_items`

预留的自定义歌单结构：

- `playlists(id, name, created_at)`
- `playlist_items(id, playlist_id, track_uri, position)`

当前版本尚未提供自定义歌单的界面入口，播放队列由曲库实时生成。

## 构建与运行

### 环境要求

- Android Studio（建议使用支持 AGP 9.4 的版本）
- JDK 17 及以上（本项目在 JDK 25 下构建通过）
- Android SDK Platform 37、Build-Tools 36.0.0
- 设备或模拟器：Android 7.0（API 24）及以上

### 命令行构建

Windows：

```bat
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

macOS / Linux：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

生成的安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。

也可以通过 Android Studio 直接打开项目根目录并运行 `app` 配置。

### 首次使用

1. 启动应用，允许"音乐和音频"读取权限（Android 13+ 为 `READ_MEDIA_AUDIO`）。
2. 进入曲库页，点击右下角扫描按钮；空曲库首次进入时也会自动扫描。
3. 点击任意歌曲开始播放，并自动跳转到"正在播放"页。
4. Android 13+ 建议允许通知权限，以便在通知栏和锁屏上控制播放。

## 歌词文件

应用会按以下顺序查找歌词：

1. 应用专属歌词目录：
   `Android/data/com.example.musicplayer/files/lyrics/`
2. 音频文件同目录下的同名 `.lrc`（Android 10 及以下通常可用；Android 11+ 受分区存储限制，仅尽力而为）。

支持的文件名（忽略大小写）：

```text
歌名.lrc
歌名 - 歌手.lrc
```

推荐使用 UTF-8 编码；无 BOM 的 GB18030 文件也可自动识别。LRC 支持：

- `[mm:ss.xx]`、`[mm:ss.xxx]`、`[mm:ss]` 时间标签；
- 一行多个时间标签；
- `[offset:+500]` 全局偏移；
- `[ti:]`、`[ar:]` 等元数据行自动忽略。

通过 adb 推送到设备：

```bash
adb push 晴天.lrc /sdcard/Android/data/com.example.musicplayer/files/lyrics/
```

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `READ_MEDIA_AUDIO`（Android 13+） | 读取本地音频 |
| `READ_EXTERNAL_STORAGE`（≤ Android 12） | 读取本地音频 |
| `POST_NOTIFICATIONS`（Android 13+） | 显示媒体通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放前台服务 |
| `WAKE_LOCK` | 播放期间保持 CPU 唤醒 |

所有曲库数据仅保存在本机；应用不申请 `INTERNET` 权限，也不会上传任何文件。（`ACCESS_NETWORK_STATE` 由 Media3 依赖合并，用于检测网络状态，本地播放不会使用。）

## 测试

```bash
./gradlew testDebugUnitTest
```

当前覆盖：

- `LrcParserTest`：基础解析、多时间标签、偏移、负时间保护、元数据过滤、二分查找；
- `ShuffleHelperTest`：保持当前下标、元素不丢失、空队列/单曲/越界下标、媒体 ID 查找；
- `TimeUtilsTest`：分秒与小时格式、负数保护。

## Git 工作流

仓库采用简化版 Git Flow：

| 分支 | 用途 |
| --- | --- |
| `main` | 可发布版本，只接受来自 `release/*` 或 `hotfix/*` 的合并 |
| `develop` | 日常集成分支 |
| `feature/<name>` | 功能开发，从 `develop` 切出，完成后合并回 `develop` |
| `release/<version>` | 发布准备，只修 Bug，验收后合并到 `main` 与 `develop` |
| `hotfix/<name>` | 线上紧急修复，从 `main` 切出，修复后合并到 `main` 与 `develop` |

### 提交规范

提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```text
<type>(<scope>): <subject>
```

允许的 `type`：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`。

示例：

```text
feat(player): 增加 10 秒快进/快退
fix(library): 修复 Android 13 扫描无结果
docs(readme): 补充歌词目录说明
```

提交信息钩子已放在 `.githooks/commit-msg`。新克隆仓库后执行一次：

```bash
git config core.hooksPath .githooks
```

### CI

`.github/workflows/android-ci.yml` 在推送到 `main` / `develop` 以及 Pull Request 时执行：

1. 运行 `testDebugUnitTest`；
2. 构建 `assembleDebug`；
3. 上传 APK 与测试报告为构建产物。

## 已知限制

- 曲库依赖 MediaStore 索引，只显示被系统媒体库收录、`IS_MUSIC != 0` 且时长不少于 10 秒的音频；未索引文件不会出现。
- 其他应用的私有目录（如网易云音乐的 `/Android/data/com.netease.cloudmusic/` 下载目录）不在 `READ_MEDIA_AUDIO` 授权范围内，MediaStore 也不会索引；网易云的 `.ncm` 加密格式无法被第三方播放器解码。可播放的 MP3 / FLAC 等文件需放到公共 `Music` 或 `Download` 目录后再扫描。
- OGG 容器内的 Vorbis / Opus 解码依赖设备提供的 MediaCodec；若目标设备缺少对应解码器，需要额外引入 Media3 解码扩展。
- Android 11+ 无法稳定读取音频同目录的 `.lrc`，请优先使用应用专属歌词目录。
- 自定义歌单表已建立，但界面尚未接入。
- 当前 `minifyEnabled` 为 `false`，发布前需要补充签名配置并开启混淆/资源压缩。
- 未包含 Android 仪器测试，播放链路仍以真机手动验收为主。

## 文档

- [技术实施方案](技术实施方案.md)：需求拆解、技术选型、核心方案与实施建议。

## 许可证

本项目使用 MIT License，详见 [LICENSE](LICENSE)。
