<!-- ppt-master-schema: design-spec/v1 -->
# 第八周中期汇报 - Design Spec

## I. Project Information

| Item | Value |
| --- | --- |
| Project Name | 第八周中期汇报 · Android 本地音乐播放器 |
| Canvas Format | PPT 16:9 (1280 × 720) |
| Page Count | 10 |
| Primary Language | zh-CN |
| Target Audience | 南京邮电大学《嵌入式手机终端开发技术》课程的任课老师与同班同学；老师按五个中期检查点判断进度，同学关注实现方式 |
| Communication Intent | 按老师给出的五个检查点汇报中期进展：先讲整体设计与三人分工，再讲环境统一与功能完成度，最后说明代码集成、报告进度、风险与完成日期 |
| Desired Audience Outcome | 老师能在 10 分钟内确认项目已达到中期要求、功能有可演示的实物，并清楚剩余风险由谁在何时闭环 |
| Core Message / Ask / Action | 必选 6/6、可选 1/1 已完成，代码已集成且构建可复现；真机回归是唯一未闭环项，11/07 前完成 |
| Delivery Context | 课堂现场汇报，1 人主讲，约 8–10 分钟，配合一台安装好 APK 的手机做现场演示；会后 PPT 作为中期材料留存 |
| Artifact Afterlife | 作为中期检查记录归档，并在 11/20 大作业答辩时复用、更新 |
| Reading Mode | balanced |
| Content Strategy | content_divergence 为空，按 balanced 默认：以现有《第八周中期汇报》与课程要求为唯一内容来源，保留全部事实与数字，按五个检查点重组为 10 页教学顺序，不引入外部事实 |
| Design Style | 工程蓝图（instructional + blueprint） |
| AI Image Acquisition Path | not applicable — 确认不使用照片或 AI 图片，封面、架构页与风险页均用原生 SVG 线稿表达 |
| Generation Mode | continuous |
| Spec Refinement | disabled |
| Speaker Notes | enabled — 最终 Stage-2 主动策略（proactive_speaker_notes: true） |
| Custom Animations | enabled — 用户明确希望看到动态演示效果，且最终 Stage-2 主动策略为 true；覆盖对象动画与页间转场 |
| Narration Audio | disabled — 最终 Stage-2 主动策略（proactive_narration_audio: false） |
| Created Date | 2026-10-30 |

## II. Canvas Specification

| Property | Value |
| --- | --- |
| Format | PPT 16:9 |
| Dimensions | 1280 × 720 |
| viewBox | `0 0 1280 720` |
| Margins | 72 px（上、右、下、左） |
| Content Area | 1136 × 576，x 72–1208，y 72–648；页脚页码位于 y 690 安全区外沿 |

## III. Visual Theme

### Theme Style

- **Mode**: instructional
- **Visual style**: blueprint
- **Theme**: 深海蓝图 · 工程审图
- **Tone**: 冷静、精确、可核对，像一张摊开的系统接线图
- **Mode Behavior**: 按“系统如何工作”的教学顺序拆解：先给结论，再逐层讲 UI → ViewModel → Service → 数据，最后用验收清单收束；每页只推进一个理解步骤，标题写清这一页要回答的问题。
- **Visual Style Behavior**: 深蓝图纸底、5% 透明度网格、单一线宽的细线框与直角连接线；组件编号与尺寸标注使用等宽字体，青色只标当前路径，珊瑚色只标风险与未闭环项；无阴影、无渐变，靠线宽与留白分层。

### Color Scheme

| Role | HEX | Purpose |
| --- | --- | --- |
| Background | #071A2B | 图纸底色，所有页面的统一底场 |
| Secondary background | #0C2740 | 分区底、表头带与被强调的象限 |
| Primary | #E6F7FF | 标题、关键数值与结论句 |
| Accent | #22D3EE | 当前路径、已完成状态与关键箭头 |
| Secondary accent | #FB7185 | 风险、未闭环项与需要老师注意的边界 |
| Body text | #CBD5E1 | 正文与清单文字 |
| Secondary text | #7C93A9 | 标注、引线说明、页脚与图号 |
| Divider | #24455F | 网格线、细线框与分隔线 |

## IV. Typography System

### Font Plan

| Role | Character (Reference) | Primary | English if non-English | Fallback tail |
| --- | --- | --- | --- | --- |
| Title | 粗体无衬线 / 高对比、克制 | Microsoft YaHei | Arial | sans-serif |
| Body | 常规无衬线 / 中性、易读 | Microsoft YaHei | Arial | sans-serif |
| Code | 等宽 / 组件编号、接口名与尺寸标注 | Consolas | Consolas | monospace |

