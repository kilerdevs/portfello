package com.portfello.data.bullion

data class BullionCoinSpec(
    val name: String,
    val metal: String,
    val totalWeightGrams: Double,
    val purity: Double
)

object BullionCoinCatalog {

    val coins: List<BullionCoinSpec> = listOf(
        // Złoto
        BullionCoinSpec("Amerykański Orzeł (Au)", "Au", 33.93, 0.9167),
        BullionCoinSpec("Krugerrand", "Au", 33.93, 0.9167),
        BullionCoinSpec("Kanadyjski Liść Klonu (Au)", "Au", 31.11, 0.9999),
        BullionCoinSpec("Wiedeński Filharmonik (Au)", "Au", 31.11, 0.9999),
        BullionCoinSpec("Britannia (Au)", "Au", 31.11, 0.9999),
        BullionCoinSpec("Australijski Kangur (Au)", "Au", 31.11, 0.9999),
        BullionCoinSpec("Chińska Panda (Au)", "Au", 30.0, 0.999),
        BullionCoinSpec("Suweren", "Au", 7.98, 0.9167),
        BullionCoinSpec("Sztabka złota 1 oz", "Au", 31.11, 0.9999),
        BullionCoinSpec("Sztabka złota 100g", "Au", 100.0, 0.9999),
        // Srebro
        BullionCoinSpec("Amerykański Orzeł (Ag)", "Ag", 31.11, 0.999),
        BullionCoinSpec("Kanadyjski Liść Klonu (Ag)", "Ag", 31.11, 0.9999),
        BullionCoinSpec("Wiedeński Filharmonik (Ag)", "Ag", 31.11, 0.999),
        BullionCoinSpec("Britannia (Ag)", "Ag", 31.11, 0.9999),
        BullionCoinSpec("Australijski Kangur (Ag)", "Ag", 31.11, 0.9999),
        BullionCoinSpec("Dolar Morgana", "Ag", 26.73, 0.900),
        BullionCoinSpec("Dolar Pokoju", "Ag", 26.73, 0.900),
        BullionCoinSpec("Sztabka srebra 1 oz", "Ag", 31.11, 0.9999),
        BullionCoinSpec("Sztabka srebra 100 oz", "Ag", 3110.3, 0.9999),
        // Platyna
        BullionCoinSpec("Amerykański Orzeł (Pt)", "Pt", 31.11, 0.9995),
        BullionCoinSpec("Kanadyjski Liść Klonu (Pt)", "Pt", 31.11, 0.9995),
        BullionCoinSpec("Sztabka platyny 1 oz", "Pt", 31.11, 0.9995),
        // Pallad
        BullionCoinSpec("Kanadyjski Liść Klonu (Pd)", "Pd", 31.11, 0.9995),
        BullionCoinSpec("Sztabka palladu 1 oz", "Pd", 31.11, 0.9995),
    )

    fun byMetal(metal: String): List<BullionCoinSpec> = coins.filter { it.metal == metal }
}
