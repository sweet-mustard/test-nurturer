package com.github.wimdeblauwe.testnurturer

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil


class GenerateTestMotherAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // Using the event, create and show a dialog
        val currentProject: Project = event.getProject()!!
        val message: StringBuilder = StringBuilder(event.getPresentation().getText() + " Selected!")
        // If an element is selected in the editor, add info about it.
        val selectedElement: PsiElement? = event.getData(CommonDataKeys.PSI_ELEMENT)


        if (selectedElement != null) {
            message.append("\nSelected Element: ").append(selectedElement)
        }

        val selectedClass: PsiClass = selectedElement as PsiClass;

        WriteCommandAction.runWriteCommandAction(currentProject) {
            generateTestMother(currentProject, selectedClass)
        }

        val title: String = event.getPresentation().getDescription()
        Messages.showMessageDialog(
                currentProject,
                message.toString(),
                title,
                Messages.getInformationIcon())
    }

    private fun generateTestMother(currentProject: Project, selectedClass: PsiClass) {
        val elementFactory = JavaPsiFacade.getInstance(currentProject).elementFactory

        val motherFile = PsiFileFactory.getInstance(currentProject)
                .createFileFromText(selectedClass.name + "Mother.java", JavaFileType.INSTANCE,
                        "public class ${selectedClass.name}Mother {}")

        val motherClass = PsiTreeUtil.findChildOfType(motherFile, PsiClass::class.java)!!
        val builderInnerClass: PsiClass = addBuilderInnerClass(elementFactory, motherClass)
        addBuilderEntryPointMethod(elementFactory, motherClass, builderInnerClass)
        addFieldsToBuilderInnerClass(selectedClass, elementFactory, builderInnerClass)

        val directoryOfClass = selectedClass.containingFile.containingDirectory
        directoryOfClass.add(motherFile)
    }

    private fun addFieldsToBuilderInnerClass(selectedClass: PsiClass, elementFactory: PsiElementFactory, builderInnerClass: PsiClass) {
        val fieldsOfSelectedClass = PsiTreeUtil.findChildrenOfAnyType(selectedClass, PsiField::class.java)
        for (psiField in fieldsOfSelectedClass) {
            val builderInnerClassField = elementFactory.createField(psiField.name, psiField.type)
            builderInnerClass.add(builderInnerClassField)
        }
    }

    private fun addBuilderEntryPointMethod(elementFactory: PsiElementFactory, motherClass: PsiClass, builderInnerClass: PsiClass) {
        val builderMethod = elementFactory.createMethod(motherClass.name!!, PsiTypesUtil.getClassType(builderInnerClass));
        PsiUtil.setModifierProperty(builderMethod, PsiModifier.STATIC, true)
        motherClass.add(builderMethod)
    }

    private fun addBuilderInnerClass(elementFactory: PsiElementFactory, motherClass: PsiClass): PsiClass {
        val builderInnerClass: PsiClass = elementFactory.createClass("Builder")
        PsiUtil.setModifierProperty(builderInnerClass, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(builderInnerClass, PsiModifier.FINAL, true)
        motherClass.add(builderInnerClass)
        return builderInnerClass
    }
}
