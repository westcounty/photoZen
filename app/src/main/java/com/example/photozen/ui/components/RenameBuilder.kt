package com.example.photozen.ui.components

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.util.LocationDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ============== 重命名元素数据模型 ==============

/**
 * 重命名元素 - 组成文件名模板的基本单元
 */
sealed class RenameElement {
    val uid: String = UUID.randomUUID().toString()

    /**
     * 自定义文本
     */
    data class CustomText(val text: String) : RenameElement()

    /**
     * 地理位置
     */
    data class GeoLocation(
        val mode: GeoMode = GeoMode.PROVINCE_CITY,
        val separator: GeoSeparator = GeoSeparator.UNDERSCORE
    ) : RenameElement()

    /**
     * 自增序号
     */
    data class AutoIncrement(
        val startValue: Int = 1,
        val stepValue: Int = 1
    ) : RenameElement()

    /**
     * 照片日期
     */
    data class PhotoDate(
        val fields: Set<DateField> = setOf(DateField.YEAR, DateField.MONTH, DateField.DAY),
        val format: DateFormat = DateFormat.CHINESE
    ) : RenameElement()
}

enum class GeoMode(val label: String) {
    PROVINCE("省份"),
    PROVINCE_CITY("省份+城市"),
    CITY_ONLY("仅城市")
}

enum class GeoSeparator(val label: String, val value: String) {
    UNDERSCORE("下划线 _", "_"),
    HYPHEN("连字符 -", "-"),
    DOT("点号 .", "."),
    SPACE("空格", " "),
    NONE("无（直接连接）", "")
}

enum class DateField(val label: String) {
    YEAR("年"), MONTH("月"), DAY("日"),
    HOUR("时"), MINUTE("分"), SECOND("秒")
}

enum class DateFormat(val label: String) {
    CHINESE("2024年01月15日"),
    HYPHEN("2024-01-15"),
    COMPACT("20240115"),
    DOT("2024.01.15")
}

enum class Separator(val label: String, val value: String) {
    UNDERSCORE("下划线 _", "_"),
    HYPHEN("连字符 -", "-"),
    DOT("点号 .", "."),
    SPACE("空格", " "),
    NONE("无", "")
}

/**
 * 重命名模板 - 由多个元素和连接符组成
 */
data class RenameTemplate(
    val elements: List<RenameElement> = emptyList(),
    val separator: Separator = Separator.UNDERSCORE
)

// ============== 文件名生成逻辑 ==============

/**
 * 为单张照片生成新文件名（不含扩展名）
 *
 * @param photo 照片实体
 * @param template 重命名模板
 * @param index 照片在列表中的序号（从0开始）
 * @param totalCount 照片总数（用于计算固定位数）
 * @param locationDetails 预先解析的地理位置（可为null）
 */
fun generateNewName(
    photo: PhotoEntity,
    template: RenameTemplate,
    index: Int,
    totalCount: Int,
    locationDetails: LocationDetails?
): String {
    val parts = template.elements.mapNotNull { element ->
        when (element) {
            is RenameElement.CustomText -> element.text.ifBlank { null }

            is RenameElement.GeoLocation -> {
                if (locationDetails == null) null
                else when (element.mode) {
                    GeoMode.PROVINCE -> locationDetails.province
                    GeoMode.PROVINCE_CITY -> "${locationDetails.province}${element.separator.value}${locationDetails.city}"
                    GeoMode.CITY_ONLY -> locationDetails.city
                }
            }

            is RenameElement.AutoIncrement -> {
                val number = element.startValue + index * element.stepValue
                number.toString()
            }

            is RenameElement.PhotoDate -> {
                val timestamp = if (photo.dateTaken > 0) photo.dateTaken
                    else if (photo.dateAdded > 0) photo.dateAdded * 1000 // dateAdded is in seconds
                    else null
                if (timestamp == null) null
                else formatDate(timestamp, element.fields, element.format)
            }
        }
    }
    return parts.joinToString(template.separator.value)
}

/**
 * 根据选中字段和格式化选项格式化日期
 */
