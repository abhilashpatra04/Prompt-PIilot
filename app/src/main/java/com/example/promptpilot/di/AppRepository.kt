package com.example.promptpilot.di

import com.example.promptpilot.data.remote.ConversationRepository
import com.example.promptpilot.data.remote.ConversationRepositoryImpl
import com.example.promptpilot.data.remote.MessageRepository
import com.example.promptpilot.data.remote.MessageRepositoryImpl
import com.example.promptpilot.data.remote.OpenAIRepository
import com.example.promptpilot.data.remote.OpenAIRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun conversationRepository(
        repo: ConversationRepositoryImpl
    ): ConversationRepository

    @Binds
    abstract fun messageRepository(
        repo: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    abstract fun openAIRepository(
        repo: OpenAIRepositoryImpl
    ): OpenAIRepository
}
