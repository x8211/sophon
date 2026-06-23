---
name: sophon-fork-project
description: 以 Sophon 项目为孵化器，fork 出一个新的子项目。子项目拥有不同的应用名称、包名（Kotlin 包路径 + bundleID）、Windows 升级 UUID 和图标，但功能代码完全一致。使用时机：用户说"创建子项目"、"fork 出新项目"、"以 Sophon 为模板创建新 App"、"新增一个同功能不同名的应用"等。
disable-model-invocation: true
---

# Sophon Fork Project

以 Sophon 项目为孵化器，创建功能完全一致、但品牌（名称/包名/图标）不同的子项目。

## 所需信息

开始前向用户确认以下参数（用 AskQuestion 工具）：

| 参数 | 说明 | 示例 |
|---|---|---|
| `NEW_APP_NAME` | 新应用显示名称 | `Hermes` |
| `NEW_PKG_PREFIX` | 新 Kotlin 包前缀（全小写，无点） | `hermes` |
| `NEW_BUNDLE_ID` | macOS Bundle ID | `hermes.desktop` |
| `DEST_DIR` | 目标目录绝对路径 | `/Users/mico/projects/hermes` |
| 图标文件 | 用户提供的三个图标文件路径（.icns/.ico/.png）或说明稍后替换 |  |

`NEW_PKG_PREFIX` 决定了新 Kotlin 包路径，原始包路径为 `sophon/desktop/`，新路径为 `{NEW_PKG_PREFIX}/desktop/`。

> **多级包前缀**：若新项目需要多级包路径（如 `com/myapp` → `com.myapp.desktop`），Step 5 中 `mv` 需手动创建中间目录，Step 6 的 sed 模式需调整为完整的多级路径匹配。建议优先使用单级前缀以避免复杂性。

---

## 执行清单

收集好参数后，按顺序执行以下步骤，每步完成后打勾：

```
- [ ] Step 1: 复制源码目录
- [ ] Step 2: 修改 gradle.properties
- [ ] Step 3: 修改 settings.gradle.kts
- [ ] Step 4: 修改 composeApp/build.gradle.kts
- [ ] Step 5: 重命名 Kotlin 源码目录
- [ ] Step 6: 批量替换包名/导入声明（含全量 sophon 字符串扫描）
- [ ] Step 7: 同步 Agent Rules（CLAUDE.md / AGENTS.md）
- [ ] Step 8: 替换图标文件
- [ ] Step 9: 验证构建
```

---

## Step 1：复制源码目录

```bash
# 从 Sophon 根目录复制，排除 build 产物和 .idea
rsync -av --exclude='.git' --exclude='.idea' --exclude='build' \
  /Users/mico/projects/henrywang92000/sophon/ \
  {DEST_DIR}/
cd {DEST_DIR}
git init && git add -A && git commit -m "chore: fork from Sophon"
```

---

## Step 2：修改 gradle.properties

文件位置：`{DEST_DIR}/gradle.properties`

将以下一行替换：
```properties
# 旧
appName=Sophon

# 新
appName={NEW_APP_NAME}
```

---

## Step 3：修改 settings.gradle.kts

文件位置：`{DEST_DIR}/settings.gradle.kts`

```kotlin
// 旧
rootProject.name = "Sophon"

// 新
rootProject.name = "{NEW_APP_NAME}"
```

---

## Step 4：修改 composeApp/build.gradle.kts

文件位置：`{DEST_DIR}/composeApp/build.gradle.kts`

需修改三处：

**4a. mainClass**
```kotlin
// 旧
mainClass = "sophon.desktop.MainKt"

// 新
mainClass = "{NEW_PKG_PREFIX}.desktop.MainKt"
```

**4b. macOS bundleID 与 dockName**
```kotlin
// 旧
bundleID = "sophon.desktop"

// 新
bundleID = "{NEW_BUNDLE_ID}"
```

**4c. Windows upgradeUuid（必须生成全新 UUID，防止与 Sophon 安装包冲突）**

用 Shell 生成新 UUID：
```bash
python3 -c "import uuid; print(str(uuid.uuid4()).upper())"
```
将生成结果填入：
```kotlin
upgradeUuid = "生成的新UUID"
```

**4d. generateAppInfo task 中的包路径**

