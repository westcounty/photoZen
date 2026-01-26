# PhotoZen 项目规则

## 沟通语言

**始终使用中文与用户沟通。** 包括代码注释、提交信息、文档说明等都使用中文。

---

## Git 分支规则

**默认在 main 分支进行开发。** 除非用户特别指定其他分支，所有开发工作都在 main 分支上进行。

---

## ⚠️ 关键操作检查清单

### 当用户要求「打包」「构建 release」「assembleRelease」时，必须执行：

1. **构建 APK**
   ```bash
   cd "D:/work/photoZen" && ./gradlew.bat assembleRelease
   ```

2. **读取版本号**
   ```bash
   cat D:/work/photoZen/build_version.properties
   ```

3. **重命名 APK**（必须执行！）
   ```bash
   cp app/build/outputs/apk/release/app-release.apk \
      app/build/outputs/apk/release/PhotoZen-release-v{major}.{minor}.{patch}.{build}.apk
   ```

4. **告知用户 APK 位置和完整文件名**

---

## 版本号规则

### 格式: `w.x.y.z`
- **w (大版本号)**: 重大功能更新时递增
- **x (小版本号)**: 小功能更新时递增
- **y (修复版本号)**: Bug 修复和小优化时递增
- **z (build 号)**: 每次编译/构建/打包时自动 +1

### 版本号存储位置
- 配置文件: `build_version.properties` (根目录)
  ```properties
  major=2
  minor=1
  patch=0
  build=001
  ```
- 构建脚本: `app/build.gradle.kts` 读取并生成 versionCode 和 versionName

### 版本更新规则

#### 每次编译/构建/打包时 (包括 debug 和 release)
- **Build 号自动递增**：`app/build.gradle.kts` 中的 `incrementBuildNumber` 任务会在每次 `assemble*` 时自动将 build 号 +1
- 无需手动更新 build 号

#### y (patch) 或 z (build) 更新时
- 只更新版本号，不更新 changelog

#### x (minor) 更新时
需要更新以下文件:
1. `build_version.properties` - 更新版本号，reset build=001
2. `CHANGELOG.md` (根目录) - 添加新版本更新日志
3. `app/src/main/assets/CHANGELOG.md` - 同步更新 (设置界面读取此文件)
4. `README.md` - 更新版本相关信息

#### w (major) 更新时
除了 x 更新的所有文件外，还需要:
- 更新 `app/src/main/java/com/example/photozen/ui/screens/settings/SettingsScreen.kt` 中的 `AboutDialog` 函数
- 根据最新版本的完整功能重新总结 app 介绍，突出亮点

### 设置界面版本信息
- **版本更新日志**: 读取 `assets/CHANGELOG.md`，只显示当前版本的更新信息 (ChangelogDialog 已实现)
- **关于 PhotoZen**: 点击图标显示 app 介绍 (AboutDialog 函数)

---

## 环境配置

### ADB
- 路径: `D:\androidsdk\platform-tools\adb.exe`
- 无线调试时使用此路径

### 构建命令
```bash
# Debug 包
./gradlew assembleDebug

# Release 包
./gradlew assembleRelease
```

---

## APK 命名规则

打包时使用以下命名格式:
```
PhotoZen-{性质}-v{版本号}.apk
```

示例:
- `PhotoZen-debug-v2.1.0.002.apk`
- `PhotoZen-release-v2.1.0.002.apk`

---

## 关键文件位置

| 用途 | 文件路径 |
|------|----------|
| 版本配置 | `build_version.properties` |
| 构建脚本 | `app/build.gradle.kts` |
| 更新日志 (项目) | `CHANGELOG.md` |
| 更新日志 (app) | `app/src/main/assets/CHANGELOG.md` |
| 更新日志对话框 | `app/src/main/java/com/example/photozen/ui/components/ChangelogDialog.kt` |
| 关于对话框 | `app/src/main/java/com/example/photozen/ui/screens/settings/SettingsScreen.kt` (AboutDialog) |
| README | `README.md` |

---

## 代码规范

- 注释使用中文
- Compose 组件放在 `ui/components/`
- ViewModel 使用 Hilt 注入
- 数据库操作通过 Room DAO

---

## 自动化说明

### Build 号自动递增（已实现）
- `app/build.gradle.kts` 中的 `incrementBuildNumber` 任务
- 触发条件：任何 `assemble*` 任务（assembleDebug、assembleRelease 等）
- 无需手动更新，Gradle 会自动处理
