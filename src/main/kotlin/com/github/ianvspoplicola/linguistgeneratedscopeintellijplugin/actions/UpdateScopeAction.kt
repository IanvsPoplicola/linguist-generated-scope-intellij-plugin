package com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.actions

import com.github.ianvspoplicola.linguistgeneratedscopeintellijplugin.utils.FilePatternMapper.Companion.mapGitPatternToIntellijPattern
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.scope.packageSet.*
import com.intellij.ui.FileColorManager
import com.intellij.util.Processor
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

private val LOGGER = logger<UpdateScopeAction>()

const val SCOPE_DEFAULT_COLOUR = "630330" // https://htmlcolorcodes.com/colors/tyrian-purple/
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

                val fileColorManager = FileColorManager.getInstance(project)
                if (fileColorManager.getScopeColor(SCOPE_NAME_GENERATED) == null) {
                    fileColorManager.addScopeColor(SCOPE_NAME_GENERATED, SCOPE_DEFAULT_COLOUR, false)
                }

                // update project view
                ProjectView.getInstance(project).refresh()
                ProjectView.getInstance(project).currentProjectViewPane?.updateFromRoot(true)

                // update open tabs
                val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
                for (openFile in editorManagerEx.openFiles) {
                    editorManagerEx.updateFilePresentation(openFile!!)
                }
            }
        })
    }

    companion object {
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
                    GlobalSearchScope.projectScope(project), //
                    object : Processor<VirtualFile> {
                        override fun process(file: VirtualFile): Boolean {
                            for (line in LoadTextUtil.loadText(file).split("\n")) {
                                if (!shouldReadLine(line)) {
                                    continue
                                }
                                var relativePath = line.split(" ")[0]

                                val inverted = relativePath.startsWith("!")
                                relativePath = relativePath.removePrefix("!")

                                val intellijRelativePaths = mapGitPatternToIntellijPattern(relativePath)
                                val currentDirectoryRelativePath = getRelativeDirectoryPath(project, file)
                                val srcPaths = intellijRelativePaths.mapNotNull {
                                    var path = it
                                    if (currentDirectoryRelativePath.endsWith("/") && path.startsWith("/")) {
                                        path = path.removePrefix("/")
                                    }
                                    replaceAtSymbol(currentDirectoryRelativePath + path)
                                }.toSet()
                                srcPaths.forEach { srcPath ->
                                    if (filePatternsToIsGenerated[srcPath] != false) { // if there's a conflict, avoid false positives
                                        filePatternsToIsGenerated[srcPath] = line.endsWith("true") != inverted
                                    }
                                }
                            }
                            return true
                        }
                    } //
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

        private fun getRelativeDirectoryPath(project: Project, file: VirtualFile): String {
            val sourceRoot = ProjectFileIndex.getInstance(project).getSourceRootForFile(file)
            var currentDirectoryRelativePath = file.path
            if (sourceRoot != null && VfsUtilCore.getRelativePath(file, sourceRoot) != null) {
                currentDirectoryRelativePath = VfsUtilCore.getRelativePath(file, sourceRoot)!!
            }
            return currentDirectoryRelativePath.removeSuffix(GITATTRIBUTES_FILE_NAME)
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

}
