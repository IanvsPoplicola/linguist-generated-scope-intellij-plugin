package com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.listeners

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
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

private val LOGGER = logger<UpdateScopeOnSave>()

const val SCOPE_NAME = "linguist-generated-true"
const val PROGRESS_NAME = "Updating linguist-generated scope"
const val GITATTRIBUTES_FILE_NAME = ".gitattributes"

//TODO handle double slashes
//TODO enable select colour
//TODO command clickable: // Generated from services/profile_search/server/profile_search_internal.proto.

/**
 * This listener sets up format on save functionality.
 */
class UpdateScopeOnSave : ActionsOnSaveFileDocumentManagerListener.ActionOnSave() {

    override fun isEnabledForProject(project: Project): Boolean {
        return true
    }

    override fun processDocuments(project: Project, documents: Array<out Document>) {
        val progressManager = ProgressManager.getInstance()
        progressManager.run(
                object : Task.Backgroundable(
                        project,
                        PROGRESS_NAME,
                        true,
                        PerformInBackgroundOption.ALWAYS_BACKGROUND
                ) {
                    override fun run(indicator: ProgressIndicator) {
                        var filePatternsToIsGenerated = TreeMap<String, Boolean>()
                        runReadAction {
                            filePatternsToIsGenerated = gitattributesAsMap(project)
                        }

                        val localScopeManager = NamedScopeManager.getInstance(project)
                        if (localScopeManager.getScope(SCOPE_NAME) == null) {
                            localScopeManager.addScope(localScopeManager.createScope(SCOPE_NAME, InvalidPackageSet("")))
                        }

                        val updatedScope = NamedScope(
                                localScopeManager.getScope(SCOPE_NAME)!!.scopeId,
                                localScopeManager.getScope(SCOPE_NAME)!!.icon,
                                mapToPackageSet(filePatternsToIsGenerated)
                        )

                        val unchangedScopes = Stream.of(*localScopeManager.editableScopes)
                                .filter { editableScope -> editableScope.scopeId != SCOPE_NAME }
                                .collect(Collectors.toList())
                        val scopes = unchangedScopes.toTypedArray().plus(updatedScope)
                        localScopeManager.scopes = scopes
                    }
                })
    }

    private fun mapToPackageSet(filePatternsToIsGenerated: TreeMap<String, Boolean>): PackageSet {
        if (filePatternsToIsGenerated.isEmpty()) {
            return InvalidPackageSet("")
        }

        val generatedFiles = ArrayList<FilePatternPackageSet>();
        filePatternsToIsGenerated.forEach { (filePattern, isGenerated) ->
            if (isGenerated) {
                generatedFiles.add(FilePatternPackageSet(null, filePattern))
            }
        }

        val nonGeneratedFiles = ArrayList<FilePatternPackageSet>();
        filePatternsToIsGenerated.forEach { (filePattern, isGenerated) ->
            if (!isGenerated) {
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
        FilenameIndex.processFilesByName(
                GITATTRIBUTES_FILE_NAME, //
                false, //
                false, //
                object : Processor<PsiFileSystemItem> {
                    override fun process(file: PsiFileSystemItem): Boolean {
                        for (line in file.text.split("\n")) {
                            if (!shouldReadLine(line)) {
                                continue
                            }

                            val relativePath = line.split(" ")[0]
                            val srcPath = replaceAtSymbol(
                                    file.virtualFile.path //
                                            .removePrefix(project.basePath!! + "/")
                                            .removeSuffix(GITATTRIBUTES_FILE_NAME) + relativePath
                            )
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
