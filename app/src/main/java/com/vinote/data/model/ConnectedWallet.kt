package com.vinote.data.model

data class ConnectedWallet(
    val id: String,
    val name: String,
    val isConnected: Boolean = true,
    val isActiveSync: Boolean = true,
    val iconColorHex: String = "#0057C2",
    val description: String = "Active Sync"
)
