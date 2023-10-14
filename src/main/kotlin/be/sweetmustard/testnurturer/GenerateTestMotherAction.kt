package be.sweetmustard.testnurturer

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
import com.intellij.psi.util.PsiTreeUtil.findChildOfType
import com.intellij.psi.util.PsiTreeUtil.findChildrenOfType
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil.setModifierProperty
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
        if (motherFile == null) {
            motherFile = PsiFileFactory.getInstance(currentProject)
                    .createFileFromText(motherFileName, JavaFileType.INSTANCE,
                            "public class ${selectedClass.name}Mother {}")
        }

        val motherClass = findChildOfType(motherFile, PsiClass::class.java)!!

        val existingBuilder = findChildrenOfType(motherClass, PsiClass::class.java)
                .filter { it.name == "Builder" }
                .firstOrNull()
        if (existingBuilder != null) {
            populateClass(elementFactory, selectedClass, motherClass, existingBuilder)
        } else {
            val builderClass = createBuilderInnerClass(elementFactory, motherClass)
            populateClass(elementFactory, selectedClass, motherClass, builderClass)
            motherClass.add(builderClass)
        }


        val codeStyleManager = CodeStyleManager.getInstance(currentProject)
        codeStyleManager.reformat(motherFile)

        if (directoryOfClass.findFile(motherFileName) == null) {
            directoryOfClass.createSubdirectory("test")
            val subdir = directoryOfClass.subdirectories[0]
            subdir.add(motherFile)
        }
    }

    private fun populateClass(elementFactory: PsiElementFactory, selectedClass: PsiClass, motherClass: PsiClass, builderClass: PsiClass) {
        addBuilderEntryPointMethod(elementFactory, selectedClass, motherClass, builderClass)
        addFieldsAndMethodsToBuilderInnerClass(selectedClass, elementFactory, builderClass)
    }

    private fun addFieldsAndMethodsToBuilderInnerClass(selectedClass: PsiClass, elementFactory: PsiElementFactory, builderInnerClass: PsiClass) {
        val fieldsOfSelectedClass = PsiTreeUtil.findChildrenOfAnyType(selectedClass, PsiField::class.java)
        for (psiField in fieldsOfSelectedClass) {

            val fieldName = psiField.name

            val existingField = findChildrenOfType(builderInnerClass, PsiField::class.java)
                    .filter { it.name == fieldName }
                    .firstOrNull()

            if (existingField == null) {
                val builderInnerClassField = elementFactory.createField(psiField.name, psiField.type)
                builderInnerClass.add(builderInnerClassField)

                val fieldMethod = elementFactory.createMethod(fieldName, PsiTypesUtil.getClassType(builderInnerClass))
                val parameter = elementFactory.createParameter(fieldName, psiField.type)
                fieldMethod.parameterList.add(parameter)

                fieldMethod.body!!.add(elementFactory.createStatementFromText("this.$fieldName = $fieldName;", fieldMethod))
                fieldMethod.body!!.add(elementFactory.createStatementFromText("return this;", fieldMethod))

                builderInnerClass.add(fieldMethod)
            }
        }

        val existingReturnMethod = findChildrenOfType(builderInnerClass, PsiMethod::class.java)
                .filter { it.name == "build" }
                .firstOrNull()

        existingReturnMethod?.delete()

        val parameterList = fieldsOfSelectedClass.map { it.name }.joinToString(separator = ",")

        val returnMethod = elementFactory.createMethod("build", PsiTypesUtil.getClassType(selectedClass))
        returnMethod.body!!.add(elementFactory.createStatementFromText("return new ${selectedClass.name} ( $parameterList) ;", returnMethod))
        builderInnerClass.add(returnMethod)
    }

    private fun addBuilderEntryPointMethod(elementFactory: PsiElementFactory, selectedClass: PsiClass, motherClass: PsiClass, builderInnerClass: PsiClass) {
        val className = selectedClass.name!!
        val builderEntryPointMethodName = className.replaceFirstChar { it.lowercase() }

        val existingMethod = findChildrenOfType(motherClass, PsiMethod::class.java)
                .filter { it.name == builderEntryPointMethodName }
                .firstOrNull()

        if (existingMethod == null) {
            val builderMethod: PsiMethod = elementFactory.createMethod(builderEntryPointMethodName, PsiTypesUtil.getClassType(builderInnerClass));
            setModifierProperty(builderMethod, PsiModifier.STATIC, true)
            val body = builderMethod.body!!
            body.add(elementFactory.createStatementFromText("return new Builder();", builderMethod))
            motherClass.add(builderMethod)
        }
    }

    private fun createBuilderInnerClass(elementFactory: PsiElementFactory, motherClass: PsiClass): PsiClass {
        val builderInnerClass: PsiClass = elementFactory.createClass("Builder")
        setModifierProperty(builderInnerClass, PsiModifier.STATIC, true);
        setModifierProperty(builderInnerClass, PsiModifier.FINAL, true)
        return builderInnerClass
    }
}
