package com.example.photozen.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Label mapping system inspired by PhotoPrism's label taxonomy.
 * 
 * Provides:
 * - Priority-based label display
 * - Confidence threshold filtering
 * - Category grouping
 * - Label translation (English to Chinese)
 * - Synonym/alias handling
 * - Hidden label filtering
 * 
 * Reference: https://docs.photoprism.app/developer-guide/vision/classification/
 */
@Singleton
class LabelMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // Label configurations loaded from assets or hardcoded
    private val labelConfigs: Map<String, LabelConfig> by lazy { loadLabelConfigs() }
    private val categoryConfigs: Map<String, CategoryConfig> by lazy { loadCategoryConfigs() }
    private val rules: List<LabelRule> by lazy { loadRules() }
    
    /**
     * Map a raw label from ML model to display-friendly format.
     * 
     * @param rawLabel The raw label from model (e.g., "tabby cat")
     * @param confidence The confidence score (0.0-1.0)
     * @return MappedLabel with display name and metadata, or null if filtered out
     */
    fun mapLabel(rawLabel: String, confidence: Float): MappedLabel? {
        val normalizedLabel = rawLabel.lowercase().trim()
        
        // Apply rules first
        val processedLabel = applyRules(normalizedLabel)
        if (processedLabel == null) return null // Hidden by rule
        
        // Look up configuration
        val config = labelConfigs[processedLabel] ?: labelConfigs[normalizedLabel]
        
        // If no specific config, use default behavior
        if (config == null) {
            // Default: show if confidence >= 0.5
            return if (confidence >= DEFAULT_THRESHOLD) {
                MappedLabel(
                    rawLabel = rawLabel,
                    displayName = capitalizeLabel(rawLabel),
                    displayNameChinese = translateToChineseDefault(rawLabel),
                    category = null,
                    priority = 0,
                    confidence = confidence
                )
            } else null
        }
        
        // Check if label should be hidden
        if (config.hidden) return null
        
        // Check confidence threshold
        if (confidence < config.threshold) return null
        
        return MappedLabel(
            rawLabel = rawLabel,
            displayName = config.displayName ?: capitalizeLabel(rawLabel),
            displayNameChinese = config.displayNameChinese ?: translateToChineseDefault(rawLabel),
            category = config.category,
            priority = config.priority,
            confidence = confidence
        )
    }
    
    /**
     * Map multiple labels and return sorted by priority.
     */
    fun mapLabels(labels: List<Pair<String, Float>>): List<MappedLabel> {
        return labels
            .mapNotNull { (label, confidence) -> mapLabel(label, confidence) }
            .sortedByDescending { it.priority }
    }
    
    /**
     * Get category info for a category ID.
     */
    fun getCategory(categoryId: String): CategoryConfig? {
        return categoryConfigs[categoryId]
    }
    
    /**
     * Get all categories.
     */
    fun getAllCategories(): List<CategoryConfig> {
        return categoryConfigs.values.sortedByDescending { it.priority }
    }
    
    /**
     * Apply label rules (replacement, hiding, etc.)
     * Returns null if label should be hidden.
     */
    private fun applyRules(label: String): String? {
        for (rule in rules) {
            when {
                rule.exact != null && label == rule.exact -> {
                    if (rule.action == "hide") return null
                    if (rule.replace != null) return rule.replace
                }
                rule.contains != null && label.contains(rule.contains) -> {
                    if (rule.action == "hide") return null
                    if (rule.replace != null) return rule.replace
                }
                rule.startsWith != null && label.startsWith(rule.startsWith) -> {
                    if (rule.action == "hide") return null
                    if (rule.replace != null) return rule.replace
                }
            }
        }
        return label
    }
    
    /**
     * Load label configurations.
     * In production, this could load from assets/labels.json
     */
    private fun loadLabelConfigs(): Map<String, LabelConfig> {
        // Try to load from assets
        try {
            val jsonString = context.assets.open("labels.json").bufferedReader().readText()
            val data = json.decodeFromString<LabelConfigData>(jsonString)
            return data.labels.associateBy { it.id }
        } catch (e: Exception) {
            // Fall back to built-in labels
        }
        
        // Built-in label configurations (based on ImageNet + PhotoPrism patterns)
        return mapOf(
            // People
            "person" to LabelConfig("person", "Person", "人物", 100, "people", 0.6f),
            "man" to LabelConfig("man", "Man", "男性", 95, "people", 0.6f),
            "woman" to LabelConfig("woman", "Woman", "女性", 95, "people", 0.6f),
            "child" to LabelConfig("child", "Child", "儿童", 95, "people", 0.6f),
            "baby" to LabelConfig("baby", "Baby", "婴儿", 95, "people", 0.6f),
            
            // Animals
            "cat" to LabelConfig("cat", "Cat", "猫", 80, "animal", 0.5f),
            "tabby" to LabelConfig("tabby", "Cat", "猫", 80, "animal", 0.5f),
            "tabby cat" to LabelConfig("tabby cat", "Cat", "猫", 80, "animal", 0.5f),
            "dog" to LabelConfig("dog", "Dog", "狗", 80, "animal", 0.5f),
            "golden retriever" to LabelConfig("golden retriever", "Golden Retriever", "金毛犬", 78, "animal", 0.5f),
            "labrador retriever" to LabelConfig("labrador retriever", "Labrador", "拉布拉多", 78, "animal", 0.5f),
            "bird" to LabelConfig("bird", "Bird", "鸟", 75, "animal", 0.5f),
            "fish" to LabelConfig("fish", "Fish", "鱼", 70, "animal", 0.5f),
            
            // Nature
            "flower" to LabelConfig("flower", "Flower", "花", 70, "nature", 0.5f),
            "tree" to LabelConfig("tree", "Tree", "树", 65, "nature", 0.4f),
            "mountain" to LabelConfig("mountain", "Mountain", "山", 70, "nature", 0.5f),
            "beach" to LabelConfig("beach", "Beach", "海滩", 70, "nature", 0.5f),
            "ocean" to LabelConfig("ocean", "Ocean", "海洋", 70, "nature", 0.5f),
            "sky" to LabelConfig("sky", "Sky", "天空", 50, "nature", 0.4f),
            "sunset" to LabelConfig("sunset", "Sunset", "日落", 75, "nature", 0.5f),
            "sunrise" to LabelConfig("sunrise", "Sunrise", "日出", 75, "nature", 0.5f),
            
            // Food
            "food" to LabelConfig("food", "Food", "食物", 65, "food", 0.5f),
            "pizza" to LabelConfig("pizza", "Pizza", "披萨", 70, "food", 0.5f),
            "burger" to LabelConfig("burger", "Burger", "汉堡", 70, "food", 0.5f),
            "cake" to LabelConfig("cake", "Cake", "蛋糕", 70, "food", 0.5f),
            "coffee" to LabelConfig("coffee", "Coffee", "咖啡", 65, "food", 0.5f),
            
            // Places
            "building" to LabelConfig("building", "Building", "建筑", 60, "place", 0.4f),
            "house" to LabelConfig("house", "House", "房屋", 60, "place", 0.4f),
            "church" to LabelConfig("church", "Church", "教堂", 65, "place", 0.5f),
            "bridge" to LabelConfig("bridge", "Bridge", "桥", 65, "place", 0.5f),
            "street" to LabelConfig("street", "Street", "街道", 55, "place", 0.4f),
            "city" to LabelConfig("city", "City", "城市", 60, "place", 0.4f),
            
            // Transportation
            "car" to LabelConfig("car", "Car", "汽车", 65, "transport", 0.5f),
            "airplane" to LabelConfig("airplane", "Airplane", "飞机", 70, "transport", 0.5f),
            "boat" to LabelConfig("boat", "Boat", "船", 65, "transport", 0.5f),
            "bicycle" to LabelConfig("bicycle", "Bicycle", "自行车", 65, "transport", 0.5f),
            "train" to LabelConfig("train", "Train", "火车", 65, "transport", 0.5f),
            
            // Events
            "wedding" to LabelConfig("wedding", "Wedding", "婚礼", 85, "event", 0.6f),
            "birthday" to LabelConfig("birthday", "Birthday", "生日", 80, "event", 0.5f),
            "party" to LabelConfig("party", "Party", "派对", 75, "event", 0.5f),
            "concert" to LabelConfig("concert", "Concert", "音乐会", 75, "event", 0.5f),
            
            // Objects
            "book" to LabelConfig("book", "Book", "书", 55, "object", 0.4f),
            "phone" to LabelConfig("phone", "Phone", "手机", 60, "object", 0.5f),
            "laptop" to LabelConfig("laptop", "Laptop", "笔记本电脑", 60, "object", 0.5f),
            "camera" to LabelConfig("camera", "Camera", "相机", 65, "object", 0.5f),
            
            // Hidden/Low priority labels
            "no person" to LabelConfig("no person", hidden = true),
            "indoor" to LabelConfig("indoor", "Indoor", "室内", 20, "scene", 0.3f),
            "outdoor" to LabelConfig("outdoor", "Outdoor", "户外", 20, "scene", 0.3f)
        )
    }
    
    /**
     * Load category configurations.
     */
    private fun loadCategoryConfigs(): Map<String, CategoryConfig> {
        return mapOf(
            "people" to CategoryConfig("people", "People", "人物", "person", 100),
            "animal" to CategoryConfig("animal", "Animals", "动物", "pets", 90),
            "nature" to CategoryConfig("nature", "Nature", "自然", "landscape", 80),
            "food" to CategoryConfig("food", "Food & Drink", "美食", "restaurant", 70),
            "place" to CategoryConfig("place", "Places", "地点", "place", 60),
            "transport" to CategoryConfig("transport", "Transport", "交通", "directions_car", 50),
            "event" to CategoryConfig("event", "Events", "活动", "event", 85),
            "object" to CategoryConfig("object", "Objects", "物品", "category", 40),
            "scene" to CategoryConfig("scene", "Scenes", "场景", "photo_camera", 30)
        )
    }
    
    /**
     * Load label rules.
     */
    private fun loadRules(): List<LabelRule> {
        return listOf(
            // Hide "no person" type labels
            LabelRule(contains = "no person", action = "hide"),
            LabelRule(contains = "no people", action = "hide"),
            
            // Merge similar labels
            LabelRule(exact = "tabby", replace = "cat"),
            LabelRule(exact = "tabby cat", replace = "cat"),
            LabelRule(exact = "persian cat", replace = "cat"),
            LabelRule(exact = "siamese cat", replace = "cat"),
            
            // Merge dog breeds to generic when low confidence
            LabelRule(contains = "retriever", replace = "dog"),
            LabelRule(contains = "terrier", replace = "dog"),
            LabelRule(contains = "poodle", replace = "dog")
        )
    }
    
    /**
     * Capitalize label for display.
     */
    private fun capitalizeLabel(label: String): String {
        return label.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    }
    
    /**
     * Basic translation to Chinese for unknown labels.
     */
    private fun translateToChineseDefault(label: String): String {
        // For unknown labels, just return the English with capitalization
        return capitalizeLabel(label)
    }
    
    companion object {
        private const val DEFAULT_THRESHOLD = 0.5f
    }
}

/**
 * Mapped label result.
 */
data class MappedLabel(
    val rawLabel: String,
    val displayName: String,
    val displayNameChinese: String,
    val category: String?,
    val priority: Int,
    val confidence: Float
)

/**
 * Label configuration.
 */
@Serializable
data class LabelConfig(
    val id: String,
    val displayName: String? = null,
    val displayNameChinese: String? = null,
    val priority: Int = 0,
    val category: String? = null,
    val threshold: Float = 0.5f,
    val hidden: Boolean = false,
    val synonyms: List<String> = emptyList()
)

/**
 * Category configuration.
 */
@Serializable
data class CategoryConfig(
    val id: String,
    val name: String,
    val nameChinese: String,
    val icon: String,
    val priority: Int = 0
)

/**
 * Label rule for transformation/filtering.
 */
@Serializable
data class LabelRule(
    val exact: String? = null,
    val contains: String? = null,
    val startsWith: String? = null,
    val action: String? = null, // "hide", "merge"
    val replace: String? = null
)

/**
 * Data class for JSON deserialization.
 */
@Serializable
data class LabelConfigData(
    val labels: List<LabelConfig> = emptyList(),
    val categories: List<CategoryConfig> = emptyList(),
    val rules: List<LabelRule> = emptyList()
)
