# 贡献指南

## 环境准备

1. 安装 Android Studio（支持 AGP 9.4）与 JDK 17+。
2. 确认 SDK Platform 37、Build-Tools 36.0.0 已安装。
3. 配置本地 SDK 路径（Android Studio 会自动生成，或手动创建 `local.properties`）：

   ```properties
   sdk.dir=C\:\\path\\to\\Android\\Sdk
   ```

4. 启用提交信息校验钩子：

   ```bash
   git config core.hooksPath .githooks
   ```

## 分支模型

```text
main ─────────●──────────────●────────►
               \            /
develop ──●────●──●────●────●─────────►
           \      /    \  /
   feature/login  ...  feature/lyrics
```

- 功能分支从 `develop` 切出：`git switch -c feature/lyrics develop`
- 发布分支：`release/1.1.0`
- 紧急修复：`hotfix/1.0.1`，从 `main` 切出

## 提交规范

提交信息必须符合 Conventional Commits，否则 `commit-msg` 钩子会拒绝提交。

```text
<type>(<scope>): <subject>
```

| type | 用途 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档变更 |
| `style` | 格式调整（不影响逻辑） |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `build` | 构建脚本 / 依赖 |
| `ci` | CI 配置 |
| `chore` | 其他杂项 |

常用 scope：`player`、`library`、`service`、`lyrics`、`data`、`ui`、`build`、`docs`。

示例：

```text
feat(lyrics): 支持一行多个时间标签
fix(service): 修复切歌后通知未刷新
refactor(data): 抽取 MediaStore 扫描逻辑
```

## 提交前检查

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows 使用 `gradlew.bat`。

请确认没有提交以下内容：

- `local.properties`
- `*.jks` / `*.keystore` / `keystore.properties`
- `app/build/`、`.gradle/`、`.idea/`
- 大体积音频或歌词测试文件

## Pull Request

1. 从 `develop` 切出功能分支。
2. 保持提交粒度清晰，一个提交只做一件事。
3. 提交 PR 到 `develop`，填写 PR 模板中的自测清单。
4. CI 必须通过，至少一名维护者 Review 后合并。
5. 合并后删除功能分支。

## 代码风格

- 统一使用 Java 17，不引入 Kotlin（除非团队另行约定）。
- 缩进 4 空格，行宽建议不超过 120 字符。
- 类名使用 UpperCamelCase，方法与变量使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。
- 回调、线程切换必须明确所在线程；数据库与文件读写不得在主线程执行。
- 新增 Room 字段时同步提升数据库版本并提供迁移，或明确说明使用 `fallbackToDestructiveMigration`。
- 面向用户的文本放入 `strings.xml`，不要硬编码在布局或 Java 中。
- 修改播放行为时，同时检查顺序、列表循环、单曲循环、随机四种组合。
