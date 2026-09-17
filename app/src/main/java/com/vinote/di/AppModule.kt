package com.vinote.di

import android.app.Application
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
import com.vinote.data.local.RecurringTransactionDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionCategoryDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.TransactionTemplateDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.repository.AuthRepository
import com.vinote.data.repository.AuthRepositoryImpl
import com.vinote.data.repository.WalletGatewayRepository
import com.vinote.data.supabase.SupabaseClientProvider
import com.vinote.domain.ai.NoTaAiService
import com.vinote.domain.ai.NoTaFinanceTools
import com.vinote.domain.transaction.TransactionService
import com.vinote.domain.usecase.BudgetUseCaseImpl
import com.vinote.domain.usecase.BudgetUseCaseInterface
import com.vinote.domain.usecase.ChatUseCaseImpl
import com.vinote.domain.usecase.ChatUseCaseInterface
import com.vinote.domain.usecase.EwalletUseCaseImpl
import com.vinote.domain.usecase.EwalletUseCaseInterface
import com.vinote.domain.usecase.GoalUseCaseImpl
import com.vinote.domain.usecase.GoalUseCaseInterface
import com.vinote.domain.usecase.ReceiptUseCaseImpl
import com.vinote.domain.usecase.ReceiptUseCaseInterface
import com.vinote.domain.usecase.TransactionUseCaseImpl
import com.vinote.domain.usecase.TransactionUseCaseInterface
import com.vinote.domain.usecase.VoiceUseCaseImpl
import com.vinote.domain.usecase.VoiceUseCaseInterface
import com.vinote.services.ai.HybridAiProcessor
import com.vinote.services.wallet.WalletDeduplicationService
import com.vinote.services.wallet.WalletTransactionProcessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Provides
    fun provideTransactionTemplateDao(db: NoTaDatabase): TransactionTemplateDao = db.transactionTemplateDao()

    @Provides
    fun provideRecurringTransactionDao(db: NoTaDatabase): RecurringTransactionDao = db.recurringTransactionDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideTransactionService(
        transactionDao: TransactionDao
    ): TransactionService = TransactionService(transactionDao)

    @Provides
    @Singleton
    fun provideHybridAiProcessor(
        @ApplicationContext context: Context
    ): HybridAiProcessor = HybridAiProcessor(context as Application)

    @Provides
    @Singleton
    fun provideNoTaAiService(
        openRouterClient: OpenRouterClient
    ): NoTaAiService = NoTaAiService(openRouterClient)

    @Provides
    @Singleton
    fun provideNoTaFinanceTools(
        transactionDao: TransactionDao,
        goalDao: GoalDao,
        walletAccountDao: WalletAccountDao,
        budgetDao: BudgetDao,
        recurringTransactionDao: RecurringTransactionDao
    ): NoTaFinanceTools = NoTaFinanceTools(transactionDao, goalDao, walletAccountDao, budgetDao, recurringTransactionDao)

    @Provides
    @Singleton
    fun provideTransactionUseCase(
        transactionDao: TransactionDao,
        templateDao: TransactionTemplateDao,
        recurringDao: RecurringTransactionDao,
        transactionService: TransactionService
    ): TransactionUseCaseInterface = TransactionUseCaseImpl(transactionDao, templateDao, recurringDao, transactionService)

    @Provides
    @Singleton
    fun provideBudgetUseCase(
        budgetDao: BudgetDao
    ): BudgetUseCaseInterface = BudgetUseCaseImpl(budgetDao)

    @Provides
    @Singleton
    fun provideGoalUseCase(
        goalDao: GoalDao
    ): GoalUseCaseInterface = GoalUseCaseImpl(goalDao)

    @Provides
    @Singleton
    fun provideChatUseCase(
        aiService: NoTaAiService,
        financeTools: NoTaFinanceTools
    ): ChatUseCaseInterface = ChatUseCaseImpl(aiService, financeTools)

    @Provides
    @Singleton
    fun provideReceiptUseCase(
        hybridAiProcessor: HybridAiProcessor
    ): ReceiptUseCaseInterface = ReceiptUseCaseImpl(hybridAiProcessor)

    @Provides
    @Singleton
    fun provideVoiceUseCase(
        hybridAiProcessor: HybridAiProcessor
    ): VoiceUseCaseInterface = VoiceUseCaseImpl(hybridAiProcessor)

    @Provides
    @Singleton
    fun provideWalletGatewayRepository(
        midtransGatewayService: MidtransGatewayService,
        unofficialGoPayService: UnofficialGoPayService,
        unofficialOvoService: UnofficialOvoService,
        unofficialDanaService: UnofficialDanaService,
        walletAccountDao: WalletAccountDao,
        supabaseClientProvider: SupabaseClientProvider
    ): WalletGatewayRepository = WalletGatewayRepository(
        midtransGatewayService,
        unofficialGoPayService,
        unofficialOvoService,
        unofficialDanaService,
        walletAccountDao,
        supabaseClientProvider
    )

    @Provides
    @Singleton
    fun provideEwalletUseCase(
        walletGatewayRepository: WalletGatewayRepository,
        walletAccountDao: WalletAccountDao
    ): EwalletUseCaseInterface = EwalletUseCaseImpl(walletGatewayRepository, walletAccountDao)

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
        return OpenRouterClient(
            proxyUrl = "${supabaseClientProvider.supabaseFunctionsUrl}/openrouter-proxy"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
