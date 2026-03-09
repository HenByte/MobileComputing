package com.example.soloproject

//This enables to save all the necessary data about a fruit.
data class Fruit(
    val name: String,
    val icon: String,
    val fruitId: Long
)
// This is a list of fruits that are used in this mobile application.
val fruits = listOf(
    Fruit(name = "Banana",    icon = "\uD83C\uDF4C", fruitId = 28934L),
    Fruit(name = "Apple",     icon = "\uD83C\uDF4E", fruitId = 28942L),
    Fruit(name = "Pineapple", icon = "\uD83C\uDF4D", fruitId = 11056L),
    Fruit(name = "Orange",    icon = "\uD83C\uDF4A", fruitId = 11045L),
    Fruit(name = "Kiwi",      icon = "\uD83E\uDD5D", fruitId = 11050L),
)
