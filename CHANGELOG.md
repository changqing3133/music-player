# Changelog

本文件记录项目的显著变更，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Planned

- 自定义歌单界面与歌单导入/导出
- 播放列表拖拽排序
- 仪器测试与播放链路自动化测试

## [1.0.0] - 2026-10-17

### Added

- MediaStore 本地音频扫描，支持 MP3、AAC/M4A、AMR、OGG/OPUS 等格式，自动写入 Room 曲库。
- 基于 Media3 ExoPlayer 的播放内核，支持播放、暂停、拖动进度、快进/快退 10 秒。
- 上一首 / 下一首；上一首在播放超过 3 秒时回到本曲开头。
- 循环模式切换：顺序播放、列表循环、单曲循环，状态持久化。
- 队列级随机播放：洗牌时保持当前曲目下标，手动切歌行为稳定。
- MediaSessionService 前台播放服务，支持通知栏、锁屏、蓝牙/有线耳机控制。
- 歌曲信息展示：标题、艺术家、专辑、时长与内嵌封面。
- LRC 歌词解析与同步高亮：多时间标签、毫秒/厘秒精度、`offset` 偏移、UTF-8 / GB18030 自动识别。
- Room 数据库（`tracks`、`playlists`、`playlist_items`）与播放状态持久化。
- Android 13+ `READ_MEDIA_AUDIO`、通知权限适配。
- 单元测试：LRC 解析、随机洗牌、时间格式化。
- Git 管理：分支模型文档、Conventional Commits 钩子、GitHub Actions CI、PR 与 Issue 模板。
