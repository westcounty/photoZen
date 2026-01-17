# PicZen ML 模型说明

## 模型状态概览

| 模型文件 | 用途 | 大小 | 状态 | 缺失影响 |
|---------|------|------|------|---------|
| `mobilenet_v3_small.tflite` | 图像特征提取（相似照片） | ~3.5MB | ✅ 已内置 | - |
| `face_detector.tflite` | 人脸检测 | ~1MB | ✅ 已内置 | - |
| `face_landmarker.task` | 人脸关键点检测 | ~2MB | ✅ 已内置 | - |
| `china_cities.json` | 离线地理编码 | ~50KB | ✅ 已内置 | - |
| `arcface.tflite` | 人脸聚类（512维） | ~4MB | ⚠️ 需下载 | 自动聚类不可用 |
| `mobilefacenet.tflite` | 备用人脸模型（128维） | ~1MB | ⚠️ 可选 | - |
| `labels.json` | 标签映射配置 | ~10KB | ℹ️ 可选 | 无影响（有内置配置） |

## 已内置的模型

以下模型已内置在项目中，无需额外下载：

- **mobilenet_v3_small.tflite** - 图像特征提取，用于相似照片搜索
- **face_detector.tflite** - 人脸检测，配合 ML Kit 使用
- **face_landmarker.task** - 人脸关键点检测
- **china_cities.json** - 中国城市坐标数据，用于离线地理编码

## ML Kit 打包模型

以下功能由 ML Kit 提供，模型在安装时自动打包（通过 AndroidManifest.xml 配置）：

- **人脸检测** - 由 `com.google.mlkit.vision.DEPENDENCIES` 配置自动打包
- **图像标签** - 由 ML Kit Image Labeling 提供（内置 400+ 标签识别）

## 可选模型（人脸聚类功能）

### ArcFace 人脸嵌入模型（推荐）

用于生成人脸特征向量，实现"相同人物"照片自动分组。

**⚠️ 重要提示：** 如果不添加此模型：
- 应用仍可正常运行
- 人脸检测功能正常
- "人物相册"中的**自动聚类功能将不可用**
- 界面会显示"自动聚类不可用"提示

**下载方式：**

#### 方法一：从 GitHub 下载（推荐）

1. 访问 https://github.com/mobilesec/arcface-tensorflowlite
2. 下载 `arcface.tflite` 文件（约 4MB）
3. 放置到 `app/src/main/assets/` 目录

#### 方法二：使用 MobileFaceNet（备选，较小）

1. 访问 https://github.com/sirius-ai/MobileFaceNet_TF
2. 下载并转换为 TFLite 格式
3. 重命名为 `mobilefacenet.tflite`
4. 放置到 `app/src/main/assets/` 目录

**模型规格对比：**

| 模型 | 输出维度 | 文件大小 | 精度 |
|------|---------|---------|------|
| ArcFace | 512 维 | ~4MB | 高 |
| MobileFaceNet | 128 维 | ~1MB | 中 |

应用会优先加载 ArcFace，如果不存在则尝试加载 MobileFaceNet。

## 标签配置文件（可选）

`labels.json` 是可选的标签映射配置文件。代码中已内置 40+ 个常见标签的配置（人物、动物、自然、食物、地点、交通、活动、物品等），**即使没有此文件也不影响功能**。

如需自定义标签映射，可创建 `labels.json`，格式参考内置配置。

## 地图服务

本应用使用 MapLibre + OpenStreetMap 栅格瓦片，**无需 API Key**，国内可直接访问。

默认使用 CARTO 提供的瓦片服务：
- Light: `https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png`
- Dark: `https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png`

备用 OSM 官方瓦片：
- `https://tile.openstreetmap.org/{z}/{x}/{y}.png`

## 离线模式

所有 AI 分析功能均在设备端本地运行，无需联网。地图功能需要网络加载瓦片，但不需要注册或配置任何 API Key。
