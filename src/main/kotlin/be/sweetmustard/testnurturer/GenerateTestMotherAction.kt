package be.sweetmustard.testnurturer

import be.sweetmustard.testnurturer.GenerateTestMotherAction.SelectionItemType.*
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil.findChildOfType
import com.intellij.psi.util.PsiTreeUtil.findChildrenOfType
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil.setModifierProperty
import com.intellij.psi.util.parentOfType
import com.intellij.ui.ColoredListCellRenderer
import java.util.function.Consumer
import javax.swing.Icon
import javax.swing.JList
import javax.swing.ListSelectionModel


class GenerateTestMotherAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val currentProject: Project = event.project!!
        val selectedElement: PsiElement? = event.getData(CommonDataKeys.PSI_ELEMENT)

        if (selectedElement == null) {
            val title: String = event.presentation.description
            Messages.showMessageDialog(
                currentProject,
                "no class selected :(",
                title,
                Messages.getErrorIcon()
            )
            return
        }

        val selectedClass: PsiClass = if (selectedElement is PsiClass) {
            selectedElement
        } else {
            selectedElement.parentOfType<PsiClass>()!!
        }

        showCreateUpdateOrJumpDialog(currentProject, selectedClass, event)
    }

    override fun update(event: AnActionEvent) {
        val selectedElement: PsiElement? = event.getData(CommonDataKeys.PSI_ELEMENT)

        if (selectedElement == null) {
            event.presentation.isEnabled = false
        } else {
            // The action should not be visible if the current class is already a test mother
            if (selectedElement.containingFile != null && selectedElement.containingFile.name.endsWith(
                    "Mother.java"
                )
            ) {
                event.presentation.isVisible = false
            } else {
                event.presentation.isVisible = true
                event.presentation.isEnabled = true
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun showCreateUpdateOrJumpDialog(
        currentProject: Project,
        selectedClass: PsiClass,
        event: AnActionEvent
    ) {
        val motherClass = TestMotherHelper.getMotherForClass(selectedClass)
        val items = if (motherClass != null) {
            listOf(
                SelectionItem(JUMP, "Jump to " + motherClass.name, motherClass),
                SelectionItem(UPDATE, "Update " + motherClass.name, null)
            )
        } else {
            listOf(SelectionItem(CREATE, "Create new Test Mother...", null))
        }
        JBPopupFactory.getInstance().createPopupChooserBuilder(items)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setTitle("Choose Test Mother for " + selectedClass.name)
            .setRenderer(object : ColoredListCellRenderer<SelectionItem>() {
                override fun customizeCellRenderer(
                    list: JList<out SelectionItem>,
                    value: SelectionItem,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    icon = value.getIcon()
                    append(value.title)
                }
            })
            .setItemChosenCallback {
                if (it.type == JUMP) {
                    it.clazz!!.navigate(true)
                } else {
                    WriteCommandAction.runWriteCommandAction(
                        currentProject,
                        it.title,
                        "",
                        {
                            generateTestMother(it.type, currentProject, selectedClass, event)
                        })
                }
            }
            .createPopup()
            .showInBestPositionFor(event.dataContext)
    }

    private fun generateTestMother(
        type: SelectionItemType,
        currentProject: Project,
        selectedClass: PsiClass,
        event: AnActionEvent
    ) {
        if (type == CREATE) {
            getTestSourcesRoot(selectedClass, event) { testSourcesRoot ->
                WriteCommandAction.runWriteCommandAction(
                    currentProject,
                    "Generate Test Mother",
                    "",
                    {
                        generateTestMother(selectedClass, testSourcesRoot, currentProject)
                    })
            }
        } else if (type == UPDATE) {
            // If we update, we don't show the dialog to select a test sources root as we
            // know where the current test mother is.
            val testSourcesRoot: VirtualFile =
                TestMotherHelper.getTestSourceRootOfMother(selectedClass)!!
            WriteCommandAction.runWriteCommandAction(
                currentProject,
                "Update Test Mother",
                "",
                {
                    generateTestMother(selectedClass, testSourcesRoot, currentProject)
                })
        }
    }

    private fun generateTestMother(
        selectedClass: PsiClass,
        testSourcesRoot: VirtualFile,
        currentProject: Project
    ) {
        val (motherFile: PsiFile, shouldCreateNewFile) = getOrCreateMotherFile(
            selectedClass,
            testSourcesRoot,
            currentProject
        )

        val motherClass = findChildOfType(motherFile, PsiClass::class.java)!!

        val elementFactory = JavaPsiFacade.getInstance(currentProject).elementFactory
        val existingBuilder = findChildrenOfType(motherClass, PsiClass::class.java)
            .filter { it.name == "Builder" }
            .firstOrNull()
        if (existingBuilder != null) {
            populateClass(elementFactory, selectedClass, motherClass, existingBuilder)
        } else {
            val builderClass = createBuilderInnerClass(elementFactory)
            populateClass(elementFactory, selectedClass, motherClass, builderClass)
            motherClass.add(builderClass)
        }

        formatTestMother(currentProject, motherFile)

        if (shouldCreateNewFile) {
            val testSourceRootDirectory =
                PsiManager.getInstance(currentProject).findDirectory(testSourcesRoot)!!

            val directory =
                createPackageDirectoriesIfNeeded(testSourceRootDirectory, selectedClass)

            val addedMotherFile: PsiFile = directory.add(motherFile) as PsiFile
            addedMotherFile.navigate(true)
        } else {
            motherFile.navigate(true)
        }

    }

    private fun createPackageDirectoriesIfNeeded(
        testSourceRootDirectory: PsiDirectory,
        selectedClass: PsiClass
    ): PsiDirectory {
        val packageName = TestMotherHelper.getPackageName(selectedClass)
        var directory = testSourceRootDirectory
        val packageNameParts = packageName.split(".").toList()
        for (packageNamePart in packageNameParts) {
            var subdirectory = directory.findSubdirectory(packageNamePart)
            if (subdirectory == null) {
                subdirectory = directory.createSubdirectory(packageNamePart)
            }
            directory = subdirectory
        }
        return directory
    }

    private fun getOrCreateMotherFile(
        selectedClass: PsiClass,
        testSourcesRoot: VirtualFile,
        currentProject: Project
    ): Pair<PsiFile, Boolean> {
        val packageName = TestMotherHelper.getPackageName(selectedClass)
        var motherFile = TestMotherHelper.getCorrespondingMother(testSourcesRoot, selectedClass)

        var shouldCreateNewFile = false
        if (motherFile == null) {
            shouldCreateNewFile = true
            val builder = StringBuilder()
            if (packageName.isNotEmpty()) {
                builder.appendLine("package ${packageName};")
                builder.appendLine("")
            }
            builder.appendLine("public final class ${selectedClass.name}Mother {}")
            motherFile = PsiFileFactory.getInstance(currentProject)
                .createFileFromText(
                    TestMotherHelper.getMotherFileName(selectedClass),
                    JavaFileType.INSTANCE,
                    builder.toString()
                )
        }
        return Pair(motherFile, shouldCreateNewFile)
    }

    private fun getTestSourcesRoot(
        selectedClass: PsiClass,
        event: AnActionEvent,
        callback: Consumer<VirtualFile>
    ) {
        val testSourceRoots = TestMotherHelper.getPossibleTestSourceRoots(selectedClass)
        if (testSourceRoots.size != 1) {
            allowUserToSelectTestSourceRoot(testSourceRoots, selectedClass, callback, event)
        } else {
            callback.accept(testSourceRoots.get(0))
        }
    }

    private fun allowUserToSelectTestSourceRoot(
        testSourceRoots: List<VirtualFile>,
        selectedClass: PsiClass,
        callback: Consumer<VirtualFile>,
        event: AnActionEvent
    ) {
        JBPopupFactory.getInstance().createPopupChooserBuilder(testSourceRoots)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setTitle("Choose test sources root for " + selectedClass.name + " Test Mother generation")
            .setRenderer(object : ColoredListCellRenderer<VirtualFile>() {
                override fun customizeCellRenderer(
                    list: JList<out VirtualFile>,
                    value: VirtualFile,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    icon = AllIcons.Modules.TestRoot
                    append(value.presentableName)
                }
            })
            .setItemChosenCallback {
                callback.accept(it)
            }
            .createPopup()
            .showInBestPositionFor(event.dataContext)
    }

    private fun formatTestMother(
        currentProject: Project,
        motherFile: PsiFile
    ) {
        val codeStyleManager = CodeStyleManager.getInstance(currentProject)
        codeStyleManager.reformat(motherFile)
    }

    private fun populateClass(
        elementFactory: PsiElementFactory,
        selectedClass: PsiClass,
        motherClass: PsiClass,
        builderClass: PsiClass
    ) {
        addBuilderEntryPointMethod(elementFactory, selectedClass, motherClass, builderClass)
        addFieldsAndMethodsToBuilderInnerClass(selectedClass, elementFactory, builderClass)
    }

    private fun addFieldsAndMethodsToBuilderInnerClass(
        selectedClass: PsiClass,
        elementFactory: PsiElementFactory,
        builderInnerClass: PsiClass
    ) {
        val fieldsOfSelectedClass = selectedClass.allFields
        for (psiField in fieldsOfSelectedClass) {

            val fieldName = psiField.name

            val existingField = findChildrenOfType(builderInnerClass, PsiField::class.java)
                .filter { it.name == fieldName }
                .firstOrNull()

            if (existingField == null) {
                val builderInnerClassField =
                    elementFactory.createField(psiField.name, psiField.type)
                builderInnerClass.add(builderInnerClassField)

                val fieldMethod = elementFactory.createMethod(
                    fieldName,
                    PsiTypesUtil.getClassType(builderInnerClass)
                )
                val parameter = elementFactory.createParameter(fieldName, psiField.type)
                fieldMethod.parameterList.add(parameter)

                fieldMethod.body!!.add(
                    elementFactory.createStatementFromText(
                        "this.$fieldName = $fieldName;",
                        fieldMethod
                    )
                )
                fieldMethod.body!!.add(
                    elementFactory.createStatementFromText(
                        "return this;",
                        fieldMethod
                    )
                )

                builderInnerClass.add(fieldMethod)
            }
        }

        val existingReturnMethod = findChildrenOfType(builderInnerClass, PsiMethod::class.java)
            .filter { it.name == "build" }
            .firstOrNull()

        existingReturnMethod?.delete()

        val parameterList = fieldsOfSelectedClass.map { it.name }.joinToString(separator = ",")

        val returnMethod =
            elementFactory.createMethod("build", PsiTypesUtil.getClassType(selectedClass))
        returnMethod.body!!.add(
            elementFactory.createStatementFromText(
                "return new ${selectedClass.name} ( $parameterList) ;",
                returnMethod
            )
        )
        builderInnerClass.add(returnMethod)
    }

    private fun addBuilderEntryPointMethod(
        elementFactory: PsiElementFactory,
        selectedClass: PsiClass,
        motherClass: PsiClass,
        builderInnerClass: PsiClass
    ) {
        val className = selectedClass.name!!
        val builderEntryPointMethodName = className.replaceFirstChar { it.lowercase() }

        val existingMethod = findChildrenOfType(motherClass, PsiMethod::class.java)
            .filter { it.name == builderEntryPointMethodName }
            .firstOrNull()

        if (existingMethod == null) {
            val builderMethod: PsiMethod = elementFactory.createMethod(
                builderEntryPointMethodName,
                PsiTypesUtil.getClassType(builderInnerClass)
            )
            setModifierProperty(builderMethod, PsiModifier.STATIC, true)
            val body = builderMethod.body!!
            body.add(elementFactory.createStatementFromText("return new Builder();", builderMethod))
            motherClass.add(builderMethod)
        }
    }

    private fun createBuilderInnerClass(
        elementFactory: PsiElementFactory
    ): PsiClass {
        val builderInnerClass: PsiClass = elementFactory.createClass("Builder")
        setModifierProperty(builderInnerClass, PsiModifier.STATIC, true)
        setModifierProperty(builderInnerClass, PsiModifier.FINAL, true)
        return builderInnerClass
    }

    data class SelectionItem(val type: SelectionItemType, val title: String, val clazz: PsiClass?) {

        fun getIcon(): Icon {
            return if (clazz == null) {
                AllIcons.Actions.IntentionBulb
            } else {
                AllIcons.Nodes.Class
            }
        }
    }

    enum class SelectionItemType {
        CREATE,
        UPDATE,
        JUMP
    }
}
