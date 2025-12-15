package com.rezon.app.presentation.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rezon.app.presentation.ui.theme.PlayerGradientEnd
import com.rezon.app.presentation.ui.theme.PlayerGradientStart
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple

/**
 * Theme preset data
 */
data class ThemePreset(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val isPremium: Boolean = false
)

/**
 * Available theme presets
 */
val themePresets = listOf(
    ThemePreset(
        name = "Cyberpunk",
        primaryColor = Color(0xFF7F00FF),
        secondaryColor = Color(0xFF00E5FF),
        backgroundColor = Color(0xFF0D0D15),
        surfaceColor = Color(0xFF1E1E26)
    ),
    ThemePreset(
        name = "Midnight Blue",
        primaryColor = Color(0xFF3B82F6),
        secondaryColor = Color(0xFF60A5FA),
        backgroundColor = Color(0xFF0F172A),
        surfaceColor = Color(0xFF1E293B)
    ),
    ThemePreset(
        name = "Aurora",
        primaryColor = Color(0xFF22C55E),
        secondaryColor = Color(0xFF10B981),
        backgroundColor = Color(0xFF0A0F0D),
        surfaceColor = Color(0xFF1A1F1D)
    ),
    ThemePreset(
        name = "Sunset",
        primaryColor = Color(0xFFF97316),
        secondaryColor = Color(0xFFFBBF24),
        backgroundColor = Color(0xFF150D0A),
        surfaceColor = Color(0xFF251D1A)
    ),
    ThemePreset(
        name = "Rose Gold",
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFFF472B6),
        backgroundColor = Color(0xFF150A12),
        surfaceColor = Color(0xFF251A22)
    ),
    ThemePreset(
        name = "Ocean",
        primaryColor = Color(0xFF0EA5E9),
        secondaryColor = Color(0xFF06B6D4),
        backgroundColor = Color(0xFF0A1015),
        surfaceColor = Color(0xFF1A2025)
    ),
    ThemePreset(
        name = "Crimson",
        primaryColor = Color(0xFFEF4444),
        secondaryColor = Color(0xFFF87171),
        backgroundColor = Color(0xFF150A0A),
        surfaceColor = Color(0xFF251A1A),
        isPremium = true
    ),
    ThemePreset(
        name = "AMOLED Black",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFA0A0A0),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF121212),
        isPremium = true
    )
)

/**
 * Theme configuration screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(themePresets[0]) }
    var useOLED by remember { mutableStateOf(false) }
    var useGradientBackgrounds by remember { mutableStateOf(true) }
    var animatedUI by remember { mutableStateOf(true) }
    var reducedMotion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = RezonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PlayerGradientStart, PlayerGradientEnd)
                    )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Color Themes
                item {
                    Text(
                        text = "COLOR THEMES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(themePresets) { theme ->
                            ThemePresetCard(
                                theme = theme,
                                isSelected = selectedTheme == theme,
                                onClick = { selectedTheme = theme }
                            )
                        }
                    }
                }

                // Current theme preview
                item {
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ThemePreviewCard(theme = selectedTheme)
                }

                // Appearance settings
                item {
                    Text(
                        text = "APPEARANCE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column {
                            AppearanceToggle(
                                title = "OLED Black Mode",
                                subtitle = "Pure black backgrounds for OLED screens",
                                checked = useOLED,
                                onCheckedChange = { useOLED = it }
                            )
                            AppearanceToggle(
                                title = "Gradient Backgrounds",
                                subtitle = "Beautiful gradient effects on screens",
                                checked = useGradientBackgrounds,
                                onCheckedChange = { useGradientBackgrounds = it }
                            )
                            AppearanceToggle(
                                title = "Animated UI",
                                subtitle = "Smooth transitions and animations",
                                checked = animatedUI,
                                onCheckedChange = { animatedUI = it }
                            )
                            AppearanceToggle(
                                title = "Reduced Motion",
                                subtitle = "Minimize animations for accessibility",
                                checked = reducedMotion,
                                onCheckedChange = { reducedMotion = it }
                            )
                        }
                    }
                }

                // Accent colors
                item {
                    Text(
                        text = "ACCENT COLOR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    AccentColorPicker(
                        selectedColor = selectedTheme.primaryColor,
                        onColorSelected = { /* Apply accent color */ }
                    )
                }
            }
        }
    }
}

/**
 * Theme preset card
 */
@Composable
private fun ThemePresetCard(
    theme: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) theme.primaryColor else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.secondaryColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = theme.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            if (theme.isPremium) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = theme.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Theme preview card
 */
@Composable
private fun ThemePreviewCard(theme: ThemePreset) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fake app bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.primaryColor)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(theme.surfaceColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(theme.surfaceColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fake content cards
            repeat(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surfaceColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fake buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.secondaryColor)
                )
            }
        }
    }
}

/**
 * Appearance toggle
 */
@Composable
private fun AppearanceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RezonPurple
            )
        )
    }
}

/**
 * Accent color picker
 */
@Composable
private fun AccentColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val accentColors = listOf(
        Color(0xFF7F00FF), // Purple
        Color(0xFF3B82F6), // Blue
        Color(0xFF00E5FF), // Cyan
        Color(0xFF22C55E), // Green
        Color(0xFFFBBF24), // Yellow
        Color(0xFFF97316), // Orange
        Color(0xFFEF4444), // Red
        Color(0xFFEC4899), // Pink
        Color(0xFFA855F7), // Violet
        Color(0xFFFFFFFF)  // White
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accentColors) { color ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 3.dp else 0.dp,
                            color = if (selectedColor == color) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == color) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (color == Color.White) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
