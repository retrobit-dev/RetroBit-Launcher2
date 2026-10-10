package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun CategoryChipsRow(
    selectedCategory: String,
    hiddenCount: Int,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val categories = mutableListOf("ALL", "FAV", "COMM", "NET", "DATA", "SYS", "MEDIA", "TOOL")
    if (hiddenCount > 0) {
        categories.add("HIDDEN")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val label = if (cat == "HIDDEN") "#HIDDEN ($hiddenCount)" else "#$cat"

            Box(
                modifier = Modifier
                    .border(1.dp, colors.border)
                    .background(if (isSelected) colors.inkPrimary else colors.surface)
                    .clickable { onSelectCategory(cat) }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.background else colors.inkSecondary
                )
            }
        }
    }
}
