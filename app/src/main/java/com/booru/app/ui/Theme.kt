package com.booru.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light")
}

enum class AppPalette(val code: String, val title: String, val primaryColor: Color) {
    MONET("monet", "Monet", Color(0xFF8B5CF6)),
    VIOLET("violet", "Violet Dream", Color(0xFF6750A4)),
    SAKURA("sakura", "Sakura Pink", Color(0xFFD81B60)),
    OCEAN("ocean", "Ocean Blue", Color(0xFF0288D1)),
    EMERALD("emerald", "Emerald Green", Color(0xFF2E7D32)),
    SUNSET("sunset", "Sunset Amber", Color(0xFFE65100)),
    AMOLED("amoled", "Midnight AMOLED", Color(0xFF7C4DFF))
}

private val VioletLight = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), onSecondary = Color.White, secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7E5260), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF), onSurface = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0), surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerLow = Color(0xFFF7F2FA), surfaceContainerLowest = Color.White,
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0)
)
private val VioletDark = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72), primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532), tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930), surfaceContainerHighest = Color(0xFF36343B),
    surfaceContainerLow = Color(0xFF1D1B20), surfaceContainerLowest = Color(0xFF0F0D13),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F)
)

private val SakuraLight = lightColorScheme(
    primary = Color(0xFFBC004B), onPrimary = Color.White, primaryContainer = Color(0xFFFFD9DF), onPrimaryContainer = Color(0xFF3F0013),
    secondary = Color(0xFF75565B), onSecondary = Color.White, secondaryContainer = Color(0xFFFFD9DF), onSecondaryContainer = Color(0xFF2B1519),
    tertiary = Color(0xFF795831), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFDDBA), onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFFF8F8), onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F8), onSurface = Color(0xFF22191B),
    surfaceContainer = Color(0xFFF9EBEC), surfaceContainerHigh = Color(0xFFF3E5E6), surfaceContainerHighest = Color(0xFFEDDFE0),
    surfaceContainerLow = Color(0xFFFFF0F1), surfaceContainerLowest = Color.White,
    outline = Color(0xFF837375), outlineVariant = Color(0xFFD6C2C3)
)
private val SakuraDark = darkColorScheme(
    primary = Color(0xFFFFB1C1), onPrimary = Color(0xFF650024), primaryContainer = Color(0xFF8F0037), onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE5BDC2), onSecondary = Color(0xFF43292D), secondaryContainer = Color(0xFF5C3F43), onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFEBBF90), onTertiary = Color(0xFF452B07), tertiaryContainer = Color(0xFF5E411C), onTertiaryContainer = Color(0xFFFFDDBA),
    background = Color(0xFF191113), onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF191113), onSurface = Color(0xFFF0DEE0),
    surfaceContainer = Color(0xFF261D1F), surfaceContainerHigh = Color(0xFF312829), surfaceContainerHighest = Color(0xFF3C3234),
    surfaceContainerLow = Color(0xFF22191B), surfaceContainerLowest = Color(0xFF140C0E),
    outline = Color(0xFF9E8C8E), outlineVariant = Color(0xFF514345)
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF00668B), onPrimary = Color.White, primaryContainer = Color(0xFFC3E8FF), onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF4E616D), onSecondary = Color.White, secondaryContainer = Color(0xFFD1E5F3), onSecondaryContainer = Color(0xFF0A1E28),
    tertiary = Color(0xFF605A7D), onTertiary = Color.White, tertiaryContainer = Color(0xFFE6DEFF), onTertiaryContainer = Color(0xFF1C1736),
    background = Color(0xFFFBFCFE), onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFE), onSurface = Color(0xFF191C1E),
    surfaceContainer = Color(0xFFEDF1F5), surfaceContainerHigh = Color(0xFFE7EBF0), surfaceContainerHighest = Color(0xFFE1E6EA),
    surfaceContainerLow = Color(0xFFF3F7FB), surfaceContainerLowest = Color.White,
    outline = Color(0xFF71787E), outlineVariant = Color(0xFFC1C7CE)
)
private val OceanDark = darkColorScheme(
    primary = Color(0xFF7BD0FF), onPrimary = Color(0xFF00354A), primaryContainer = Color(0xFF004D6A), onPrimaryContainer = Color(0xFFC3E8FF),
    secondary = Color(0xFFB5C9D7), onSecondary = Color(0xFF20333E), secondaryContainer = Color(0xFF364955), onSecondaryContainer = Color(0xFFD1E5F3),
    tertiary = Color(0xFFCAC1EA), onTertiary = Color(0xFF312C4C), tertiaryContainer = Color(0xFF484264), onTertiaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF111416), onBackground = Color(0xFFE1E6EA),
    surface = Color(0xFF111416), onSurface = Color(0xFFE1E6EA),
    surfaceContainer = Color(0xFF1D2023), surfaceContainerHigh = Color(0xFF272B2E), surfaceContainerHighest = Color(0xFF323639),
    surfaceContainerLow = Color(0xFF191C1E), surfaceContainerLowest = Color(0xFF0C0F11),
    outline = Color(0xFF8B9297), outlineVariant = Color(0xFF41474D)
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF006D44), onPrimary = Color.White, primaryContainer = Color(0xFF91F7BE), onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF4E6354), onSecondary = Color.White, secondaryContainer = Color(0xFFD1E8D5), onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3C6473), onTertiary = Color.White, tertiaryContainer = Color(0xFFC0E9FB), onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF6FBF3), onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF3), onSurface = Color(0xFF181D19),
    surfaceContainer = Color(0xFFEAEFE7), surfaceContainerHigh = Color(0xFFE4EAE1), surfaceContainerHighest = Color(0xFFDFE4DC),
    surfaceContainerLow = Color(0xFFF0F5ED), surfaceContainerLowest = Color.White,
    outline = Color(0xFF717971), outlineVariant = Color(0xFFC0C9BE)
)
private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF74DAA3), onPrimary = Color(0xFF003920), primaryContainer = Color(0xFF005232), onPrimaryContainer = Color(0xFF91F7BE),
    secondary = Color(0xFFB5CCBA), onSecondary = Color(0xFF213527), secondaryContainer = Color(0xFF374C3D), onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA4CDDE), onTertiary = Color(0xFF063543), tertiaryContainer = Color(0xFF234C5A), onTertiaryContainer = Color(0xFFC0E9FB),
    background = Color(0xFF101511), onBackground = Color(0xFFDFE4DC),
    surface = Color(0xFF101511), onSurface = Color(0xFFDFE4DC),
    surfaceContainer = Color(0xFF1C211D), surfaceContainerHigh = Color(0xFF262C27), surfaceContainerHighest = Color(0xFF313732),
    surfaceContainerLow = Color(0xFF181D19), surfaceContainerLowest = Color(0xFF0B100C),
    outline = Color(0xFF8A9388), outlineVariant = Color(0xFF414941)
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFF944A00), onPrimary = Color.White, primaryContainer = Color(0xFFFFDCC4), onPrimaryContainer = Color(0xFF301400),
    secondary = Color(0xFF755845), onSecondary = Color.White, secondaryContainer = Color(0xFFFFDCC4), onSecondaryContainer = Color(0xFF2B1708),
    tertiary = Color(0xFF616032), onTertiary = Color.White, tertiaryContainer = Color(0xFFE8E5AB), onTertiaryContainer = Color(0xFF1D1D00),
    background = Color(0xFFFFF8F5), onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F5), onSurface = Color(0xFF221A14),
    surfaceContainer = Color(0xFFF9ECE3), surfaceContainerHigh = Color(0xFFF3E6DE), surfaceContainerHighest = Color(0xFFEDE0D8),
    surfaceContainerLow = Color(0xFFFFF1E9), surfaceContainerLowest = Color.White,
    outline = Color(0xFF847469), outlineVariant = Color(0xFFD6C3B6)
)
private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB782), onPrimary = Color(0xFF4F2500), primaryContainer = Color(0xFF703700), onPrimaryContainer = Color(0xFFFFDCC4),
    secondary = Color(0xFFE5BEA8), onSecondary = Color(0xFF422B1B), secondaryContainer = Color(0xFF5B4130), onSecondaryContainer = Color(0xFFFFDCC4),
    tertiary = Color(0xFFCBC991), onTertiary = Color(0xFF333208), tertiaryContainer = Color(0xFF49481D), onTertiaryContainer = Color(0xFFE8E5AB),
    background = Color(0xFF1A120C), onBackground = Color(0xFFEDE0D8),
    surface = Color(0xFF1A120C), onSurface = Color(0xFFEDE0D8),
    surfaceContainer = Color(0xFF281E18), surfaceContainerHigh = Color(0xFF332922), surfaceContainerHighest = Color(0xFF3E342C),
    surfaceContainerLow = Color(0xFF221A14), surfaceContainerLowest = Color(0xFF140D08),
    outline = Color(0xFFA08D82), outlineVariant = Color(0xFF52443B)
)

