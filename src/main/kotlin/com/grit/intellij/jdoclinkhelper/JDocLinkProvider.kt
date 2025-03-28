package com.grit.intellij.jdoclinkhelper

import com.intellij.ide.actions.DumbAwareCopyPathProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

class JDocLinkProvider : DumbAwareCopyPathProvider() {

    override fun getQualifiedName(
        project: Project,
        elements: List<PsiElement>,
        editor: Editor?,
        dataContext: DataContext
    ): String? {
        val refs = elements
            .flatMap { linkTo(it) }
            .ifEmpty { CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)?.mapNotNull { getPathToElement(project, it, editor) } }
            .orEmpty()

        return if (refs.isNotEmpty()) refs.joinToString("\n") else null
    }

    private fun linkTo(element: PsiElement): List<String> {
        val modulename = ModuleUtil.findModuleForPsiElement(element)?.name ?: ""
        val containerNames = outerScope(element)
        return containerNames?.map { "<jdoc://$modulename/$it>" } ?: listOf()
    }

    /** Finds the enclosing Java class or method. If a file is selected in the project tree, returns all classes in the file. */
    private fun outerScope(element: PsiElement?): List<String>? {
        return when (element) {
            null -> null
            is PsiJavaFile -> element.classes.mapNotNull { it.qualifiedName }
            is PsiClass -> element.qualifiedName?.let { listOf(it) }
            is PsiMethod ->
                // todo support method links with full param signature. Examples here: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html
                outerScope(PsiTreeUtil.getParentOfType(element, PsiClass::class.java))
            else -> outerScope(PsiTreeUtil.getParentOfType(element, PsiMethod::class.java))
        }
    }
}
