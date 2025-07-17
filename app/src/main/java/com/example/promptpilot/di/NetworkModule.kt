package com.example.promptpilot.di

import com.example.promptpilot.data.api.BackendApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
//    @Singleton
//    @Provides
//    fun provideOkHttpClient() = OkHttpClient.Builder()
//        .addNetworkInterceptor(
//            Interceptor { chain ->
//                var request: Request? = null
//                val original = chain.request()
//                // Request customization: add request headers
//                val requestBuilder = original.newBuilder()
//                    .addHeader("Authorization", "Bearer $openAIApiKey")
//                request = requestBuilder.build()
//                chain.proceed(request)
//            })
//        .build()
//
//    @Singleton
//    @Provides
//    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
//        val gson = GsonBuilder().setLenient().create()
//
//        return Retrofit.Builder()
//            .baseUrl(baseUrlOpenAI)
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .build()
//    }
    @Provides
    @Singleton
    fun provideBackendOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .build()
}
    @Provides
    @Singleton
    fun provideBackendRetrofit(backendOkHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.54.138.76:8000/")
            .client(backendOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBackendApi(backendRetrofit: Retrofit): BackendApi {
        return backendRetrofit.create(BackendApi::class.java)
    }
//    @Provides
//    @Singleton
//    fun provideOpenAIService(retrofit: Retrofit): OpenAIApi =
//        retrofit.create(OpenAIApi::class.java)
}