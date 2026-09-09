package com.example.domain.model

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.local.entity.ProductEntity

enum class ProductCategory(val displayName: String) {
    ALL("All Items"),
    BEVERAGES("Beverages"),
    SNACKS("Snacks"),
    INSTANT_MEALS("Instant Meals"),
    CANNED_GOODS("Canned Goods"),
    HOUSEHOLD("Household")
}

data class ProductItem(
    val id: String,
    val name: String,
    val price: Double,
    val category: ProductCategory,
    val costPrice: Double = 0.0,
    @param:DrawableRes val imageResId: Int? = null,
    val imageUrl: String? = null,
    val drawableResName: String? = null,
    val stockQuantity: Int = 50,
    val lowStockThreshold: Int = 5,
    val barcode: String? = null,
    val unit: String = "pc"
) {
    val formattedPrice: String
        get() = "₱${String.format("%,.2f", price)}"

    val formattedCostPrice: String
        get() = "₱${String.format("%,.2f", costPrice)}"

    val profitMargin: Double
        get() = price - costPrice

    val formattedProfitMargin: String
        get() = "₱${String.format("%,.2f", profitMargin)}"

    val profitMarginPercent: Double
        get() = if (costPrice > 0) ((price - costPrice) / costPrice) * 100.0 else 0.0

    val isLowStock: Boolean
        get() = stockQuantity in 1..lowStockThreshold

    val isOutOfStock: Boolean
        get() = stockQuantity <= 0
}

data class CartItem(
    val product: ProductItem,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = product.price * quantity

    val formattedSubtotal: String
        get() = "₱${String.format("%,.2f", subtotal)}"
}

object SampleProductCatalog {
    val items = listOf(
        ProductItem(
            id = "prod_1",
            name = "Coca-Cola Original 330ml Can",
            price = 38.00,
            costPrice = 30.00,
            category = ProductCategory.BEVERAGES,
            imageResId = R.drawable.img_beverage_can,
            drawableResName = "img_beverage_can",
            stockQuantity = 42,
            lowStockThreshold = 10,
            barcode = "4800016644021",
            unit = "can"
        ),
        ProductItem(
            id = "prod_2",
            name = "Piattos Cheese Potato Crisps 85g",
            price = 42.50,
            costPrice = 34.00,
            category = ProductCategory.SNACKS,
            imageResId = R.drawable.img_snack_chips,
            drawableResName = "img_snack_chips",
            stockQuantity = 24,
            lowStockThreshold = 8,
            barcode = "4800016024113",
            unit = "pack"
        ),
        ProductItem(
            id = "prod_3",
            name = "Lucky Me! Instant Cup Noodles 60g",
            price = 28.00,
            costPrice = 22.00,
            category = ProductCategory.INSTANT_MEALS,
            imageResId = R.drawable.img_instant_noodles,
            drawableResName = "img_instant_noodles",
            stockQuantity = 35,
            lowStockThreshold = 10,
            barcode = "4807770270014",
            unit = "cup"
        ),
        ProductItem(
            id = "prod_4",
            name = "San Miguel Pale Pilsen 330ml Can",
            price = 55.00,
            costPrice = 45.00,
            category = ProductCategory.BEVERAGES,
            imageResId = R.drawable.img_beverage_can,
            drawableResName = "img_beverage_can",
            stockQuantity = 18,
            lowStockThreshold = 10,
            barcode = "4800016001015",
            unit = "can"
        ),
        ProductItem(
            id = "prod_5",
            name = "Oishi Prawn Crackers 60g",
            price = 22.00,
            costPrice = 17.50,
            category = ProductCategory.SNACKS,
            imageResId = R.drawable.img_snack_chips,
            drawableResName = "img_snack_chips",
            stockQuantity = 4,
            lowStockThreshold = 8,
            barcode = "4800194112019",
            unit = "pack"
        ),
        ProductItem(
            id = "prod_6",
            name = "Lucky Me! Pancit Canton Kalamansi",
            price = 19.50,
            costPrice = 15.00,
            category = ProductCategory.INSTANT_MEALS,
            imageResId = R.drawable.img_instant_noodles,
            drawableResName = "img_instant_noodles",
            stockQuantity = 50,
            lowStockThreshold = 12,
            barcode = "4807770271103",
            unit = "pack"
        ),
        ProductItem(
            id = "prod_7",
            name = "Purefoods Corned Beef 150g",
            price = 72.00,
            costPrice = 58.00,
            category = ProductCategory.CANNED_GOODS,
            stockQuantity = 15,
            lowStockThreshold = 5,
            barcode = "4800054001015",
            unit = "can"
        ),
        ProductItem(
            id = "prod_8",
            name = "Argentina Beef Loaf 150g",
            price = 34.00,
            costPrice = 27.00,
            category = ProductCategory.CANNED_GOODS,
            stockQuantity = 28,
            lowStockThreshold = 6,
            barcode = "4800054002029",
            unit = "can"
        ),
        ProductItem(
            id = "prod_9",
            name = "Kopiko Blanca 3-in-1 Coffee 30g",
            price = 15.00,
            costPrice = 11.50,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 60,
            lowStockThreshold = 15,
            barcode = "8996001301018",
            unit = "sachet"
        ),
        ProductItem(
            id = "prod_10",
            name = "Safeguard Pure White Soap 135g",
            price = 48.00,
            costPrice = 38.00,
            category = ProductCategory.HOUSEHOLD,
            stockQuantity = 3,
            lowStockThreshold = 5,
            barcode = "4902430701011",
            unit = "bar"
        ),
        ProductItem(
            id = "prod_11",
            name = "Tide Detergent Powder Bar 40g",
            price = 12.00,
            costPrice = 9.00,
            category = ProductCategory.HOUSEHOLD,
            stockQuantity = 80,
            lowStockThreshold = 15,
            barcode = "4902430702025",
            unit = "bar"
        ),
        ProductItem(
            id = "prod_12",
            name = "Chippy Barbecue Snack 110g",
            price = 32.00,
            costPrice = 25.00,
            category = ProductCategory.SNACKS,
            imageResId = R.drawable.img_snack_chips,
            drawableResName = "img_snack_chips",
            stockQuantity = 0,
            lowStockThreshold = 8,
            barcode = "4800016010024",
            unit = "pack"
        )
    )
}

fun ProductEntity.toProductItem(): ProductItem {
    val categoryEnum = try {
        ProductCategory.valueOf(category)
    } catch (e: Exception) {
        ProductCategory.ALL
    }

    val resId = when (drawableResName) {
        "img_beverage_can" -> R.drawable.img_beverage_can
        "img_snack_chips" -> R.drawable.img_snack_chips
        "img_instant_noodles" -> R.drawable.img_instant_noodles
        else -> null
    }

    return ProductItem(
        id = id,
        name = name,
        price = price,
        costPrice = costPrice,
        category = categoryEnum,
        imageResId = resId,
        drawableResName = drawableResName,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        barcode = barcode,
        unit = unit
    )
}

fun ProductItem.toProductEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        price = price,
        costPrice = costPrice,
        category = category.name,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        barcode = barcode,
        unit = unit,
        drawableResName = drawableResName,
        updatedAt = System.currentTimeMillis()
    )
}
