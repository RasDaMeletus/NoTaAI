package com.vinote.domain.wallet

import com.vinote.data.wallet.adapters.BCAAdapter
import com.vinote.data.wallet.adapters.BRImoAdapter
import com.vinote.data.wallet.adapters.DANAAdapter
import com.vinote.data.wallet.adapters.GenericWalletAdapter
import com.vinote.data.wallet.adapters.GoPayAdapter
import com.vinote.data.wallet.adapters.MandiriAdapter
import com.vinote.data.wallet.adapters.OVOAdapter
import com.vinote.data.wallet.adapters.ShopeePayAdapter

object WalletDetectionService {
    private val adapters: List<WalletAdapter> = listOf(
        GoPayAdapter(),
        DANAAdapter(),
        OVOAdapter(),
        ShopeePayAdapter(),
        BCAAdapter(),
        MandiriAdapter(),
        BRImoAdapter(),
        GenericWalletAdapter()
    )

    fun findAdapterForPackage(packageName: String): WalletAdapter? {
        return adapters.firstOrNull { it.supportedPackageNames.contains(packageName) }
            ?: adapters.lastOrNull() // Generic fallback
    }

    fun getAllAdapters(): List<WalletAdapter> = adapters
}
