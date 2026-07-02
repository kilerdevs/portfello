package com.portfello.ui.theme

import androidx.compose.ui.graphics.Color
import com.portfello.data.db.entity.AssetType

// Surfaces — deep navy/charcoal
val Ink = Color(0xFF0B0F1A)
val Surface1 = Color(0xFF111827)
val Surface2 = Color(0xFF1B2436)
val SurfaceHigh = Color(0xFF232E47)

// Accents
val ElectricTeal = Color(0xFF2DD4BF)
val TealDeep = Color(0xFF0F3D38)
val Violet = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF2E2159)
val VioletPale = Color(0xFFD8CCFF)

// Text
val TextPrimary = Color(0xFFE7ECF5)
val TextSecondary = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF3B4863)
val OutlineFaint = Color(0xFF273349)

// Semantic
val GainGreen = Color(0xFF34D399)
val LossRed = Color(0xFFF87171)

// Asset-type colors (allocation donut, legend) — tuned to the palette
val typeColors = mapOf(
    AssetType.STOCK to ElectricTeal,
    AssetType.CRYPTO to Violet,
    AssetType.METAL_BULLION to Color(0xFFFBBF24),
    AssetType.CURRENCY to Color(0xFF60A5FA),
    AssetType.BOND_RETAIL to Color(0xFF38BDF8),
    AssetType.BOND_TRADED to Color(0xFFF472B6),
    AssetType.MANUAL to Color(0xFF9CA3AF),
)
