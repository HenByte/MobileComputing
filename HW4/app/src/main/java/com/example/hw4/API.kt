package com.example.hw4

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit


//This handles the network calls.
object FineliModule {
    //This creates the HTTP-client.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .build()
            )
        }
        .build()
    //This creates the retrofit client that uses the HTTP-client.
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://fineli.fi/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    //This is the API what the app uses
    val fineliApi: FineliApi = retrofit.create(FineliApi::class.java)
}

//This describes endpoint in fineli-API that returns the nutritionvalues.
interface FineliApi {
    @GET("fineli/api/v1/foods/{id}")
    suspend fun getFood(@Path("id") foodId: Long): FineliFoodResponse
}
//This is the response from the API.
data class FineliFoodResponse(
    @SerializedName("carbohydrate") val carbohydrate: Double
) {

    //This is amount of carbs that has been fetched from the Fineli-API.
    val carbohydratePer100g: Int
        get() = carbohydrate.toInt().coerceAtLeast(0)
}
