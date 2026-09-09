package com.example.presentation.pos.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.ui.theme.*

@Composable
fun ProductCard(
    product: ProductItem,
    cartQuantity: Int = 0,
    thresholdOverride: Int? = null,
    onAddToCart: (ProductItem) -> Unit,
    onRemoveFromCart: (ProductItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveThreshold = thresholdOverride ?: product.lowStockThreshold
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = !isOutOfStock && product.stockQuantity <= effectiveThreshold
    val isSelected = cartQuantity > 0

    val cardBorderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isLowStock -> Amber500
        isOutOfStock -> Red200
        else -> Slate200
    }
    val cardBorderWidth = if (isSelected || isLowStock) 2.dp else 1.dp

    Card(
        modifier = modifier
            .testTag("product_card_${product.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = cardBorderWidth,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isOutOfStock) {
                onAddToCart(product)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutOfStock -> Slate100.copy(alpha = 0.7f)
                isSelected -> MaterialTheme.colorScheme.surface
                isLowStock -> Amber50.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else if (isLowStock) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Product Image Container with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                // Image Loading: Local Resource > Remote URL > Category Icon Fallback
                when {
                    product.imageResId != null -> {
                        Image(
                            painter = painterResource(id = product.imageResId),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    product.imageUrl != null -> {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        ProductCategoryFallback(category = product.category)
                    }
                }

                // Subtle gradient vignette at the bottom of the image for text clarity
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)),
                                startY = 40f
                            )
                        )
                )

                // Top Badges (Category pill on left, Stock / In Cart badge on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Category Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(6.dp),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = product.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Stock Status Badges
                    when {
                        isOutOfStock -> {
                            Surface(
                                color = Red600,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("out_of_stock_badge_${product.id}")
                            ) {
                                Text(
                                    text = "Out of Stock",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        isLowStock -> {
                            Surface(
                                color = Amber500,
                                shape = RoundedCornerShape(6.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.testTag("low_stock_badge_${product.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Low stock alert",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${product.stockQuantity} left",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected In-Cart Quantity Indicator
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "$cartQuantity",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isOutOfStock) Slate400 else Slate800,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Visual Stock Indicator Pill & Threshold Meter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isOutOfStock -> Red50
                            isLowStock -> Amber100.copy(alpha = 0.7f)
                            else -> Slate50
                        }
                    )
                    .border(
                        width = 0.5.dp,
                        color = when {
                            isOutOfStock -> Red200
                            isLowStock -> Amber200
                            else -> Slate200
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("low_stock_indicator_${product.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (isLowStock) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stock warning",
                            tint = Amber600,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = when {
                            isOutOfStock -> "0 in stock"
                            isLowStock -> "${product.stockQuantity} left (Alert: ≤$effectiveThreshold)"
                            else -> "${product.stockQuantity} ${product.unit} in stock"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = when {
                            isOutOfStock -> Red600
                            isLowStock -> Amber700
                            else -> Slate500
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Mini Visual Stock Level Bar
                val stockCap = maxOf(effectiveThreshold * 2, 20)
                val ratio = (product.stockQuantity.toFloat() / stockCap.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isOutOfStock -> Red100
                                isLowStock -> Amber200
                                else -> Slate200
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .background(
                                when {
                                    isOutOfStock -> Red600
                                    isLowStock -> Amber500
                                    else -> Emerald600
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price and Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.formattedPrice,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = if (isOutOfStock) Slate400 else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per ${product.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Slate400
                    )
                }

                if (!isOutOfStock) {
                    if (cartQuantity > 0) {
                        // Stepper Control for items in cart
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(TealCard, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { onRemoveFromCart(product) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("remove_button_${product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease quantity",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "$cartQuantity",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onAddToCart(product) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("add_more_button_${product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase quantity",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Add to Cart Primary Button (with 48dp touch target)
                        FilledIconButton(
                            onClick = { onAddToCart(product) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("add_button_${product.id}"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add ${product.name} to cart",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCategoryFallback(category: ProductCategory) {
    val (icon: ImageVector, bgColors: List<Color>) = when (category) {
        ProductCategory.BEVERAGES -> Icons.Default.LocalCafe to listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))
        ProductCategory.SNACKS -> Icons.Default.Fastfood to listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
        ProductCategory.INSTANT_MEALS -> Icons.Default.RamenDining to listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA))
        ProductCategory.CANNED_GOODS -> Icons.Default.SoupKitchen to listOf(Color(0xFFFEE2E2), Color(0xFFFECACA))
        ProductCategory.HOUSEHOLD -> Icons.Default.CleaningServices to listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE))
        ProductCategory.ALL -> Icons.Default.ShoppingBag to listOf(Slate100, Slate200)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(bgColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Slate800.copy(alpha = 0.6f),
            modifier = Modifier.size(44.dp)
        )
    }
}
