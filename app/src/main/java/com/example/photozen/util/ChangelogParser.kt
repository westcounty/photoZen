package com.example.photozen.util

import android.content.Context

/**
 * Represents a single version entry in the changelog.
 */
data class ChangelogEntry(
    val version: String,
    val date: String,
    val title: String,
    val summary: String,
    val sections: List<ChangelogSection>
)

/**
 * Represents a section within a changelog entry (e.g., "新增功能", "修复").
 */
data class ChangelogSection(
    val title: String,
    val items: List<ChangelogItem>
)

/**
 * Represents a single item in a changelog section.
 */
data class ChangelogItem(
    val title: String,
    val description: String
)

/**
 * Parser for CHANGELOG.md files.
 * Extracts version-specific changelog entries.
 */
object ChangelogParser {
    
    /**
     * Parse the current version's changelog from assets.
     * @param context Application context
     * @param currentVersion Current app version (e.g., "1.4.1.001")
     * @return ChangelogEntry for the current version, or null if not found
     */
    fun parseCurrentVersion(context: Context, currentVersion: String): ChangelogEntry? {
        return try {
            val content = context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
            parseVersionFromContent(content, currentVersion)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse the latest version's changelog from assets.
     * Returns the first (most recent) version entry in the CHANGELOG.md file.
     * @param context Application context
     * @return ChangelogEntry for the latest version, or null if not found
     */
    fun parseLatestVersion(context: Context): ChangelogEntry? {
        return try {
            val content = context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
            parseLatestVersionFromContent(content)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse the latest (first) version's changelog from content.
     */
    fun parseLatestVersionFromContent(content: String): ChangelogEntry? {
        val lines = content.lines()
        var inVersion = false
        var version = ""
        var date = ""
        var title = ""
        var summary = ""
        val sections = mutableListOf<ChangelogSection>()
        var currentSectionTitle = ""
        var currentItems = mutableListOf<ChangelogItem>()
        
        for (line in lines) {
            // Check for version header: ## [1.4.1.001] - 2026-01-18
            if (line.startsWith("## [")) {
                if (inVersion) {
                    // We've reached the next version, stop parsing
                    break
                }
                
                val versionMatch = Regex("""\[(.+?)\]\s*-\s*(.+)""").find(line)
                if (versionMatch != null) {
                    // Take the first version we find (latest)
                    inVersion = true
                    version = versionMatch.groupValues[1]
                    date = versionMatch.groupValues[2].trim()
                }
                continue
            }
            
            if (!inVersion) continue

            // Skip separator lines (but don't break - sections within a version use ---)
            if (line.trim() == "---") {
                continue
            }

            // Parse title: ### 📁 相册分类模式 & 对比模式优化
            if (line.startsWith("### ") && title.isEmpty()) {
                title = line.removePrefix("### ").trim()
                continue
            }
            
            // Parse summary (first non-empty line after title that's not a header)
            if (title.isNotEmpty() && summary.isEmpty() && line.isNotBlank() && !line.startsWith("#")) {
                summary = line.trim()
                continue
            }
            
            // Parse section headers: ### 新增功能, ### 修复
            if (line.startsWith("### ")) {
                // Save previous section if exists
                if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
                }
                currentSectionTitle = line.removePrefix("### ").trim()
                currentItems = mutableListOf()
                continue
            }
            
            // Parse sub-section headers: #### 📁 相册分类模式
            if (line.startsWith("#### ")) {
                val subTitle = line.removePrefix("#### ").trim()
                // Treat sub-sections as section titles
                if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
                }
                currentSectionTitle = subTitle
                currentItems = mutableListOf()
                continue
            }
            
            // Parse list items: - **滑动整理支持相册分类**：筛选时可将照片直接加入相册
            if (line.trimStart().startsWith("- ")) {
                val itemContent = line.trimStart().removePrefix("- ").trim()
                
                // Try to parse bold title with description
                val boldMatch = Regex("""\*\*(.+?)\*\*[：:]\s*(.+)""").find(itemContent)
                if (boldMatch != null) {
                    currentItems.add(ChangelogItem(
                        title = boldMatch.groupValues[1],
                        description = boldMatch.groupValues[2]
                    ))
                } else {
                    // Simple item without bold formatting
                    currentItems.add(ChangelogItem(
                        title = itemContent,
                        description = ""
                    ))
                }
                continue
            }
        }
        
        // Add last section
        if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
            sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
        }
        
        return if (version.isNotEmpty()) {
            ChangelogEntry(version, date, title, summary, sections)
        } else {
            null
        }
    }
    
    /**
     * Parse a specific version's changelog from content.
     */
    fun parseVersionFromContent(content: String, targetVersion: String): ChangelogEntry? {
        val lines = content.lines()
        var inTargetVersion = false
        var version = ""
        var date = ""
        var title = ""
        var summary = ""
        val sections = mutableListOf<ChangelogSection>()
        var currentSectionTitle = ""
        var currentItems = mutableListOf<ChangelogItem>()
        
        for (line in lines) {
            // Check for version header: ## [1.4.1.001] - 2026-01-18
            if (line.startsWith("## [")) {
                if (inTargetVersion) {
                    // We've reached the next version, stop parsing
                    break
                }
                
                val versionMatch = Regex("""\[(.+?)\]\s*-\s*(.+)""").find(line)
                if (versionMatch != null) {
                    val foundVersion = versionMatch.groupValues[1]
                    if (foundVersion == targetVersion) {
                        inTargetVersion = true
                        version = foundVersion
                        date = versionMatch.groupValues[2].trim()
                    }
                }
                continue
            }
            
            if (!inTargetVersion) continue

            // Skip separator lines (but don't break - sections within a version use ---)
            if (line.trim() == "---") {
                continue
            }

            // Parse title: ### 📁 相册分类模式 & 对比模式优化
            if (line.startsWith("### ") && title.isEmpty()) {
                title = line.removePrefix("### ").trim()
                continue
            }
            
            // Parse summary (first non-empty line after title that's not a header)
            if (title.isNotEmpty() && summary.isEmpty() && line.isNotBlank() && !line.startsWith("#")) {
                summary = line.trim()
                continue
            }
            
            // Parse section headers: ### 新增功能, ### 修复
            if (line.startsWith("### ")) {
                // Save previous section if exists
                if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
                }
                currentSectionTitle = line.removePrefix("### ").trim()
                currentItems = mutableListOf()
                continue
            }
            
            // Parse sub-section headers: #### 📁 相册分类模式
            if (line.startsWith("#### ")) {
                val subTitle = line.removePrefix("#### ").trim()
                // Treat sub-sections as section titles
                if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
                }
                currentSectionTitle = subTitle
                currentItems = mutableListOf()
                continue
            }
            
            // Parse list items: - **滑动整理支持相册分类**：筛选时可将照片直接加入相册
            if (line.trimStart().startsWith("- ")) {
                val itemContent = line.trimStart().removePrefix("- ").trim()
                
                // Try to parse bold title with description
                val boldMatch = Regex("""\*\*(.+?)\*\*[：:]\s*(.+)""").find(itemContent)
                if (boldMatch != null) {
                    currentItems.add(ChangelogItem(
                        title = boldMatch.groupValues[1],
                        description = boldMatch.groupValues[2]
                    ))
                } else {
                    // Simple item without bold formatting
                    currentItems.add(ChangelogItem(
                        title = itemContent,
                        description = ""
                    ))
                }
                continue
            }
        }
        
        // Add last section
        if (currentSectionTitle.isNotEmpty() && currentItems.isNotEmpty()) {
            sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
        }
        
        return if (version.isNotEmpty()) {
            ChangelogEntry(version, date, title, summary, sections)
        } else {
            null
        }
    }
}
