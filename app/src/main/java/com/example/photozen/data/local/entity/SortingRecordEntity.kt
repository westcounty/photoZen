package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 整理记录实体
 * 
 * 每日一条记录，累计当天的整理数据。
 * 与 DailyStats 不同，本表记录更详细的分类统计（保留/删除/待定）。
 * 
 * 用于支持：
 * - 整理统计页面（总数、本周、本月统计）
 * - 日历热力图（每日整理量可视化）
 * - 连续整理天数计算
 */
@Entity(
    tableName = "sorting_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class SortingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 日期，格式: YYYY-MM-DD */
    @ColumnInfo(name = "date")
    val date: String,
    
    /** 当日整理总数（sorted_count = kept_count + trashed_count + maybe_count） */
    @ColumnInfo(name = "sorted_count")
    val sortedCount: Int = 0,
    
    /** 当日保留数 */
    @ColumnInfo(name = "kept_count")
    val keptCount: Int = 0,
    
    /** 当日回收站数 */
    @ColumnInfo(name = "trashed_count")
    val trashedCount: Int = 0,
    
    /** 当日待定数 */
    @ColumnInfo(name = "maybe_count")
    val maybeCount: Int = 0,
    
    /** 记录创建时间戳 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