private fun formatDate(timestampMs: Long, fields: Set<DateField>, format: DateFormat): String {
    val date = Date(timestampMs)
    val parts = mutableListOf<String>()

    val hasYear = DateField.YEAR in fields
    val hasMonth = DateField.MONTH in fields
    val hasDay = DateField.DAY in fields
    val hasHour = DateField.HOUR in fields
    val hasMinute = DateField.MINUTE in fields
    val hasSecond = DateField.SECOND in fields

    when (format) {
        DateFormat.CHINESE -> {
            if (hasYear) parts.add(SimpleDateFormat("yyyy年", Locale.CHINA).format(date))
            if (hasMonth) parts.add(SimpleDateFormat("MM月", Locale.CHINA).format(date))
            if (hasDay) parts.add(SimpleDateFormat("dd日", Locale.CHINA).format(date))
            if (hasHour) parts.add(SimpleDateFormat("HH时", Locale.CHINA).format(date))
            if (hasMinute) parts.add(SimpleDateFormat("mm分", Locale.CHINA).format(date))
            if (hasSecond) parts.add(SimpleDateFormat("ss秒", Locale.CHINA).format(date))
            return parts.joinToString("")
        }
        DateFormat.HYPHEN -> {
            val dateParts = mutableListOf<String>()
            if (hasYear) dateParts.add(SimpleDateFormat("yyyy", Locale.CHINA).format(date))
            if (hasMonth) dateParts.add(SimpleDateFormat("MM", Locale.CHINA).format(date))
            if (hasDay) dateParts.add(SimpleDateFormat("dd", Locale.CHINA).format(date))
            val timeParts = mutableListOf<String>()
            if (hasHour) timeParts.add(SimpleDateFormat("HH", Locale.CHINA).format(date))
            if (hasMinute) timeParts.add(SimpleDateFormat("mm", Locale.CHINA).format(date))
            if (hasSecond) timeParts.add(SimpleDateFormat("ss", Locale.CHINA).format(date))
            val result = mutableListOf<String>()
            if (dateParts.isNotEmpty()) result.add(dateParts.joinToString("-"))
            if (timeParts.isNotEmpty()) result.add(timeParts.joinToString(":"))
            return result.joinToString(" ")
        }
        DateFormat.COMPACT -> {
            if (hasYear) parts.add(SimpleDateFormat("yyyy", Locale.CHINA).format(date))
            if (hasMonth) parts.add(SimpleDateFormat("MM", Locale.CHINA).format(date))
            if (hasDay) parts.add(SimpleDateFormat("dd", Locale.CHINA).format(date))
            if (hasHour) parts.add(SimpleDateFormat("HH", Locale.CHINA).format(date))
            if (hasMinute) parts.add(SimpleDateFormat("mm", Locale.CHINA).format(date))
            if (hasSecond) parts.add(SimpleDateFormat("ss", Locale.CHINA).format(date))
            return parts.joinToString("")
        }
        DateFormat.DOT -> {
            val dateParts = mutableListOf<String>()
            if (hasYear) dateParts.add(SimpleDateFormat("yyyy", Locale.CHINA).format(date))
            if (hasMonth) dateParts.add(SimpleDateFormat("MM", Locale.CHINA).format(date))
            if (hasDay) dateParts.add(SimpleDateFormat("dd", Locale.CHINA).format(date))
            val timeParts = mutableListOf<String>()
            if (hasHour) timeParts.add(SimpleDateFormat("HH", Locale.CHINA).format(date))
            if (hasMinute) timeParts.add(SimpleDateFormat("mm", Locale.CHINA).format(date))
            if (hasSecond) timeParts.add(SimpleDateFormat("ss", Locale.CHINA).format(date))
            val result = mutableListOf<String>()
            if (dateParts.isNotEmpty()) result.add(dateParts.joinToString("."))
            if (timeParts.isNotEmpty()) result.add(timeParts.joinToString(":"))
            return result.joinToString(" ")
        }
    }
}

/**
 * 清理文件名中的非法字符
 */
fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().trimEnd('.')
}

/**
 * 获取照片扩展名（含点号）
 */
fun getPhotoExtension(displayName: String): String {
    val dotIndex = displayName.lastIndexOf('.')
    return if (dotIndex >= 0) displayName.substring(dotIndex) else ".jpg"
}