- **Role rationale**: Code 家族只用于 blueprint 的技术注记（图号、组件编号、接口名、尺寸与命令），与中文标题/正文形成风格对比，不承担叙述性文字。
- **Title stack**: Microsoft YaHei, Arial, sans-serif
- **Body stack**: Microsoft YaHei, Arial, sans-serif
- **Code stack**: Consolas, monospace

### Font Size Hierarchy

| Purpose | Anchor Size (px) |
| --- | ---: |
| Body | 24 |
| Title | 48 |
| Subtitle | 30 |
| Display | 96 |
| Annotation | 18 |
| Code | 18 |
| Footnote | 16 |

## V. Layout Principles

### Deck-wide Direction

- **Hierarchy direction**: 每页先读到一句判断，再沿青色当前路径读线框与标注，最后落到页脚的结论行
- **Composition tendency**: 以整套线框示意作为页面骨架，文字挂在引线与标注上；不以卡片堆叠组织内容，靠线宽与留白分层
- **Cross-page continuity**: 深蓝图纸底、5% 网格、左上图号与右下页码、青色只标当前路径、珊瑚色只标风险
- **Spacing posture**: 封面与结论页疏，架构、清单与流水线页密；整体保持同一张图纸的密度感
- **Spacing anchors**: 页边距 72 px · 块间距 32 px · 栏间距 40 px · 圆角 2 px · 正文行高 36 px

## VI. Icon Usage Specification

- **Primary bundled library**: tabler-outline
- **Stroke Width**: 1.5

| Icon Path | Suitable Scenarios |
| --- | --- |
| tabler-outline/music | 项目主题、曲库与播放 |
| tabler-outline/device-mobile | 手机端、设备轮廓与本地播放 |
| tabler-outline/headphones | 耳机键、音频输出 |
| tabler-outline/player-play | 播放控制、演示入口 |
| tabler-outline/list-check | 检查点、验收清单 |
| tabler-outline/circle-check | 已完成项与验证通过 |
| tabler-outline/stack-2 | 分层架构、模块堆叠 |
| tabler-outline/users | 团队与分工 |
| tabler-outline/settings | 构建环境与配置 |
| tabler-outline/terminal | 命令行、CI 命令 |
| tabler-outline/git-branch | 分支、代码管理与集成 |
| tabler-outline/brand-github | 代码托管仓库 |
| tabler-outline/package | APK、构建产物 |
| tabler-outline/calendar | 里程碑与日期 |
| tabler-outline/alert-triangle | 风险与未闭环项 |
| tabler-outline/wave-sine | 声波、音频信号 |

## VIII. Image Resource List

| Filename | Dimensions | Ratio | Purpose | Type | Image pattern | Crop Policy | Acquire Via | Status | Reference | text_policy | page_role |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## IX. Content Outline

### Part 1: 结论与检查点

#### Slide 01 - 封面：本地音乐播放器

- **Audience move**: 刚进入会场，只知道这是一次课程汇报 → 知道项目主题、汇报人与今天的主线
- **Relationships**: 项目名为主，课程、汇报人、日期为附属信息；“08”周次与底部播放进度条共同点明课程语境
- **Composition**: 左侧大标题与副标题，右侧线框手机与声波；底部一条课程信息带
- **Title**: 本地音乐播放器
- **Core message**: Android 本地音乐播放器 · 第八周中期汇报
- **Content**: `08 · EMBEDDED MOBILE TERMINAL DEVELOPMENT` / `本地音乐播放器 · Android 本地音乐播放器` / `第八周中期汇报` / `嵌入式手机终端开发技术 · 南京邮电大学 · 2026 秋季` / `汇报人：成员A（组长） · 2026.10.24`
- **Cover impact**: Hook（binding）：巨型周次“08”与一条播放进度条，把“第八周”读成播放时间轴；Composition Reference：左侧标题、右侧线稿设备与声波
- **Motion suggestion**: 标题与周次先落位，播放进度条随后从起点推进到当前周次，右侧声波最后展开

#### Slide 02 - 五个检查点，一条主线

- **Audience move**: 不知道今天会讲什么 → 知道五个检查点、对应顺序与时间分配
- **Relationships**: 五个问题按 01–05 顺序并列，共同服务于一条主线：先证明做了，再证明可交付
- **Composition**: 左侧五个编号问题纵向排列，右侧一条主线标注；底部时间分配
- **Title**: 五个检查点，一条主线
- **Core message**: 不按“我们做了什么”平铺，而是按老师给出的五个检查点逐项回答
- **Content**: `01 设计与分工 · 02 环境与风格 · 03 功能完成度 · 04 集成与报告 · 05 风险与计划` / `8 分钟汇报 · 2 分钟演示与提问`
- **Motion suggestion**: 五个编号按 01→05 依次出现