private val AmoledDark = darkColorScheme(
    primary = Color(0xFFBB86FC), onPrimary = Color(0xFF280058), primaryContainer = Color(0xFF4B0099), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC6), onSecondary = Color(0xFF003731), secondaryContainer = Color(0xFF005047), onSecondaryContainer = Color(0xFF70F6E6),
    tertiary = Color(0xFFFF4081), onTertiary = Color(0xFF5C0020), tertiaryContainer = Color(0xFF860032), onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF000000), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF000000), onSurface = Color(0xFFE6E0E9),
    surfaceContainer = Color(0xFF101012), surfaceContainerHigh = Color(0xFF18181C), surfaceContainerHighest = Color(0xFF222228),
    surfaceContainerLow = Color(0xFF08080A), surfaceContainerLowest = Color(0xFF000000),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF3E3C44)
)

val BooruShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val BooruTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun BooruTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppPalette = AppPalette.MONET,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        palette == AppPalette.MONET && useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == AppPalette.AMOLED -> if (isDark) AmoledDark else VioletLight
        palette == AppPalette.SAKURA -> if (isDark) SakuraDark else SakuraLight
        palette == AppPalette.OCEAN -> if (isDark) OceanDark else OceanLight
        palette == AppPalette.EMERALD -> if (isDark) EmeraldDark else EmeraldLight
        palette == AppPalette.SUNSET -> if (isDark) SunsetDark else SunsetLight
        else -> if (isDark) VioletDark else VioletLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = BooruShapes,
        typography = BooruTypography
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onSurface,
            content = content
        )
    }
}
