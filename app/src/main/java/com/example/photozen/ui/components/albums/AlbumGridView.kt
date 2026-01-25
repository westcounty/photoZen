package com.example.photozen.ui.components.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.screens.albums.AlbumBubbleData
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens

/**
 * Album grid view component
 *
 * Modern card grid layout for displaying albums.
 * References Google Photos / Apple Photos design.
 *
 * ## Design Specs
 * | Property | Value | Description |
 * |----------|-------|-------------|
 * | Columns | 2 | Two-column grid |
 * | Grid Spacing | 12dp | Between cards |
 * | Edge Padding | 16dp | Outer margins |
 * | Entry Animation Delay | 30ms/card | Stagger effect |
 * | Bottom Extra Padding | 80dp | Avoid nav bar overlap |
 *
 * ## Entry Animation Sequence
 * ```
 * Timeline:
 * 0ms   ┌───┐
 *       │ 1 │ Start entry
 * 30ms  └───┘ ┌───┐
 *             │ 2 │
 * 60ms        └───┘ ┌───┐
 *                   │ 3 │
 * 90ms              └───┘ ┌───┐
 *                         │ 4 │
 *                         └───┘
 * ```
 *
 * @param albums List of album data
 * @param onAlbumClick Album click callback
 * @param onAlbumLongClick Album long click callback
 * @param modifier Modifier
 */
@Composable
fun AlbumGridView(
    albums: List<AlbumBubbleData>,
    onAlbumClick: (AlbumBubbleData) -> Unit,
    onAlbumLongClick: (AlbumBubbleData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(
            start = PicZenTokens.Spacing.L,
            end = PicZenTokens.Spacing.L,
            top = PicZenTokens.Spacing.L,
            bottom = PicZenTokens.Spacing.L + 80.dp  // Extra space for bottom nav bar
        ),
        horizontalArrangement = Arrangement.spacedBy(PicZenTokens.Spacing.M),
        verticalArrangement = Arrangement.spacedBy(PicZenTokens.Spacing.M)
    ) {
        itemsIndexed(
            items = albums,
            key = { _, album -> album.bucketId }
        ) { index, album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onLongClick = { onAlbumLongClick(album) },
                animationDelay = index * PicZenMotion.Delay.StaggerItem.toInt()
            )
        }
    }
}
