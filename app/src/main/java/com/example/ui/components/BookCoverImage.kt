package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

@Composable
fun BookCoverImage(
    coverUrl: String?,
    title: String,
    author: String,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8
) {
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Editorial Fallback Cover
            BookFallbackCover(
                title = title,
                author = author,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BookFallbackCover(
    title: String,
    author: String,
    modifier: Modifier = Modifier
) {
    // Generate deterministic pleasing book cover colors based on title hash
    val hash = abs(title.hashCode())
    val palettes = listOf(
        Pair(Color(0xFF1E293B), Color(0xFF0F172A)), // Deep Slate
        Pair(Color(0xFF831843), Color(0xFF500724)), // Velvet Wine
        Pair(Color(0xFF14532D), Color(0xFF052E16)), // Forest Emerald
        Pair(Color(0xFF1E3A8A), Color(0xFF172554)), // Classic Navy
        Pair(Color(0xFF78350F), Color(0xFF451A03)), // Antique Amber
        Pair(Color(0xFF4C1D95), Color(0xFF2E1065))  // Royal Violet
    )
    val (colorTop, colorBottom) = palettes[hash % palettes.size]

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colorTop, colorBottom)
                )
            )
            .padding(10.dp)
    ) {
        // Book Spine Shadow on the left
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxSize()
                .align(Alignment.CenterStart)
                .background(Color(0x33000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gold foil ornamental line
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(Color(0xFFFDE68A))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 14.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "FREE EDITION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFFDE68A)
            )
        }
    }
}
