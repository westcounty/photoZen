# PicZen ML 模型说明

## 已内置的模型

以下模型已内置在项目中，无需额外下载：

| 模型文件 | 用途 | 大小 | 状态 |
|---------|------|------|------|
| `mobilenet_v3_small.tflite` | 图像特征提取（相似照片搜索） | ~3.5MB | ✅ 已内置 |
| `face_detector.tflite` | 人脸检测 | ~1MB | ✅ 已内置 |
| `face_landmarker.task` | 人脸关键点检测 | ~2MB | ✅ 已内置 |

## ML Kit 打包模型

以下功能由 ML Kit 提供，模型在安装时自动打包（通过 AndroidManifest.xml 配置）：

- **人脸检测** - 由 `com.google.mlkit.vision.DEPENDENCIES` 配置自动打包
- **图像标签** - 由 ML Kit Image Labeling 提供

## 可选模型（增强功能）

以下模型为可选，如果需要**人脸聚类/人物识别**功能，请手动下载：

### ArcFace / MobileFaceNet 人脸嵌入模型

用于生成人脸特征向量，实现"相同人物"照片自动分组。

**下载方式：**

1. 访问以下任一源下载 TFLite 模型：
   - https://github.com/mobilesec/arcface-tensorflowlite
   - https://github.com/sirius-ai/MobileFaceNet_TF

2. 将下载的 `.tflite` 文件重命名为 `arcface.tflite` 或 `mobilefacenet.tflite`

3. 放置到 `app/src/main/assets/` 目录

**注意：** 如果不添加此模型，应用仍可正常运行，但"人物相册"功能中的自动分组将不可用（仍可手动标记人物）。

## 地图服务

本应用使用 MapLibre + OpenStreetMap 栅格瓦片，**无需 API Key**，国内可直接访问。

默认使用 CARTO 提供的瓦片服务：
- Light: `https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png`
- Dark: `https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png`

备用 OSM 官方瓦片：
- `https://tile.openstreetmap.org/{z}/{x}/{y}.png`

## 离线模式

所有 AI 分析功能均在设备端本地运行，无需联网。地图功能需要网络加载瓦片，但不需要注册或配置任何 API Key。
