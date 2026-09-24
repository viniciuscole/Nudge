package dev.viniciuscole.nudge.data.food

import java.text.Normalizer

object LocalFoods {
    private fun g(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "g", kcal, p, c, f, FoodItem.SOURCE_LOCAL, PORTIONS[name])

    private fun ml(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "ml", kcal, p, c, f, FoodItem.SOURCE_LOCAL, PORTIONS[name])

    // Must stay above ALL: object properties initialise in declaration order, and ALL reads this map.
    private val PORTIONS: Map<String, Portion> = mapOf(
        "Pão francês" to Portion("pão", "pães", 50.0),
        "Pão de forma integral" to Portion("fatia", "fatias", 25.0),
        "Pão de queijo" to Portion("unidade", "unidades", 20.0),
        "Tapioca (goma)" to Portion("colher de sopa", "colheres de sopa", 15.0),
        "Aveia em flocos" to Portion("colher de sopa", "colheres de sopa", 15.0),
        "Arroz branco cozido" to Portion("colher de servir", "colheres de servir", 45.0),
        "Arroz integral cozido" to Portion("colher de servir", "colheres de servir", 45.0),
        "Feijão carioca cozido" to Portion("concha", "conchas", 80.0),
        "Feijão preto cozido" to Portion("concha", "conchas", 80.0),
        "Lentilha cozida" to Portion("concha", "conchas", 80.0),
        "Ovo cozido" to Portion("ovo", "ovos", 50.0),
        "Ovo frito" to Portion("ovo", "ovos", 50.0),
        "Leite integral" to Portion("copo", "copos", 200.0),
        "Leite desnatado" to Portion("copo", "copos", 200.0),
        "Iogurte natural" to Portion("pote", "potes", 170.0),
        "Iogurte grego" to Portion("pote", "potes", 100.0),
        "Queijo minas frescal" to Portion("fatia", "fatias", 30.0),
        "Queijo mussarela" to Portion("fatia", "fatias", 15.0),
        "Requeijão cremoso" to Portion("colher de sopa", "colheres de sopa", 30.0),
        "Manteiga" to Portion("colher de chá", "colheres de chá", 5.0),
        "Banana prata" to Portion("banana", "bananas", 70.0),
        "Maçã" to Portion("maçã", "maçãs", 130.0),
        "Laranja" to Portion("laranja", "laranjas", 150.0),
        "Azeite de oliva" to Portion("colher de sopa", "colheres de sopa", 13.0),
        "Óleo de soja" to Portion("colher de sopa", "colheres de sopa", 13.0),
        "Açúcar refinado" to Portion("colher de chá", "colheres de chá", 5.0),
        "Mel" to Portion("colher de sopa", "colheres de sopa", 20.0),
        "Castanha-do-pará" to Portion("unidade", "unidades", 4.0),
        "Whey protein (pó)" to Portion("scoop", "scoops", 30.0),
        "Café coado sem açúcar" to Portion("xícara", "xícaras", 50.0),
        "Suco de laranja natural" to Portion("copo", "copos", 200.0),
    )

