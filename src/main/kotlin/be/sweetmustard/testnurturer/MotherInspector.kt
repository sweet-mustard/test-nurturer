package be.sweetmustard.testnurturer

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class MotherInspector : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitClass(aClass: PsiClass) {
                super.visitClass(aClass)
                val motherClass = hasMotherForClass(aClass)
                if (motherClass != null) {
                    val innerBuilderClass = motherClass.findInnerClassByName("Builder", false)
                    val selectClassFields = aClass.allFields.map {
                        Field(it.name, it.type)
                    }.toTypedArray()
                    

                    val innerBuilderFields = innerBuilderClass?.allFields?.map {
                        Field(it.name, it.type)
                    }?.toTypedArray()

                    if (!selectClassFields.contentEquals(innerBuilderFields)) {
                        holder.registerProblem(
                            aClass,
                            "Missing field in mother",
                            ProblemHighlightType.WARNING
                        )

                    }
                }
            }
        }
    }

    private fun hasMotherForClass(selectedClass: PsiClass): PsiClass? {
        val motherName = selectedClass.name + "Mother.java";
        val mother = selectedClass.containingFile.containingDirectory.findFile(motherName)
        if (mother != null) {
            return PsiTreeUtil.findChildOfType(mother, PsiClass::class.java)
        }
        return null;
    }

    data class Field(
        var name: String,
        var type: PsiType
    )

}


