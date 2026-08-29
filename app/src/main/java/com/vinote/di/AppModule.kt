package com.vinote.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.vinote.core.ai.OpenRouterClient
import com.vinote.data.local.GoalDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.ViNoteDatabase
import com.vinote.data.repository.AuthRepository
import com.vinote.data.repository.AuthRepositoryImpl
import com.vinote.data.repository.FirestoreExpenseSyncRepository
import com.vinote.domain.ai.ViNoteAiService
import com.vinote.domain.auth.GoogleSignInManager
import com.vinote.domain.transaction.TransactionService
import com.vinote.services.wallet.WalletDeduplicationService
import com.vinote.services.wallet.WalletTransactionProcessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ViNoteDatabase {
        return ViNoteDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideTransactionDao(db: ViNoteDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideGoalDao(db: ViNoteDatabase): GoalDao = db.goalDao()
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
    fun provideGoogleSignInManager(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth
    ): GoogleSignInManager = GoogleSignInManager(context, firebaseAuth)

    @Provides
    @Singleton
    fun provideTransactionService(
        transactionDao: TransactionDao,
        firestoreSyncRepository: FirestoreExpenseSyncRepository
    ): TransactionService = TransactionService(transactionDao, firestoreSyncRepository)

    @Provides
    @Singleton
    fun provideViNoteAiService(
        openRouterClient: OpenRouterClient
    ): ViNoteAiService = ViNoteAiService(openRouterClient)

    @Provides
    @Singleton
    fun provideWalletDeduplicationService(): WalletDeduplicationService =
        WalletDeduplicationService()

    @Provides
    @Singleton
    fun provideWalletTransactionProcessor(
        transactionService: TransactionService,
        deduplicationService: WalletDeduplicationService,
        aiService: ViNoteAiService
    ): WalletTransactionProcessor =
        WalletTransactionProcessor(transactionService, deduplicationService, aiService)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
