package com.retrobit.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiagaraCurvedAppList(
    apps: List<AppModel>,
    favorites: List<AppModel>,
    searchQuery: String,
    selectedCategory: String,
    listState: LazyListState,
    isCurved: Boolean,
    iconSizeDp: Float = 42f,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(end = 26.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp)
    ) {
        // Tampilkan Favorites jika tidak sedang mencari dan kategori ALL
        if (searchQuery.isEmpty() && selectedCategory == "ALL" && favorites.isNotEmpty()) {
            item(key = "header_favorites") {
                SectionHeader(title = "// [★] FAVORITES")
            }
            itemsIndexed(favorites, key = { _, app -> "fav_${app.packageName}" }) { index, app ->
                AppListItem(
                    app = app,
                    index = index,
                    isCurved = isCurved,
                    iconSizeDp = iconSizeDp,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }

        if (searchQuery.isEmpty()) {
            item(key = "header_all_apps") {
                SectionHeader(title = if (selectedCategory == "ALL") "// [A-Z] SYSTEM DIRECTORY" else "// CATEGORY: #$selectedCategory")
            }
        } else {
            item(key = "header_search") {
                SectionHeader(title = "// SEARCH RESULTS: \"${searchQuery.uppercase()}\"")
            }
        }

        var lastChar = ""
        apps.forEachIndexed { index, app ->
            val firstChar = app.displayLabel.take(1).uppercase()
            if (searchQuery.isEmpty() && selectedCategory == "ALL" && firstChar != lastChar) {
                lastChar = firstChar
                item(key = "letter_$firstChar") {
                    SectionHeader(title = "// [$firstChar] //")
                }
            }

            item(key = "app_${app.packageName}") {
                AppListItem(
                    app = app,
                    index = index,
                    isCurved = isCurved,
                    iconSizeDp = iconSizeDp,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalRetroBitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.inkSecondary,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.inkMuted.copy(alpha = 0.3f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppModel,
    index: Int,
    isCurved: Boolean,
    iconSizeDp: Float = 42f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current

    val curveOffset = remember(index, isCurved) {
        if (!isCurved) 0f
        else {
            val normalized = (index % 10) / 10f * Math.PI
            (sin(normalized) * 12).toFloat()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = curveOffset * density }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(iconSizeDp.dp)
                .border(1.dp, colors.border, RoundedCornerShape(2.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            val monochromeBitmap = remember(app.icon) {
                app.icon?.let { drawableToGrayscaleBitmap(it, 44, 44) }
            }

            if (monochromeBitmap != null) {
                Image(
                    bitmap = monochromeBitmap.asImageBitmap(),
                    contentDescription = app.displayLabel,
                    modifier = Modifier.size((iconSizeDp * 0.72f).dp)
                )
            } else {
                Text(
                    text = app.displayLabel.take(2).uppercase(),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = app.displayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .border(0.5.dp, colors.border)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = app.categoryTag,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkSecondary
                    )
                }
                if (app.isHidden) {
                    Box(
                        modifier = Modifier
                            .background(colors.inkPrimary)
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "HIDDEN",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.background
                        )
                    }
                }
                if (app.isDetoxRestricted) {
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, colors.border)
                            .background(colors.surface)
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "DETOX",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.inkPrimary
                        )
                    }
                }
            }

            // Notification Preview (Niagara Signature Inline Preview)
            if (!app.notificationPreview.isNullOrBlank()) {
                Text(
                    text = "» ${app.notificationPreview}",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = app.subtext,
                    fontSize = 8.5.sp,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Screentime Tag
        Text(
            text = app.screentimeText,
            fontSize = 7.5.sp,
            color = colors.inkSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Text(
            text = "[+]",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkSecondary
        )
    }
}

fun drawableToGrayscaleBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)

    val paint = Paint()
    val matrix = ColorMatrix()
    matrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(matrix)

    canvas.saveLayer(null, paint)
    drawable.draw(canvas)
    canvas.restore()

    return bitmap
}
