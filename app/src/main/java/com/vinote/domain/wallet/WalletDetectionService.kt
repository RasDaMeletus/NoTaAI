package com.vinote.domain.wallet

import com.vinote.data.wallet.adapters.BCAAdapter
import com.vinote.data.wallet.adapters.DANAAdapter
import com.vinote.data.wallet.adapters.GenericWalletAdapter
import com.vinote.data.wallet.adapters.GoPayAdapter
import com.vinote.data.wallet.adapters.MandiriAdapter
import com.vinote.data.wallet.adapters.OVOAdapter

object WalletDetectionService {
    private val adapters: List<WalletAdapter> = listOf(
        GoPayAdapter(),
        DANAAdapter(),
        OVOAdapter(),
        BCAAdapter(),
        MandiriAdapter(),
        GenericWalletAdapter()
    )

    fun findAdapterForPackage(packageName: String): WalletAdapter? {
        return adapters.firstOrNull { it.supportedPackageNames.contains(packageName) }
            ?: adapters.lastOrNull() // Generic fallback
    }

    fun getAllAdapters(): List<WalletAdapter> = adapters
}
