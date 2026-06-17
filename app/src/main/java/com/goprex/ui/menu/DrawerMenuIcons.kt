package com.goprex.ui.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconForMenuItem(rota: String): ImageVector {
    return when {
        rota.contains("home", ignoreCase = true) || rota.contains("dados", ignoreCase = true) -> Icons.Default.Home
        rota.contains("cadastro", ignoreCase = true) || rota.contains("gestao_cadastro", ignoreCase = true) -> Icons.Default.PersonAdd
        rota.contains("gestao_compra", ignoreCase = true) -> Icons.Default.ShoppingBag
        rota.contains("gestao_entrega", ignoreCase = true) -> Icons.Default.LocalShipping
        rota.contains("gestao_venda", ignoreCase = true) -> Icons.Default.ShoppingCart
        rota.contains("pagamento", ignoreCase = true) || rota.contains("sistema_pagamento", ignoreCase = true) -> Icons.Default.Payment
        rota.contains("comprar", ignoreCase = true) -> Icons.Default.AddShoppingCart
        rota.contains("minhas_compra", ignoreCase = true) -> Icons.Default.Receipt
        rota.contains("minhas_entrega", ignoreCase = true) -> Icons.Default.ListAlt
        rota.contains("relatorio", ignoreCase = true) -> Icons.Default.Assessment
        rota.contains("cadastrar_produto", ignoreCase = true) -> Icons.Default.Inventory
        rota.contains("vendas", ignoreCase = true) -> Icons.Default.PointOfSale
        rota.contains("suporte", ignoreCase = true) -> Icons.Default.HeadsetMic
        else -> Icons.Default.ChevronRight
    }
}