将 `outputDir.get().file("sophon/desktop/generated/AppInfo.kt")` 替换为：
```kotlin
outputDir.get().file("{NEW_PKG_PREFIX}/desktop/generated/AppInfo.kt")
```

以及生成文件中的 package 声明：
```kotlin
package sophon.desktop.generated
// →
package {NEW_PKG_PREFIX}.desktop.generated
```

---

## Step 5：重命名 Kotlin 源码目录

原始路径：`composeApp/src/desktopMain/kotlin/sophon/`  
目标路径：`composeApp/src/desktopMain/kotlin/{NEW_PKG_PREFIX}/`

```bash
cd {DEST_DIR}
mv composeApp/src/desktopMain/kotlin/sophon \
   composeApp/src/desktopMain/kotlin/{NEW_PKG_PREFIX}
```

同理处理测试目录（如存在）：
```bash
mv composeApp/src/desktopTest/kotlin/sophon \
   composeApp/src/desktopTest/kotlin/{NEW_PKG_PREFIX} 2>/dev/null || true
```

---

## Step 6：批量替换包名/导入声明（含全量 sophon 字符串扫描）

使用 `find + sed` 对所有 `.kt` 文件执行批量替换：

```bash
cd {DEST_DIR}

# 替换 package 声明和 import 语句中的包路径
find . -name "*.kt" -not -path "*/build/*" -exec sed -i '' \
    -e 's/package sophon\.desktop/package {NEW_PKG_PREFIX}.desktop/g' \
    -e 's/import sophon\.desktop/import {NEW_PKG_PREFIX}.desktop/g' {} \;

# 替换 CA 证书相关命名（CertificateAuthority.kt、PacketCaptureRepositoryImpl.kt、PacketCaptureScreen.kt）
# 注意：CN=Sophon CA 为首字母大写，grep -r "sophon" 不会捕获，必须单独处理
find . -name "*.kt" -not -path "*/build/*" -exec sed -i '' \
    -e 's/CN=Sophon CA, O=Sophon/CN={NEW_APP_NAME} CA, O={NEW_APP_NAME}/g' \
    -e 's/SophonCA\.crt/{NEW_APP_NAME}CA.crt/g' \
    -e 's/sophon_ca\.crt/{NEW_PKG_PREFIX}_ca.crt/g' \
    -e 's/sophon_ca\.key/{NEW_PKG_PREFIX}_ca.key/g' {} \;

# 替换 proguard-rules.pro 中的包引用（如存在）
find . -name "*.pro" -not -path "*/build/*" -exec sed -i '' \
    's/sophon\.desktop/{NEW_PKG_PREFIX}.desktop/g' {} \; 2>/dev/null || true
```

> 注意：macOS `sed` 的 `-i` 需要加 `''` 参数（`-i ''`）；`xargs` 在 macOS 沙箱环境下可能报 `sysconf` 错误，改用 `-exec` 更稳定。

**Step 6 完成后执行全量扫描**，除包名格式外，还有多类残留需手动修复。扫描需使用 `-i`（忽略大小写）以捕获 `Sophon`（首字母大写）形式：

```bash
# 全量扫描所有文本文件（-i 同时捕获 sophon / Sophon，如 "Sophon CA"、"SophonCA.crt"）
grep -ri "sophon" {DEST_DIR} \
  --include="*.kt" --include="*.kts" --include="*.toml" \
  --include="*.md" --include="*.pro" --include="*.properties" \
  --exclude-dir=".git" --exclude-dir="build" -l
```

常见残留类型及修复方式：

| 类型 | 示例 | 处理方式 |
|---|---|---|
| KDoc 类引用 | `[sophon.desktop.feature.xxx.Bar]` | sed 替换为新包名 |
| 临时文件名字符串 | `"sophon-proto"`, `"sophon_dl_"` | sed 替换为新项目名 |
| 文件路径注释 | `~/.sophon/proto_paths.json` | sed 替换为 `~/.{NEW_APP_NAME}/` |
| 文档标题/描述 | `# Sophon 项目编码总纲` | sed 替换为新项目名 |
| 目录树示例 | `sophon/` 根目录名 | sed 替换 |
| **CA 证书 DN**（首字母大写） | `CN=Sophon CA, O=Sophon` | 由上方 CA 专项 sed 处理，**grep 扫描须加 `-i`** |
| **CA 文件名**（小写） | `sophon_ca.crt`, `sophon_ca.key` | 由上方 CA 专项 sed 处理 |
| **CA adb push 路径 / UI 文案** | `/sdcard/SophonCA.crt` | 由上方 CA 专项 sed 处理 |

