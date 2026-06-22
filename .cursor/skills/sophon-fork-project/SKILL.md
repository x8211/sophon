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

---

## 执行清单

收集好参数后，按顺序执行以下步骤，每步完成后打勾：

```
- [ ] Step 1: 复制源码目录
- [ ] Step 2: 修改 gradle.properties
- [ ] Step 3: 修改 settings.gradle.kts
- [ ] Step 4: 修改 composeApp/build.gradle.kts
- [ ] Step 5: 重命名 Kotlin 源码目录
- [ ] Step 6: 批量替换包名/导入声明
- [ ] Step 7: 替换图标文件
- [ ] Step 8: 验证构建
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

将以下两行替换：
```properties
# 旧
group=sophon
appName=Sophon

# 新
group={NEW_PKG_PREFIX}
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

## Step 6：批量替换包名/导入声明

使用 `find + sed` 对所有 `.kt` 文件执行批量替换：

```bash
cd {DEST_DIR}

# 替换 package 声明和 import 语句中的包路径
find . -name "*.kt" -not -path "*/build/*" | \
  xargs sed -i '' \
    -e 's/package sophon\.desktop/package {NEW_PKG_PREFIX}.desktop/g' \
    -e 's/import sophon\.desktop/import {NEW_PKG_PREFIX}.desktop/g'

# 替换 proguard-rules.pro 中的包引用（如存在）
find . -name "*.pro" -not -path "*/build/*" | \
  xargs sed -i '' 's/sophon\.desktop/{NEW_PKG_PREFIX}.desktop/g' 2>/dev/null || true
```

> 注意：macOS `sed` 的 `-i` 需要加 `''` 参数（`-i ''`），Linux 上直接用 `-i`。

替换完成后验证无残留引用：
```bash
rg "sophon\.desktop" --include="*.kt" --include="*.pro" -l
# 预期输出：空（无文件）
```

---

## Step 7：替换图标文件

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

## Step 8：验证构建

```bash
cd {DEST_DIR}
./gradlew :composeApp:compileKotlinDesktop
```

构建成功则任务完成。如有错误：
1. 搜索残留的旧包名：`rg "sophon" --include="*.kt" -l`
2. 检查 `build.gradle.kts` 中是否遗漏了某处 `sophon` 字符串
3. 检查 `generateAppInfo` task 生成的 `AppInfo.kt` 包名是否已更新（见 Step 4d）

---

## 注意事项

- **数据隔离**：`CACHE_HOME` 由 `APP_NAME` 推导（`~/.{APP_NAME}`），修改 `appName` 后子项目的用户数据目录会自动隔离，无需额外处理。
- **DataStore 文件**：若 `DataStoreUtils.kt` 中硬编码了文件名（非仅依赖 `CACHE_HOME`），需额外检查并更新。
- **Windows UUID**：`upgradeUuid` 与 Sophon 原始值不同是强制要求，否则 Windows 安装程序会认为两者是同一应用并互相覆盖。
- **ProGuard**：`proguard-rules.pro` 中如有 `-keep class sophon.**` 类似规则，Step 6 的 `sed` 命令会自动处理 `.pro` 文件；完成后手动确认规则仍然正确。
- **Git 历史**：fork 后建议立即 `git init` + 初始提交，与 Sophon 的 git 历史彻底解耦。
