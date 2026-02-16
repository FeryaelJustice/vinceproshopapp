package com.billiardsdraw.vinceproshop.presentation.chatbot

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Composable
fun ChatbotCloseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val icon =
        remember {
            ImageVector.Builder(
                name = "ChatbotClose",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Transparent),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(6f, 6f)
                    lineTo(18f, 18f)
                    moveTo(18f, 6f)
                    lineTo(6f, 18f)
                }
            }.build()
        }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
