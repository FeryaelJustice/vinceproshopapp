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
fun ChatbotBotIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val icon =
        remember {
            ImageVector.Builder(
                name = "ChatbotRobot",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Transparent),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(12f, 4f)
                    verticalLineToRelative(2f)
                    moveToRelative(-5f, 3f)
                    horizontalLineToRelative(10f)
                    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                    verticalLineToRelative(6f)
                    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                    horizontalLineTo(7f)
                    arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                    verticalLineToRelative(-6f)
                    arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                    moveToRelative(2f, 5f)
                    horizontalLineToRelative(0.01f)
                    moveTo(15f, 13f)
                    horizontalLineToRelative(0.01f)
                    moveTo(9f, 17f)
                    horizontalLineToRelative(6f)
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
