package dev.viniciuscole.nudge

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StringsParityTest {

    private fun file(path: String): File = listOf(File(path), File("app/$path")).first { it.exists() }

    private fun specifiers(s: String): List<String> =
        Regex("""%\d+\$[0-9.+-]*[sdf]""").findAll(s).map { it.value }.distinct().sorted().toList()

    private fun entries(path: String): Map<String, List<String>> {
        val xml = file(path).readText()
        val strings = Regex("""<string name="([a-z_]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).associate { it.groupValues[1] to specifiers(it.groupValues[2]) }
        val plurals = Regex("""<plurals name="([a-z_]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).associate { "plurals:" + it.groupValues[1] to specifiers(it.groupValues[2]) }
        return strings + plurals
    }

    @Test
    fun bothLocalesHaveTheSameKeysAndSpecifiers() {
        val en = entries("src/main/res/values/strings.xml")
        val pt = entries("src/main/res/values-pt-rBR/strings.xml")
        assertEquals(en.keys.sorted(), pt.keys.sorted())
        en.forEach { (key, specs) -> assertEquals("specifiers of $key", specs, pt[key]) }
    }
}
