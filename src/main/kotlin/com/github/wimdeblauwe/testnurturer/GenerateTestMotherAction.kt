package com.github.wimdeblauwe.testnurturer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement


class GenerateTestMotherAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // Using the event, create and show a dialog
        val currentProject: Project? = event.getProject()
        val message: StringBuilder = StringBuilder(event.getPresentation().getText() + " Selected!")
        // If an element is selected in the editor, add info about it.
        val selectedElement: PsiElement? = event.getData(CommonDataKeys.PSI_ELEMENT)
        if (selectedElement != null) {
            message.append("\nSelected Element: ").append(selectedElement)
        }
        val title: String = event.getPresentation().getDescription()
        Messages.showMessageDialog(
                currentProject,
                message.toString(),
                title,
                Messages.getInformationIcon())
    }
}