---

## Step 7：同步 Agent Rules（CLAUDE.md / AGENTS.md）

Sophon 使用 `CLAUDE.md`（完整规范）+ `AGENTS.md`（仅一行引用）的双文件结构，需一并替换品牌信息：

```bash
cd {DEST_DIR}

# 1. 删除旧的 .agent/rules/AGENTS.md（若存在）
rm -f .agent/rules/AGENTS.md

# 2. 批量替换 CLAUDE.md 中的项目名与包名
sed -i '' \
  -e 's/sophon\.desktop/{NEW_PKG_PREFIX}.desktop/g' \
  -e 's/sophon\/desktop/{NEW_PKG_PREFIX}\/desktop/g' \
  -e 's/\*\*Sophon\*\*/**{NEW_APP_NAME}**/g' \
  -e 's/^# Sophon /# {NEW_APP_NAME} /' \
  -e 's/^sophon\/$/{NEW_PKG_PREFIX}\//' \
  CLAUDE.md
```

完成后用全量扫描确认 `CLAUDE.md` 中无残留 `sophon`：
```bash
grep "sophon" CLAUDE.md || echo "clean"
```

---

## Step 8：替换图标文件

图标目录：`{DEST_DIR}/composeApp/src/desktopMain/launcher/`

需要覆盖以下三个文件：
- `icon.icns`：macOS 图标（512×512 及多分辨率）
- `icon.ico`：Windows 图标（256×256 多尺寸 .ico）
- `icon.png`：通用预览图（512×512 PNG）

**如用户已提供图标文件：**
```bash
cp {用户提供的.icns路径} {DEST_DIR}/composeApp/src/desktopMain/launcher/icon.icns
cp {用户提供的.ico路径}  {DEST_DIR}/composeApp/src/desktopMain/launcher/icon.ico
cp {用户提供的.png路径}  {DEST_DIR}/composeApp/src/desktopMain/launcher/icon.png
```

**如用户暂未提供图标：** 告知用户稍后手动替换上述三个文件，并记录路径。

---

## Step 9：验证构建

```bash
cd {DEST_DIR}
./gradlew :composeApp:compileKotlinDesktop
```

构建成功则任务完成。如有错误：
1. 执行全量扫描定位残留：`grep -ri "sophon" . --include="*.kt" --include="*.kts" --include="*.pro" --exclude-dir=".git" --exclude-dir="build" -l`
2. 检查 `build.gradle.kts` 中是否遗漏了某处 `sophon` 字符串
3. 检查 `generateAppInfo` task 生成的 `AppInfo.kt` 包名是否已更新（见 Step 4d）

---

## 注意事项

- **数据隔离**：`CACHE_HOME` 由 `APP_NAME` 推导（`~/.{APP_NAME}`），修改 `appName` 后子项目的用户数据目录会自动隔离，无需额外处理。但代码中可能存在硬编码了 `sophon` 的路径字符串（如 `~/.sophon/`），需通过 Step 6 的全量扫描找到并手动替换。
- **DataStore 文件**：若 `DataStoreUtils.kt` 中硬编码了文件名（非仅依赖 `CACHE_HOME`），需额外检查并更新。
- **Windows UUID**：`upgradeUuid` 与 Sophon 原始值不同是强制要求，否则 Windows 安装程序会认为两者是同一应用并互相覆盖。
- **ProGuard**：`proguard-rules.pro` 中如有 `-keep class sophon.**` 类似规则，Step 6 的 `sed` 命令会自动处理 `.pro` 文件；完成后手动确认规则仍然正确。
- **临时文件名**：代码中使用 `File.createTempFile("sophon-xxx", ...)` 的地方不影响功能但会保留旧品牌痕迹，Step 6 全量扫描时一并替换。
- **Git 历史**：fork 后建议立即 `git init` + 初始提交，与 Sophon 的 git 历史彻底解耦。
