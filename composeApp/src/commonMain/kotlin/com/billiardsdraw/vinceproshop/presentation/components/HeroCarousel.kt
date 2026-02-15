package com.billiardsdraw.vinceproshop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.localizedSubtitle
import com.billiardsdraw.vinceproshop.domain.model.localizedTitle
import kotlinx.coroutines.delay

@Composable
fun HeroCarousel(
    slides: List<FeaturedSlide>,
    languageCode: String,
    onSlideClick: (FeaturedSlide) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (slides.isEmpty()) {
        EmptyHero(modifier)
        return
    }

    var index by remember(slides.size) { mutableIntStateOf(0) }

    LaunchedEffect(slides.size) {
        if (slides.size < 2) return@LaunchedEffect
        while (true) {
            delay(4500)
            index = (index + 1) % slides.size
        }
    }

    val slide = slides[index]
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { onSlideClick(slide) },
    ) {
        AsyncImage(
            model = slide.imageUrl,
            contentDescription = slide.localizedTitle(languageCode),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f),
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = slide.localizedTitle(languageCode),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = slide.localizedSubtitle(languageCode),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            slides.indices.forEach { item ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (item == index) Color.White else Color.White.copy(alpha = 0.35f))
                        .height(8.dp)
                        .fillMaxWidth(fraction = 0.04f),
                )
            }
        }
    }
}

@Composable
private fun EmptyHero(modifier: Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Featured products are loading",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
