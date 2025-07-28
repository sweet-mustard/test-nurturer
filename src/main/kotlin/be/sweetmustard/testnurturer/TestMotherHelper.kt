package be.sweetmustard.testnurturer

import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.util.*

class TestMotherHelper {
    companion object {
        fun getCorrespondingMother(
            testSourceRoot: VirtualFile,
            productionClass: PsiClass
        ): PsiFile? {
            val motherFileName = getMotherFileName(productionClass)
            val packageName: String = getPackageName(productionClass)
            val motherVirtualFile =
                testSourceRoot.findFileByRelativePath(
                    packageName.replace(
                        ".",
                        "/"
                    ) + "/" + motherFileName
                )
            var motherFile: PsiFile? = null
            if (motherVirtualFile != null) {
                motherFile =
                    PsiManager.getInstance(productionClass.project).findFile(motherVirtualFile)
            }
            return motherFile
        }

        fun getPackageName(selectedClass: PsiClass): String {
            val selectedClassFile = selectedClass.containingFile
            var packageName = ""
            if (selectedClassFile is PsiJavaFile) {
                packageName = selectedClassFile.packageName
            }
            return packageName
        }

        fun getMotherFileName(productionClass: PsiClass): String {
            return productionClass.name + "Mother.java"
        }

        fun getPossibleTestSourceRoots(productionClass: PsiClass): List<VirtualFile> {
            val module =
                ProjectRootManager.getInstance(productionClass.project).fileIndex
                    .getModuleForFile(productionClass.containingFile.virtualFile)
            if (module == null) {
                return Collections.emptyList()
            }

            // First search for test sources in the own module
            val result = ModuleRootManager.getInstance(module)
                .getSourceRoots(JavaSourceRootType.TEST_SOURCE)
            if (result.isEmpty()) {
                // If nothing is found, search in the other modules as well (Needed for Gradle Spring Boot projects)
                return productionClass.project.modules.flatMap { module ->
                    ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE)
                }
            } else {
                return result
            }
        }

        fun getMotherForClass(currentClass: PsiClass): PsiClass? {
            val testSourceRoots = getPossibleTestSourceRoots(currentClass)
            for (testSourceRoot in testSourceRoots) {
                val correspondingMother = getCorrespondingMother(testSourceRoot, currentClass)
                if (correspondingMother != null) {
                    return PsiTreeUtil.findChildOfType(correspondingMother, PsiClass::class.java)
                }
            }
            return null
        }

        fun getTestSourceRootOfMother(currentClass: PsiClass): VirtualFile? {
            val testSourceRoots = getPossibleTestSourceRoots(currentClass)
            for (testSourceRoot in testSourceRoots) {
                val correspondingMother = getCorrespondingMother(testSourceRoot, currentClass)
                if (correspondingMother != null) {
                    return testSourceRoot
                }
            }
            return null
        }
    }

}
