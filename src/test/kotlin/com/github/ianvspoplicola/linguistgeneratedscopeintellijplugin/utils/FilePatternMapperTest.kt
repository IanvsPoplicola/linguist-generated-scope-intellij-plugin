package com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.utils

import com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.utils.FilePatternMapper.Companion.mapGitPatternToIntellijPattern
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class FilePatternMapperTest {

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
    }

    @Test
    fun `simple case`() = checkGitPatternMapsToExpected("mundus", setOf("mundus"))

    @Test
    fun `multiple directories`() = checkGitPatternMapsToExpected("mundus/dei/iuppiter", setOf("mundus/dei/iuppiter"))

    @Test
    fun `redundant leading slash is removed`() = checkGitPatternMapsToExpected("/mundus", setOf("mundus"))

    @Test
    fun `single asterisk`() = checkGitPatternMapsToExpected("*", setOf("*"))

    @Test
    fun `slash then single asterisk`() = checkGitPatternMapsToExpected("/*", setOf("*"))

    @Test
    fun `leading single asterisk`() = checkGitPatternMapsToExpected("*/mundus", setOf("*/mundus"))

    @Test
    fun `middle single asterisk`() = checkGitPatternMapsToExpected("mundus/*/iuppiter", setOf("mundus/*/iuppiter"))

    @Test
    fun `trailing single asterisk`() = checkGitPatternMapsToExpected("mundus/dei/*", setOf("mundus/dei/*"))

    @Test
    fun `leading single asterisk in word`() = checkGitPatternMapsToExpected("mundus/dei/*ppiter", setOf("mundus/dei/*ppiter"))

    @Test
    fun `trailing single asterisk in word`() = checkGitPatternMapsToExpected("mundus/dei/iupp*", setOf("mundus/dei/iupp*"))

    @Test
    fun `double asterisks`() = checkGitPatternMapsToExpected("**", setOf("//*"))

    @Test
    fun `slash then double asterisks`() = checkGitPatternMapsToExpected("**", setOf("//*"))

    @Test
    fun `leading double asterisks`() = checkGitPatternMapsToExpected("**/mundus", setOf("mundus", "*/mundus"))

    @Test
    fun `middle double asterisks`() = checkGitPatternMapsToExpected("mundus/**/iuppiter", setOf("mundus/iuppiter", "mundus/*/iuppiter"))

    @Test
    fun `trailing double asterisks`() = checkGitPatternMapsToExpected("mundus/dei/**", setOf("mundus/dei//*"))

    @Test
    fun `leading double asterisks in word`() = checkGitPatternMapsToExpected("mundus/dei/**ppiter", setOf("mundus/dei/*ppiter"))

    @Test
    fun `trailing double asterisks in word`() = checkGitPatternMapsToExpected("mundus/dei/iupp**", setOf("mundus/dei/iupp*"))

    @Test
    fun `combination of double asterisks`() = checkGitPatternMapsToExpected("**/mundus/**/iuppiter/tela/**/fulminat**/**", setOf(
            "mundus/iuppiter/tela/fulminat*//*",
            "mundus/iuppiter/tela/*/fulminat*//*",
            "mundus/*/iuppiter/tela/fulminat*//*",
            "mundus/*/iuppiter/tela/*/fulminat*//*",
            "*/mundus/iuppiter/tela/fulminat*//*",
            "*/mundus/iuppiter/tela/*/fulminat*//*",
            "*/mundus/*/iuppiter/tela/fulminat*//*",
            "*/mundus/*/iuppiter/tela/*/fulminat*//*",
    ))

    @Test
    fun `single then double asterisks`() = checkGitPatternMapsToExpected("*/**", setOf("//*"))

    @Test
    fun `double then single asterisks`() = checkGitPatternMapsToExpected("**/*", setOf("//*"))

    @Test
    fun `leading single then double asterisks`() = checkGitPatternMapsToExpected("*/**/dei/iuppiter", setOf("*/dei/iuppiter"))

    @Test
    fun `leading double then single asterisks`() = checkGitPatternMapsToExpected("**/*/dei/iuppiter", setOf("*/dei/iuppiter"))

    @Test
    fun `middle single then double asterisks`() = checkGitPatternMapsToExpected("mundus/*/**/iuppiter", setOf("mundus/*/iuppiter"))

    @Test
    fun `middle double then single asterisks`() = checkGitPatternMapsToExpected("mundus/**/*/iuppiter", setOf("mundus/*/iuppiter"))

    @Test
    fun `trailing single then double asterisks`() = checkGitPatternMapsToExpected("mundus/dei/*/**", setOf("mundus/dei//*"))

    @Test
    fun `trailing double then single asterisks`() = checkGitPatternMapsToExpected("mundus/dei/**/*", setOf("mundus/dei//*"))

    @Test
    fun `combination of single and double asterisks`() = checkGitPatternMapsToExpected("**/mundus/*/iuppiter/tela/**/fulminat*/*", setOf(
            "mundus/*/iuppiter/tela/fulminat*/*",
            "mundus/*/iuppiter/tela/*/fulminat*/*",
            "*/mundus/*/iuppiter/tela/fulminat*/*",
            "*/mundus/*/iuppiter/tela/*/fulminat*/*",
    ))

    private fun checkGitPatternMapsToExpected(input: String, expected: Set<String>) {
        val actual = mapGitPatternToIntellijPattern(input)
        assertEquals(expected, actual)
    }
}