    val ALL: List<FoodItem> = listOf(
        g("Chicken breast", 165.0, 31.0, 0.0, 3.6),
        g("Chicken thigh", 209.0, 26.0, 0.0, 11.0),
        g("Chickpeas, cooked", 164.0, 8.9, 27.0, 2.6),
        g("Rice, cooked", 130.0, 2.7, 28.0, 0.3),
        ml("Olive oil", 884.0, 0.0, 0.0, 100.0),
        g("Avocado", 160.0, 2.0, 9.0, 15.0),
        g("Greek yogurt", 59.0, 10.0, 3.6, 0.4),
        g("Egg", 155.0, 13.0, 1.1, 11.0),
        g("Broccoli", 34.0, 2.8, 7.0, 0.4),
        g("Almonds", 579.0, 21.0, 22.0, 50.0),
        g("Salmon", 208.0, 20.0, 0.0, 13.0),
        g("Cherry tomatoes", 18.0, 0.9, 3.9, 0.2),
        g("Spinach", 23.0, 2.9, 3.6, 0.4),
        g("Wholegrain bread", 247.0, 13.0, 41.0, 3.4),
        g("Feta", 264.0, 14.0, 4.1, 21.0),

        // Tabela brasileira (valores por 100 g/ml, referência TACO)
        g("Arroz branco cozido", 128.0, 2.5, 28.1, 0.2),
        g("Arroz integral cozido", 124.0, 2.6, 25.8, 1.0),
        g("Macarrão cozido", 102.0, 3.3, 19.9, 1.3),
        g("Pão francês", 300.0, 8.0, 58.6, 3.1),
        g("Pão de forma integral", 253.0, 9.4, 49.9, 3.7),
        g("Tapioca (goma)", 240.0, 0.1, 59.4, 0.1),
        g("Aveia em flocos", 394.0, 13.9, 66.6, 8.5),
        g("Cuscuz de milho cozido", 113.0, 2.4, 25.3, 0.5),
        g("Farinha de mandioca", 365.0, 1.6, 87.9, 0.3),
        g("Pão de queijo", 363.0, 5.0, 40.0, 19.0),
        g("Feijão carioca cozido", 76.0, 4.8, 13.6, 0.5),
        g("Feijão preto cozido", 77.0, 4.5, 14.0, 0.5),
        g("Lentilha cozida", 93.0, 6.3, 16.3, 0.5),
        g("Grão-de-bico cozido", 164.0, 8.9, 27.0, 2.6),
        g("Peito de frango grelhado", 163.0, 31.5, 0.0, 3.2),
        g("Coxa de frango assada", 215.0, 26.9, 0.0, 11.4),
        g("Patinho moído cozido", 219.0, 31.9, 0.0, 9.2),
        g("Contrafilé grelhado", 278.0, 32.0, 0.0, 16.0),
        g("Picanha grelhada", 249.0, 26.4, 0.0, 15.5),
        g("Lombo suíno assado", 210.0, 35.7, 0.0, 6.4),
        g("Linguiça toscana", 296.0, 16.1, 0.0, 25.6),
        g("Bacon frito", 541.0, 37.0, 0.0, 43.0),
        g("Ovo cozido", 146.0, 13.3, 0.6, 9.5),
        g("Ovo frito", 240.0, 15.6, 1.2, 18.6),
        g("Atum em lata (em água)", 116.0, 25.5, 0.0, 1.0),
        g("Sardinha em lata", 208.0, 24.6, 0.0, 12.0),
        g("Tilápia grelhada", 96.0, 20.1, 0.0, 1.7),
        g("Salmão grelhado", 208.0, 20.0, 0.0, 13.0),
        ml("Leite integral", 61.0, 2.9, 4.3, 3.2),
        ml("Leite desnatado", 35.0, 2.9, 4.9, 0.2),
        g("Iogurte natural", 51.0, 4.1, 1.9, 3.0),
        g("Iogurte grego", 97.0, 9.0, 4.0, 5.0),
        g("Queijo minas frescal", 264.0, 17.4, 3.2, 20.2),
        g("Queijo mussarela", 330.0, 22.6, 3.0, 25.2),
        g("Requeijão cremoso", 257.0, 9.6, 3.0, 23.0),
        g("Manteiga", 726.0, 0.4, 0.1, 82.4),
        g("Banana prata", 98.0, 1.3, 26.0, 0.1),
        g("Maçã", 56.0, 0.3, 15.2, 0.0),
        g("Mamão", 45.0, 0.8, 11.6, 0.1),
        g("Laranja", 45.0, 1.0, 11.5, 0.1),
        g("Manga", 64.0, 0.4, 16.7, 0.2),
        g("Melancia", 33.0, 0.9, 8.1, 0.0),
        g("Abacate", 96.0, 1.2, 6.0, 8.4),
        g("Morango", 30.0, 0.9, 6.8, 0.3),
        g("Batata cozida", 52.0, 1.2, 11.9, 0.0),
        g("Batata doce cozida", 77.0, 0.6, 18.4, 0.1),
        g("Mandioca cozida", 125.0, 0.6, 30.1, 0.3),
        g("Cenoura crua", 34.0, 1.3, 7.7, 0.2),
        g("Tomate", 15.0, 1.1, 3.1, 0.2),
        g("Alface", 15.0, 1.4, 2.9, 0.2),
        g("Cebola", 39.0, 1.7, 8.9, 0.1),
        g("Abobrinha", 19.0, 1.1, 4.3, 0.1),
        g("Brócolis cozido", 25.0, 2.1, 4.4, 0.5),
        g("Couve refogada", 90.0, 1.7, 5.7, 7.2),
        ml("Azeite de oliva", 884.0, 0.0, 0.0, 100.0),
        ml("Óleo de soja", 884.0, 0.0, 0.0, 100.0),
        g("Açúcar refinado", 387.0, 0.0, 99.5, 0.0),
        g("Mel", 309.0, 0.4, 84.0, 0.0),
        g("Castanha-do-pará", 643.0, 14.5, 15.1, 63.5),
        g("Amendoim torrado", 544.0, 27.2, 20.3, 43.9),
        g("Amêndoas", 579.0, 21.0, 22.0, 50.0),
        g("Whey protein (pó)", 380.0, 75.0, 8.0, 5.0),
        ml("Café coado sem açúcar", 2.0, 0.1, 0.3, 0.0),
        ml("Suco de laranja natural", 37.0, 0.7, 8.7, 0.1),
    )

    fun search(query: String, limit: Int = 4): List<FoodItem> {
        val q = fold(query)
        if (q.isEmpty()) return emptyList()
        return ALL.filter { fold(it.name).contains(q) }.take(limit)
    }

    // "feijao" has to find "Feijão": strip diacritics from both sides before matching.
    fun fold(s: String): String =
        Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    fun portionFor(name: String): Portion? = PORTIONS[name]
}
