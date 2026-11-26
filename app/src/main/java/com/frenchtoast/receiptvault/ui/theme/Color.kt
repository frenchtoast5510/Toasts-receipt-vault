package com.frenchtoast.receiptvault.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val ToastAmber40 = Color(0xFF8C4A1F)
val ToastAmber80 = Color(0xFFFFB68B)
val ToastOnAmberLight = Color(0xFFFFFFFF)
val ToastOnAmberDark = Color(0xFF522300)
val ToastAmberContainerLight = Color(0xFFFFDBC7)
val ToastOnAmberContainerLight = Color(0xFF321200)
val ToastAmberContainerDark = Color(0xFF6F340B)
val ToastOnAmberContainerDark = Color(0xFFFFDBC7)

val ParchmentSage40 = Color(0xFF5A6147)
val ParchmentSage80 = Color(0xFFC2CAAA)
val ParchmentSageContainerLight = Color(0xFFDEE6C5)
val ParchmentSageContainerDark = Color(0xFF434A31)

val CopperAccent40 = Color(0xFF7B5733)
val CopperAccent80 = Color(0xFFEDBE91)
val CopperContainerLight = Color(0xFFFFDCBE)
val CopperContainerDark = Color(0xFF60401E)

val CreamSurfaceLight = Color(0xFFFCF8F3)
val CreamSurfaceContainerLight = Color(0xFFF4EDE4)
val CreamSurfaceContainerHighLight = Color(0xFFEDE4D9)
val EspressoTextLight = Color(0xFF201A16)
val WarmOutlineLight = Color(0xFF84746A)

val EspressoSurfaceDark = Color(0xFF17120E)
val EspressoSurfaceContainerDark = Color(0xFF241E19)
val EspressoSurfaceContainerHighDark = Color(0xFF2F2822)
val CreamTextDark = Color(0xFFEDE0D6)
val WarmOutlineDark = Color(0xFF9E8E83)

val FallbackLightColorScheme = lightColorScheme(
    primary = ToastAmber40,
    onPrimary = ToastOnAmberLight,
    primaryContainer = ToastAmberContainerLight,
    onPrimaryContainer = ToastOnAmberContainerLight,
    secondary = ParchmentSage40,
    onSecondary = Color.White,
    secondaryContainer = ParchmentSageContainerLight,
    onSecondaryContainer = Color(0xFF181E09),
    tertiary = CopperAccent40,
    onTertiary = Color.White,
    tertiaryContainer = CopperContainerLight,
    onTertiaryContainer = Color(0xFF2C1600),
    background = CreamSurfaceLight,
    onBackground = EspressoTextLight,
    surface = CreamSurfaceLight,
    onSurface = EspressoTextLight,
    surfaceContainerLow = Color(0xFFF8F3EC),
    surfaceContainer = CreamSurfaceContainerLight,
    surfaceContainerHigh = CreamSurfaceContainerHighLight,
    outline = WarmOutlineLight,
)

val FallbackDarkColorScheme = darkColorScheme(
    primary = ToastAmber80,
    onPrimary = ToastOnAmberDark,
    primaryContainer = ToastAmberContainerDark,
    onPrimaryContainer = ToastOnAmberContainerDark,
    secondary = ParchmentSage80,
    onSecondary = Color(0xFF2C331C),
    secondaryContainer = ParchmentSageContainerDark,
    onSecondaryContainer = ParchmentSageContainerLight,
    tertiary = CopperAccent80,
    onTertiary = Color(0xFF472A09),
    tertiaryContainer = CopperContainerDark,
    onTertiaryContainer = CopperContainerLight,
    background = EspressoSurfaceDark,
    onBackground = CreamTextDark,
    surface = EspressoSurfaceDark,
    onSurface = CreamTextDark,
    surfaceContainerLow = Color(0xFF1E1914),
    surfaceContainer = EspressoSurfaceContainerDark,
    surfaceContainerHigh = EspressoSurfaceContainerHighDark,
    outline = WarmOutlineDark,
)

@Immutable
data class VaultStatusColors(
    val activeContainer: Color,
    val onActiveContainer: Color,
    val expiringContainer: Color,
    val onExpiringContainer: Color,
    val expiredContainer: Color,
    val onExpiredContainer: Color,
)

val LightVaultStatusColors = VaultStatusColors(
    activeContainer = Color(0xFFD9EBD8),
    onActiveContainer = Color(0xFF183A1C),
    expiringContainer = Color(0xFFFCE5BE),
    onExpiringContainer = Color(0xFF4E3000),
    expiredContainer = Color(0xFFF9DADA),
    onExpiredContainer = Color(0xFF5C1416),
)

val DarkVaultStatusColors = VaultStatusColors(
    activeContainer = Color(0xFF254529),
    onActiveContainer = Color(0xFFC2E8C4),
    expiringContainer = Color(0xFF563A05),
    onExpiringContainer = Color(0xFFFFDFA6),
    expiredContainer = Color(0xFF5E2022),
    onExpiredContainer = Color(0xFFFFD1D1),
)

val LocalVaultStatusColors = staticCompositionLocalOf { LightVaultStatusColors }
