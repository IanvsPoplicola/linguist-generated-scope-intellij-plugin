package com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.utils

import com.google.common.math.IntMath.pow

class FilePatternMapper {
    companion object {
        fun mapGitPatternToIntellijPattern(line: String): Set<String> {
            var intellijPattern = line.trimStart('/') // standardise the pattern to not have leading /

            // if Git pattern ends with */**, the IntelliJ equivalent is //*
            val matchAllSuffix1 = "(?:/|^)\\*/\\*\\*$".toRegex()
            intellijPattern = intellijPattern.replace(matchAllSuffix1, "//*")

            // if Git pattern ends with **/*, the IntelliJ equivalent is //*
            val matchAllSuffix2 = "(?:/|^)\\*\\*/\\*$".toRegex()
            intellijPattern = intellijPattern.replace(matchAllSuffix2, "//*")

            // if Git pattern ends with **, the IntelliJ equivalent is //*
            val matchAllSuffix3 = "(?:/|^)\\*\\*$".toRegex()
            intellijPattern = intellijPattern.replace(matchAllSuffix3, "//*")

            // if Git pattern has /**/ next to a /*/, remove the /**/ part
            val matchOneOrMoreDirectories1 = "(?<=/|^)\\*\\*/\\*(?=/|$)".toRegex()
            intellijPattern = intellijPattern.replace(matchOneOrMoreDirectories1, "*")
            val matchOneOrMoreDirectories2 = "(?<=/|^)\\*/\\*\\*(?=/|$)".toRegex()
            intellijPattern = intellijPattern.replace(matchOneOrMoreDirectories2, "*")

            // for the next step, first eliminate any ** that's a part of a file/directory name
            val doubleAsterisksInWords1 = "(?<!/|^)\\*\\*".toRegex() // any non-prefix ** not preceded by /
            intellijPattern = intellijPattern.replace(doubleAsterisksInWords1, "*")
            val doubleAsterisksInWords2 = "\\*\\*(?!/|$)".toRegex() // any non-suffix ** not followed by /
            intellijPattern = intellijPattern.replace(doubleAsterisksInWords2, "*")
            // for the remaining **/, change each to either an empty string or */
            // we want to create all possible combinations
            val intellijPatternParts = intellijPattern.split("**/")
            if (intellijPatternParts.size < 2) {
                return setOf(intellijPattern) // return directly if no **/ is found
            }
            val possibleAsteriskReplacements = arrayOf("", "*/")
            val intellijPatterns = mutableSetOf<String>()
            for (counter: Int in 0 until pow(2, intellijPatternParts.size)) {
                var subCounter = counter
                var pattern = ""
                for (index: Int in intellijPatternParts.indices) {
                    pattern += intellijPatternParts[index]
                    if (index != intellijPatternParts.size - 1) {
                        pattern += possibleAsteriskReplacements[subCounter % 2]
                        subCounter /= 2
                    }
                }
                intellijPatterns.add(pattern)
            }

            return intellijPatterns
        }
    }

}
