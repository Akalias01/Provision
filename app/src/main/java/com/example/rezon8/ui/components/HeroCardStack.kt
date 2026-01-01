package com.mossglen.lithos.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * HeroCardStack - Premium Depth Carousel for the Now page.
 *
 * Uses HorizontalPager for proper sizing + forced snap-back for stuck cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> HeroCardStack(
    items: List<T>,
    onPageChanged: (index: Int) -> Unit = {},
    onSwipeProgress: (progress: Float) -> Unit = {},
    modifier: Modifier = Modifier,
    cardContent: @Composable BoxScope.(item: T, centeredness: Float) -> Unit
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val pagerState = rememberPagerState(initialPage = 0) { items.size }

    // Continuously monitor for stuck state and force snap
    LaunchedEffect(pagerState) {
        snapshotFlow {
            Triple(
                pagerState.isScrollInProgress,
                pagerState.currentPageOffsetFraction,
                pagerState.currentPage
            )
        }.collect { (isScrolling, offset, page) ->
            // If not scrolling but offset from page center, we're stuck
            if (!isScrolling && abs(offset) > 0.02f) {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage,
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = 200f
                    )
                )
            }
        }
    }

    // Auto-return to index 0 after 5 seconds of inactivity
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0) {
            delay(5000)
            if (pagerState.currentPage != 0 && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    // Report page changes
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    // Report swipe progress
    LaunchedEffect(pagerState.currentPageOffsetFraction) {
        onSwipeProgress(abs(pagerState.currentPageOffsetFraction))
    }

    // Depth Carousel constants
    val flankingScale = 0.9f
    val flankingAlpha = 0.6f
    val tuckOffset = with(density) { 24.dp.toPx() }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = (-16).dp,
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = 0.35f,
            pagerSnapDistance = PagerSnapDistance.atMost(1)
        )
    ) { page ->
        val item = items.getOrNull(page) ?: return@HorizontalPager

        // Calculate offset from center
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absoluteOffset = abs(pageOffset).coerceIn(0f, 1f)

        // Visual properties
        val scale = lerp(1f, flankingScale, absoluteOffset)
        val alpha = lerp(1f, flankingAlpha, absoluteOffset)

        // Tuck for depth
        val translationX = if (pageOffset < 0) {
            -tuckOffset * absoluteOffset
        } else {
            tuckOffset * absoluteOffset
        }

        // Z-index: center on top
        val zIndex = 1f - absoluteOffset

        // Centeredness for shadow interpolation
        val centeredness = (1f - absoluteOffset).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .zIndex(zIndex)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.translationX = translationX
                },
            contentAlignment = Alignment.Center
        ) {
            cardContent(item, centeredness)
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
