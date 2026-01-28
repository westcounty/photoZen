package com.example.photozen.widget.memory

import java.util.Calendar
import java.util.Date

/**
 * Memory Time Formatter - 诗意时间格式化器
 *
 * 将照片拍摄时间转换为诗意的中文描述，增强情感连接。
 *
 * 转换规则：
 * - 今年   -> "今年的夏天"
 * - 去年   -> "去年的冬天"
 * - 2-4年  -> "三年前的夏天"
 * - 5-9年  -> "多年前的夏天"
 * - 10年+  -> "很久以前的夏天"
 *
 * 季节判断：
 * - 春 (3-5月): 春天
 * - 夏 (6-8月): 夏天
 * - 秋 (9-11月): 秋天
 * - 冬 (12-2月): 冬天
 */
object MemoryTimeFormatter {

    private val chineseNumbers = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    /**
     * Format timestamp to poetic time description.
     *
     * @param timestamp Unix timestamp in milliseconds (dateTaken)
     * @param usePoetic Whether to use poetic format (default true)
     * @return Formatted time string
     */
    fun format(timestamp: Long, usePoetic: Boolean = true): String {
        if (timestamp <= 0) {
            return "未知时间"
        }

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        val photoYear = photoDate.get(Calendar.YEAR)
        val photoMonth = photoDate.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val currentYear = now.get(Calendar.YEAR)

        val yearDiff = currentYear - photoYear
        val season = getSeason(photoMonth)

        if (!usePoetic) {
            // Return simple date format
            return String.format(
                "%d.%02d.%02d",
                photoYear,
                photoMonth,
                photoDate.get(Calendar.DAY_OF_MONTH)
            )
        }

        return when {
            yearDiff == 0 -> "今年的$season"
            yearDiff == 1 -> "去年的$season"
            yearDiff in 2..4 -> "${toChineseNumber(yearDiff)}年前的$season"
            yearDiff in 5..9 -> "多年前的$season"
            else -> "很久以前的$season"
        }
    }

    /**
     * Get full date with poetic description.
     * For Large widget that shows both date and poetic text.
     *
     * @param timestamp Unix timestamp in milliseconds
     * @param usePoetic Whether to use poetic format (default true)
     * @return Pair of (date string, poetic string or empty if not poetic)
     */
    fun formatFull(timestamp: Long, usePoetic: Boolean = true): Pair<String, String> {
        if (timestamp <= 0) {
            return Pair("", if (usePoetic) "未知时间" else "")
        }

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateString = String.format(
            "%d.%02d.%02d",
            photoDate.get(Calendar.YEAR),
            photoDate.get(Calendar.MONTH) + 1,
            photoDate.get(Calendar.DAY_OF_MONTH)
        )
        val poeticString = if (usePoetic) format(timestamp, true) else ""

        return Pair(dateString, poeticString)
    }

    /**
     * Get full date with poetic description including city name.
     * For Large widget: date shows with province, poetic time shows with city.
     *
     * @param timestamp Unix timestamp in milliseconds
     * @param usePoetic Whether to use poetic format
     * @param province Province name (e.g., "福建省"), shown after date
     * @param city City name (e.g., "厦门市"), embedded in poetic time
     * @return Pair of (date string with province, poetic string with city)
     */
    fun formatFullWithLocation(
        timestamp: Long,
        usePoetic: Boolean = true,
        province: String? = null,
        city: String? = null
    ): Pair<String, String> {
        if (timestamp <= 0) {
            return Pair("", if (usePoetic) "未知时间" else "")
        }

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        // Date string with province (e.g., "2019.07.28 福建省")
        val baseDateString = String.format(
            "%d.%02d.%02d",
            photoDate.get(Calendar.YEAR),
            photoDate.get(Calendar.MONTH) + 1,
            photoDate.get(Calendar.DAY_OF_MONTH)
        )
        val dateString = if (!province.isNullOrEmpty()) {
            "$baseDateString $province"
        } else {
            baseDateString
        }

        // Poetic string with city (e.g., "去年厦门市的冬天")
        val poeticString = if (usePoetic) {
            formatWithCity(timestamp, city)
        } else {
            ""
        }

        return Pair(dateString, poeticString)
    }

