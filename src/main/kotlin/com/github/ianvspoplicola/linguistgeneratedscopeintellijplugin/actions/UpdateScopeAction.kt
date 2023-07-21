package com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.scope.packageSet.*
import com.intellij.util.Processor
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

private val LOGGER = logger<UpdateScopeAction>()

const val SCOPE_NAME_GENERATED = "linguist-generated-true"
const val SCOPE_NAME_NON_GENERATED = "linguist-generated-false"
const val PROGRESS_NAME = "Updating linguist-generated scope"
const val GITATTRIBUTES_FILE_NAME = ".gitattributes"

/**
 * This action synchronises the linguist-generated-true scope from all .gitattributes files in the project.
 */
class UpdateScopeAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {
        // Using the event, evaluate the context,
        // and enable or disable the action.
    }

    override fun actionPerformed(event: AnActionEvent) {
        val progressManager = ProgressManager.getInstance()
        progressManager.run(object : Task.Backgroundable(event.project, PROGRESS_NAME, true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                var filePatternsToIsGenerated = TreeMap<String, Boolean>()
                runReadAction {
                    filePatternsToIsGenerated = gitattributesAsMap(project)
                }

                val localScopeManager = NamedScopeManager.getInstance(project)
                if (localScopeManager.getScope(SCOPE_NAME_GENERATED) == null) {
                    localScopeManager.addScope(localScopeManager.createScope(SCOPE_NAME_GENERATED, InvalidPackageSet("")))
                }
                if (localScopeManager.getScope(SCOPE_NAME_NON_GENERATED) == null) {
                    localScopeManager.addScope(localScopeManager.createScope(SCOPE_NAME_NON_GENERATED, InvalidPackageSet("")))
                }

                val generatedFilesSet = mapToPackageSet(filePatternsToIsGenerated)
                val updatedScopes = arrayOf(
                        NamedScope(localScopeManager.getScope(SCOPE_NAME_GENERATED)!!.scopeId, localScopeManager.getScope(SCOPE_NAME_GENERATED)!!.icon, generatedFilesSet),
                        NamedScope(localScopeManager.getScope(SCOPE_NAME_NON_GENERATED)!!.scopeId, localScopeManager.getScope(SCOPE_NAME_NON_GENERATED)!!.icon, ComplementPackageSet(generatedFilesSet)))

                val unchangedScopes = Stream.of(*localScopeManager.editableScopes).filter { editableScope -> editableScope.scopeId != SCOPE_NAME_GENERATED && editableScope.scopeId != SCOPE_NAME_NON_GENERATED }.collect(Collectors.toList())
                val scopes = unchangedScopes.toTypedArray().plus(updatedScopes)
                localScopeManager.scopes = scopes
            }
        })
    }

    private fun mapToPackageSet(filePatternsToIsGenerated: TreeMap<String, Boolean>): PackageSet {
        if (filePatternsToIsGenerated.isEmpty()) {
            return InvalidPackageSet("")
        }

        val generatedFiles = ArrayList<FilePatternPackageSet>()
        val nonGeneratedFiles = ArrayList<FilePatternPackageSet>()
        filePatternsToIsGenerated.forEach { (filePattern, isGenerated) ->
            if (isGenerated) {
                generatedFiles.add(FilePatternPackageSet(null, filePattern))
            } else {
                nonGeneratedFiles.add(FilePatternPackageSet(null, filePattern))
            }
        }

        val combinedSet = ArrayList<PackageSet>();
        if (generatedFiles.isNotEmpty()) {
            combinedSet.add(UnionPackageSet.create(*generatedFiles.toTypedArray()))
        }
        if (nonGeneratedFiles.isNotEmpty()) {
            combinedSet.add(ComplementPackageSet(UnionPackageSet.create(*nonGeneratedFiles.toTypedArray())))
        }

        return IntersectionPackageSet.create(*combinedSet.toTypedArray())
    }

    private fun gitattributesAsMap(project: Project): TreeMap<String, Boolean> {
        val filePatternsToIsGenerated = TreeMap<String, Boolean>()
        FilenameIndex.processFilesByName(GITATTRIBUTES_FILE_NAME, //
                false, //
                false, //
                object : Processor<PsiFileSystemItem> {
                    override fun process(file: PsiFileSystemItem): Boolean {
                        for (line in file.text.split("\n")) {
                            if (!shouldReadLine(line)) {
                                continue
                            }

                            val relativePath = line.split(" ")[0]
                            val srcPath = replaceAtSymbol(file.virtualFile.path //
                                    .removePrefix(project.basePath!! + "/").removeSuffix(GITATTRIBUTES_FILE_NAME) + relativePath)
                            if (srcPath == null) {
                                continue
                            }
                            if (filePatternsToIsGenerated[srcPath] == false) {
                                continue // if there's a conflict, avoid false positives
                            }
                            filePatternsToIsGenerated[srcPath] = line.endsWith("true")
                        }
                        return true
                    }
                }, //
                GlobalSearchScope.projectScope(project), //
                project //
        )
        return filePatternsToIsGenerated
    }

    private fun shouldReadLine(line: String): Boolean {
        if (line.startsWith("#")) {
            return false
        }

        var trimmed = line.removeSuffix("=true")
        trimmed = trimmed.removeSuffix("=false")
        if (!trimmed.endsWith("linguist-generated")) {
            return false
        }

        val split = line.split(" ")
        if (split.size < 2) {
            return false
        }

        return true
    }

    // This is a workaround for IntelliJ's inability to handle the '@' symbol
    // https://youtrack.jetbrains.com/issue/IDEA-99797/Scope-pattern-cannot-contain-symbol
    private fun replaceAtSymbol(line: String): String? {
        val split = line.split("/")
        split.forEach {
            if (it == "@") {
                return null // there's no way to handle folders that are just called "@"
            }
        }
        return line.replace("@", "*")
    }
}