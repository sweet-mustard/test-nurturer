package com.github.wimdeblauwe.testnurturer

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType


class GenerateTestMotherAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val currentProject: Project = event.getProject()!!
        val selectedElement: PsiElement? = event.getData(CommonDataKeys.PSI_ELEMENT)

        if (selectedElement == null) {
            val title: String = event.getPresentation().getDescription()
            Messages.showMessageDialog(
                    currentProject,
                    "no class selected :(",
                    title,
                    Messages.getErrorIcon())
            return
        }

        val selectedClass: PsiClass = if (selectedElement is PsiClass) {
            selectedElement
        } else {
            selectedElement.parentOfType<PsiClass>()!!
        }

        WriteCommandAction.runWriteCommandAction(currentProject) {
            generateTestMother(currentProject, selectedClass)
        }
    }

    private fun generateTestMother(currentProject: Project, selectedClass: PsiClass) {
        val elementFactory = JavaPsiFacade.getInstance(currentProject).elementFactory

        val directoryOfClass = selectedClass.containingFile.containingDirectory
        val motherFileName = selectedClass.name + "Mother.java"
        var motherFile = directoryOfClass.findFile(motherFileName);
        if (motherFile != null) {
            motherFile.delete()
        }

        motherFile = PsiFileFactory.getInstance(currentProject)
                .createFileFromText(motherFileName, JavaFileType.INSTANCE,
                        "public class ${selectedClass.name}Mother {}")


        val motherClass = PsiTreeUtil.findChildOfType(motherFile, PsiClass::class.java)!!
        val builderInnerClass: PsiClass = createBuilderInnerClass(elementFactory, motherClass)
        addBuilderEntryPointMethod(elementFactory, selectedClass, motherClass, builderInnerClass)
        addFieldsAndMethodsToBuilderInnerClass(selectedClass, elementFactory, builderInnerClass, selectedClass)

        motherClass.add(builderInnerClass)

        val codeStyleManager = CodeStyleManager.getInstance(currentProject)
        codeStyleManager.reformat(motherFile)

        directoryOfClass.add(motherFile)
    }

    private fun addFieldsAndMethodsToBuilderInnerClass(selectedClass: PsiClass, elementFactory: PsiElementFactory, builderInnerClass: PsiClass, selectedClass1: PsiClass) {
        val fieldsOfSelectedClass = PsiTreeUtil.findChildrenOfAnyType(selectedClass, PsiField::class.java)
        for (psiField in fieldsOfSelectedClass) {
            val builderInnerClassField = elementFactory.createField(psiField.name, psiField.type)
            builderInnerClass.add(builderInnerClassField)
        }

        for (psiField in fieldsOfSelectedClass) {
            val fieldName = psiField.name
            val fieldMethod = elementFactory.createMethod(fieldName, PsiTypesUtil.getClassType(builderInnerClass))
            val parameter = elementFactory.createParameter(fieldName, psiField.type)
            fieldMethod.parameterList.add(parameter)

            fieldMethod.body!!.add(elementFactory.createStatementFromText("this.$fieldName = $fieldName;", fieldMethod))
            fieldMethod.body!!.add(elementFactory.createStatementFromText("return this;", fieldMethod))

            builderInnerClass.add(fieldMethod)
        }

        val parameterList = fieldsOfSelectedClass.map { it.name }.joinToString(separator = ",")

        val returnMethod = elementFactory.createMethod("build", PsiTypesUtil.getClassType(selectedClass))
        returnMethod.body!!.add(elementFactory.createStatementFromText("return new ${selectedClass.name} ( $parameterList) ;", returnMethod))
        builderInnerClass.add(returnMethod)


    }

    private fun addBuilderEntryPointMethod(elementFactory: PsiElementFactory, selectedClass: PsiClass, motherClass: PsiClass, builderInnerClass: PsiClass) {
        val className = selectedClass.name!!
        val lowerCasedClassName = className.replaceFirstChar { it.lowercase() }
        val builderMethod: PsiMethod = elementFactory.createMethod(lowerCasedClassName, PsiTypesUtil.getClassType(builderInnerClass));
        PsiUtil.setModifierProperty(builderMethod, PsiModifier.STATIC, true)

        val body = builderMethod.body!!
        body.add(elementFactory.createStatementFromText("return new Builder();", builderMethod))

        motherClass.add(builderMethod)
    }

    private fun createBuilderInnerClass(elementFactory: PsiElementFactory, motherClass: PsiClass): PsiClass {

        val builderInnerClass: PsiClass = elementFactory.createClass("Builder")
        PsiUtil.setModifierProperty(builderInnerClass, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(builderInnerClass, PsiModifier.FINAL, true)
        return builderInnerClass
    }
}