#### Slide 03 - 结论：6/6 必选、1/1 可选已全部完成

- **Audience move**: 不确定项目是否达到中期要求 → 确认课程要求已全部完成，并额外补齐三项系统级能力
- **Relationships**: 必选 6、可选 1、扩展 3 三个计数并列；“本地优先”是贯穿三者的共同约束
- **Composition**: 顶部结论句；中部三个计数块；右侧本地优先说明与支持格式列表
- **Title**: 结论：6/6 必选、1/1 可选已全部完成
- **Core message**: 课程要求全部完成，并额外补齐后台播放、媒体通知与文件管理器打开
- **Content**: `6/6 必选 · 1/1 可选 · 3 项扩展` / `单一播放实例 · 单一状态出口 · 单一视觉语言` / `本地优先：不申请 INTERNET 权限，扫描、播放、歌词全部在本机完成` / `MP3 · AMR · AAC · OGG · FLAC · WAV`
- **Motion suggestion**: 三个计数先出现，本地优先约束最后收束

### Part 2: 系统如何工作

#### Slide 04 - 三个人的代码，怎么合成一个应用？

- **Audience move**: 不清楚三个人如何合成一个应用 → 理解四层分工、命令下行与状态上行
- **Relationships**: UI → PlayerViewModel → MediaController / Repository → PlaybackService / Room 四层；命令向下、状态向上；“唯一播放实例”是整张图的中心约束
- **Composition**: 四层横向分层图，左侧下行命令箭头、右侧上行状态箭头；青色高亮当前播放路径
- **Title**: 三个人的代码，怎么合成一个应用？
- **Core message**: 播放实例只存在于 PlaybackService，UI 通过 MediaController 发命令、经 LiveData 收状态
- **Content**: `UI：MainActivity / LibraryFragment / PlayerFragment` / `ViewModel：命令转发 · 状态汇聚 · LiveData` / `服务：PlaybackService / ExoPlayer 唯一实例 · 前台服务 · 媒体通知` / `数据：Room + MediaStore · .lrc` / `接口：playTrack · togglePlayPause · seekTo · next/previous · scanLibrary · playExternalUri · importLyrics`
- **Motion suggestion**: 按 UI → ViewModel → 服务 → 数据逐层出现，最后同时点亮命令与状态两条箭头

#### Slide 05 - 三个人各自负责什么？

- **Audience move**: 担心分工重叠或遗漏 → 知道每人 33% 主责、交付物与共同承担
- **Relationships**: 三人并列；每人内部为“主责模块 → 交付物 → 个人报告重点”三段式；底部共同承担横跨三人
- **Composition**: 三列线框 Owner 卡，顶部各自 33%；底部共同交付带
- **Title**: 三个人各自负责什么？
- **Core message**: 按模块 Owner 制拆分，每人都有一份可验收的交付物
- **Content**: `A · 播放与集成：PlaybackService/ExoPlayer · MediaSession · Git/CI` / `B · 数据与队列：MediaStore · Room 三表 · 队列/随机/循环 · 测试` / `C · UI 与歌词：三页面 · 交互 · LRC 解析与导入 · 演示素材` / `每人 33% · 共同：15 页个人报告 · 联调测试 · 答辩准备`
- **Motion suggestion**: 三列按 A→B→C 依次出现，底部共同承担最后压上

### Part 3: 如何保证可复现

#### Slide 06 - 怎么保证三个人的环境一致？

- **Audience move**: 不知道如何保证环境与风格一致 → 知道统一靠 Wrapper、依赖锁定、提交钩子与 CI
- **Relationships**: 环境基线 / 代码管理 / 界面一致 三组并列，各五条；底部一条 CI 命令把三组收束为同一结果
- **Composition**: 三列清单，顶部统一标题；底部等宽命令行与“同一命令同一结果”标注
- **Title**: 怎么保证三个人的环境一致？
- **Core message**: 统一不是写在文档里，而是固化在 Wrapper、固定依赖版本、提交钩子和 CI 里
- **Content**: `环境：JDK 25 · Gradle 9.6.0 · AGP 9.4.0 · SDK 37 · minSdk 24 · Wrapper 锁定` / `协作：Git + GitHub · main/develop + 功能分支 · Conventional Commits · PR 模板 · Actions` / `界面：Material 3 令牌 · 8dp 间距 · 共用 item_track · strings.xml` / `./gradlew testDebugUnitTest assembleDebug`
- **Motion suggestion**: 三列先出现，底部命令行最后出现并高亮

#### Slide 07 - 必选、可选、扩展，完成到什么程度？

