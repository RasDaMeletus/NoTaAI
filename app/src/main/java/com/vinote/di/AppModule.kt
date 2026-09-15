package com.vinote.di

import android.content.Context
import com.vinote.core.ai.OpenRouterClient
import com.vinote.data.gateway.MidtransGatewayService
import com.vinote.data.gateway.PaymentGatewayService
import com.vinote.data.gateway.UnofficialDanaService
import com.vinote.data.gateway.UnofficialGoPayService
import com.vinote.data.gateway.UnofficialOvoService
import com.vinote.data.local.BudgetDao
import com.vinote.data.local.GoalDao
import com.vinote.data.local.NoTaDatabase
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionCategoryDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.repository.AuthRepository
import com.vinote.data.repository.AuthRepositoryImpl
import com.vinote.data.repository.FirestoreExpenseSyncRepository
import com.vinote.data.repository.FirestoreWalletBudgetSyncRepository
import com.vinote.data.repository.WalletGatewayRepository
import com.vinote.data.supabase.SupabaseClientProvider
import com.vinote.domain.ai.NoTaAiService
import com.vinote.domain.transaction.TransactionService
import com.vinote.services.wallet.WalletDeduplicationService
import com.vinote.services.wallet.WalletTransactionProcessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoTaDatabase {
            return NoTaDatabase.getDatabase(context)
        }

        @Provides
        fun provideTransactionDao(db: NoTaDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideGoalDao(db: NoTaDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideWalletAccountDao(db: NoTaDatabase): WalletAccountDao = db.walletAccountDao()

    @Provides
    fun provideBudgetDao(db: NoTaDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideSyncQueueDao(db: NoTaDatabase): SyncQueueDao = db.syncQueueDao()

    @Provides
    fun provideTransactionCategoryDao(db: NoTaDatabase): TransactionCategoryDao = db.transactionCategoryDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideFirestoreExpenseSyncRepository(): FirestoreExpenseSyncRepository =
        FirestoreExpenseSyncRepository()

    @Provides
    @Singleton
    fun provideFirestoreWalletBudgetSyncRepository(
        walletDao: WalletAccountDao,
        budgetDao: BudgetDao,
        syncQueueDao: SyncQueueDao
    ): FirestoreWalletBudgetSyncRepository =
        FirestoreWalletBudgetSyncRepository(
            walletDao = walletDao,
            budgetDao = budgetDao,
            syncQueueDao = syncQueueDao
        )

    @Provides
    @Singleton
    fun provideTransactionService(
        transactionDao: TransactionDao,
        firestoreSyncRepository: FirestoreExpenseSyncRepository
    ): TransactionService = TransactionService(transactionDao, firestoreSyncRepository)

    @Provides
    @Singleton
    fun provideNoTaAiService(
        openRouterClient: OpenRouterClient
    ): NoTaAiService = NoTaAiService(openRouterClient)

    @Provides
    @Singleton
    fun provideWalletDeduplicationService(): WalletDeduplicationService =
        WalletDeduplicationService()

    @Provides
    @Singleton
    fun provideWalletTransactionProcessor(
        transactionService: TransactionService,
        deduplicationService: WalletDeduplicationService,
        aiService: NoTaAiService
    ): WalletTransactionProcessor =
        WalletTransactionProcessor(transactionService, deduplicationService, aiService)
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClientProvider(
        @ApplicationContext context: Context
    ): SupabaseClientProvider = SupabaseClientProvider(context)
}

@Module
@InstallIn(SingletonComponent::class)
object GatewayModule {
    @Provides
    @Singleton
    @IntoSet
    fun provideMidtransGatewayService(supabaseClientProvider: SupabaseClientProvider): PaymentGatewayService {
        return MidtransGatewayService(
            supabaseEdgeFunctionUrl = supabaseClientProvider.supabaseFunctionsUrl,
            supabaseAnonKey = supabaseClientProvider.supabaseAnonKey
        )
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideUnofficialGoPayService(supabaseClientProvider: SupabaseClientProvider): PaymentGatewayService {
        return UnofficialGoPayService(
            supabaseEdgeFunctionUrl = supabaseClientProvider.supabaseFunctionsUrl,
            supabaseAnonKey = supabaseClientProvider.supabaseAnonKey
        )
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideUnofficialDanaService(supabaseClientProvider: SupabaseClientProvider): PaymentGatewayService {
        return UnofficialDanaService(
            supabaseEdgeFunctionUrl = supabaseClientProvider.supabaseFunctionsUrl,
            supabaseAnonKey = supabaseClientProvider.supabaseAnonKey
        )
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideUnofficialOvoService(supabaseClientProvider: SupabaseClientProvider): PaymentGatewayService {
        return UnofficialOvoService(
            supabaseEdgeFunctionUrl = supabaseClientProvider.supabaseFunctionsUrl,
            supabaseAnonKey = supabaseClientProvider.supabaseAnonKey
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideOpenRouterClient(supabaseClientProvider: SupabaseClientProvider): OpenRouterClient {
        // Use the Edge Function proxy for OpenRouter calls (no API key in APK)
        return OpenRouterClient(
            proxyUrl = "${supabaseClientProvider.supabaseFunctionsUrl}/openrouter-proxy"
        ).also { client ->
            // The client needs the Supabase anon key for auth (safe to expose)
            // We don't need to set it here because the Edge Function uses the service role key.
            // But the client might need it for future direct calls; we'll leave it unconfigured for now.
            // client.configure(proxyUrl, supabaseClientProvider.supabaseAnonKey)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}