    /**
     * Format timestamp to poetic time with city name embedded.
     * Example: "去年厦门市的夏天", "三年前北京的冬天"
     *
     * @param timestamp Unix timestamp in milliseconds
     * @param city City name to embed (can be null)
     * @return Poetic time string with city
     */
    fun formatWithCity(timestamp: Long, city: String?): String {
        if (timestamp <= 0) {
            return "未知时间"
        }

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        val photoYear = photoDate.get(Calendar.YEAR)
        val photoMonth = photoDate.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        val yearDiff = currentYear - photoYear
        val season = getSeason(photoMonth)

        // City name without "市" suffix for better readability, but keep original if short
        val cityName = city?.let {
            if (it.length > 2 && it.endsWith("市")) it.dropLast(1) else it
        } ?: ""

        return when {
            yearDiff == 0 -> if (cityName.isNotEmpty()) "今年${cityName}的$season" else "今年的$season"
            yearDiff == 1 -> if (cityName.isNotEmpty()) "去年${cityName}的$season" else "去年的$season"
            yearDiff in 2..4 -> if (cityName.isNotEmpty()) "${toChineseNumber(yearDiff)}年前${cityName}的$season" else "${toChineseNumber(yearDiff)}年前的$season"
            yearDiff in 5..9 -> if (cityName.isNotEmpty()) "多年前${cityName}的$season" else "多年前的$season"
            else -> if (cityName.isNotEmpty()) "很久以前${cityName}的$season" else "很久以前的$season"
        }
    }

    /**
     * Check if the photo was taken on "this day in history" (same month and day).
     *
     * @param timestamp Unix timestamp in milliseconds
     * @return True if same month and day as today
     */
    fun isThisDayInHistory(timestamp: Long): Boolean {
        if (timestamp <= 0) return false

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        val sameMonth = photoDate.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        val sameDay = photoDate.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
        val differentYear = photoDate.get(Calendar.YEAR) != now.get(Calendar.YEAR)

        return sameMonth && sameDay && differentYear
    }

    /**
     * Get year difference from current date.
     *
     * @param timestamp Unix timestamp in milliseconds
     * @return Number of years difference
     */
    fun getYearDifference(timestamp: Long): Int {
        if (timestamp <= 0) return 0

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return now.get(Calendar.YEAR) - photoDate.get(Calendar.YEAR)
    }

    /**
     * Get season name based on month.
     */
    private fun getSeason(month: Int): String {
        return when (month) {
            3, 4, 5 -> "春天"
            6, 7, 8 -> "夏天"
            9, 10, 11 -> "秋天"
            else -> "冬天" // 12, 1, 2
        }
    }

    /**
     * Convert number to Chinese character (1-10).
     */
    private fun toChineseNumber(number: Int): String {
        return when {
            number <= 0 -> "零"
            number <= 10 -> chineseNumbers[number]
            else -> number.toString()
        }
    }

    /**
     * Get special day marker if applicable.
     * Returns emoji marker for special dates like "This Day in History".
     *
     * @param timestamp Unix timestamp in milliseconds
     * @return Special marker emoji or empty string
     */
    fun getSpecialDayMarker(timestamp: Long): String {
        return when {
            isThisDayInHistory(timestamp) -> "\uD83D\uDCC5" // Calendar emoji
            isSpringFestival(timestamp) -> "\uD83C\uDFEE" // Lantern emoji
            isMidAutumnFestival(timestamp) -> "\uD83E\uDD6E" // Moon cake emoji
            else -> ""
        }
    }

    /**
     * Check if date is around Spring Festival (roughly late Jan to mid Feb).
     * This is a simplified check - actual Spring Festival varies each year.
     */
    private fun isSpringFestival(timestamp: Long): Boolean {
        if (timestamp <= 0) return false

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val month = photoDate.get(Calendar.MONTH) + 1
        val day = photoDate.get(Calendar.DAY_OF_MONTH)

        // Simplified: Jan 21 - Feb 20
        return (month == 1 && day >= 21) || (month == 2 && day <= 20)
    }

    /**
     * Check if date is around Mid-Autumn Festival (roughly mid Sep to early Oct).
     * This is a simplified check - actual Mid-Autumn varies each year.
     */
    private fun isMidAutumnFestival(timestamp: Long): Boolean {
        if (timestamp <= 0) return false

        val photoDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val month = photoDate.get(Calendar.MONTH) + 1
        val day = photoDate.get(Calendar.DAY_OF_MONTH)

        // Simplified: Sep 15 - Oct 10
        return (month == 9 && day >= 15) || (month == 10 && day <= 10)
    }
}
