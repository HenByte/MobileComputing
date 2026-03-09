package com.example.soloproject

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//This tells the view to what fruit to show.
class FruitViewModel(application: Application) : AndroidViewModel(application) {

    //This reference to the database.
    private val dao = FruitDatabase.getInstance(application).fruitHistoryDao()

    //This is the currently showing fruit.
    var currentFruit by mutableStateOf(fruits.first())
        private set
    //This is the carbs for the fruit.
    var carbs by mutableStateOf<Int?>(null)
        private set

    //This is all the fruits from the database.
    val history = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        fetchCarbs(saveToHistory = false)
    }

    //This selects a fruit and fetches the carbs for it
    fun selectFruit(fruit: Fruit) {
        currentFruit = fruit
        fetchCarbs(saveToHistory = true)
    }

    //This fetches the carbs from Fineli
    fun fetchCarbs(saveToHistory: Boolean = false) {
        val fruit = currentFruit
        viewModelScope.launch {
            carbs = null
            Log.d("FruitViewModel", "Fetching carbs for ${fruit.name} (id=${fruit.fruitId})")
            try {
                val response = withContext(Dispatchers.IO) {
                    FineliModule.fineliApi.getFood(fruit.fruitId)
                }
                Log.d("FruitViewModel", "Response for ${fruit.name}: carbohydrate=${response.carbohydrate}, per100g=${response.carbohydratePer100g}")
                carbs = response.carbohydratePer100g
            } catch (e: Exception) {
                Log.e("FruitViewModel", "Failed to fetch carbs for ${fruit.name}: ${e::class.simpleName}: ${e.message}")
                carbs = null
            }

            if (saveToHistory) {
                dao.insert(FruitHistoryEntry(
                    fruitName = fruit.name,
                    fruitIcon = fruit.icon,
                    carbs = carbs
                ))
                Log.d("FruitViewModel", "Saved ${fruit.name} to history (carbs=$carbs)")
            }
        }
    }
}