- **Audience move**: 不知道功能完成到什么程度 → 知道 6/6、1/1、3 项扩展逐项状态与唯一未闭环项
- **Relationships**: 必选 6 项逐条勾选，可选 1 项单列，扩展 3 项并列；验证状态独立成组，珊瑚色标出真机回归
- **Composition**: 左侧六行必选清单，右侧可选与扩展，底部验证状态带
- **Title**: 必选、可选、扩展，完成到什么程度？
- **Core message**: 构建与 13 项单测通过；唯一未闭环项是真机回归，计划 11/07 前完成
- **Content**: `✓ 多格式播放 · 播放控制 · 歌曲信息 · 扫描与播放列表 · 上一首/下一首 · 循环/随机` / `✓ F4 同步歌词：LRC 解析 · 当前行高亮 · 自动滚动 · 应用内导入` / `扩展 3 项：后台播放 · 媒体通知/锁屏/耳机键 · 文件管理器“打开方式”` / `构建通过 · 单测 13/13 · 真机回归待完成（11/07 前）`
- **Motion suggestion**: 六项必选按顺序打勾出现，验证状态最后出现，未闭环项以珊瑚色落在最后

#### Slide 08 - 代码真的集成、真的可复现吗？

- **Audience move**: 不确定代码是否真的集成 → 看到一条每次 push 自动跑通的流水线
- **Relationships**: 五个 CI 步骤按执行顺序串联；13/13、0 lint、11 MB 三个结果并列在流水线末端
- **Composition**: 左侧纵向流水线步骤，右侧三个结果读数；底部仓库地址与结论句
- **Title**: 代码真的集成、真的可复现吗？
- **Core message**: 每次 push 自动测试并产出 APK，构建可复现
- **Content**: `Checkout → JDK 25 → Android SDK → Unit tests 13/13 → Assemble debug APK → Upload artifact` / `13 单测通过 · 0 Lint 错误 · 11 MB APK` / `单 app 模块 · main 分支 · 全员合并到同一 APK` / `github.com/changqing3133/music-player`
- **Motion suggestion**: 流水线五步自上而下依次点亮，三个结果读数最后同时出现

### Part 4: 风险与收束

#### Slide 09 - 还剩哪些风险，谁来闭环？

- **Audience move**: 担心未完成项没有着落 → 看到三项风险的边界、负责人与日期
- **Relationships**: 三项风险按 R1–R3 并列，各带 Owner 与状态；右侧里程碑按日期顺序推进到 11/20
- **Composition**: 左侧风险清单，右侧时间轴；日期节点成为视觉节拍
- **Title**: 还剩哪些风险，谁来闭环？
- **Core message**: 剩余工作是验证与打磨，不是从零开发；真机回归 11/07 前闭环
- **Content**: `R1 私有目录与 .ncm 不支持 · B · README 已标注边界` / `R2 歌词仅本地 LRC · C · 应用内导入已完成` / `R3 真机回归未闭环 · A · 11/07` / `10/24 中期 → 11/07 回归完成 → 11/15 报告定稿 → 11/20 大作业截止`
- **Motion suggestion**: 风险按 R1→R3 出现，时间轴从 10/24 推进到 11/20

#### Slide 10 - 把“能编译”变成“真机稳定可演示”

- **Audience move**: 听完细节 → 记住下一步、仓库与下一检查点
- **Relationships**: 一句主张为主，仓库、APK、下一检查点三项落地信息并列
- **Composition**: 左侧大字主张，右侧三项信息；底部致谢与院校信息
- **Title**: 把“能编译”变成“真机稳定可演示”
- **Core message**: 代码已集成、构建可复现；接下来两周逐项真机验收
- **Content**: `代码已集成 · 构建可复现 · 真机逐项验收` / `REPOSITORY changqing3133/music-player` / `APK app-debug.apk · 11 MB` / `NEXT 11/07 真机回归完成` / `谢谢，请老师和同学批评指正`
- **Closing impact**: Takeaway（binding）：把“能编译”变成“真机稳定可演示”；Composition Reference：左侧主张、右侧三项落地信息
- **Motion suggestion**: 主张先出现，三项信息随后并列出现

## X. Speaker Notes Requirements

- **Generation**: enabled
- **Filename**: match each SVG filename under `notes/`
- **Content**: 每页 60–90 秒、总时长 8–10 分钟的口语化讲稿；以最终 SVG 的实际内容为依据，数字与日期必须与页面一致；保留开场致谢、现场演示提示与结尾提问邀请
- **Total duration**: 8–10 分钟
- **Notes style**: conversational
- **Presentation purpose**: 按老师给出的五个检查点汇报中期进展：先讲整体设计与三人分工，再讲环境统一与功能完成度，最后说明代码集成、报告进度、风险与完成日期
