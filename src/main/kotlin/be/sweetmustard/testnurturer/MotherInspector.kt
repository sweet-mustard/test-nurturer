package be.sweetmustard.testnurturer

import com.google.common.collect.Sets
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class MotherInspector : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitClass(currentClass: PsiClass) {
                super.visitClass(currentClass)
                val motherClass = getMotherForClass(currentClass)
                if (motherClass == null) {
                    // There is no corresponding test mother for the current class
                    return
                }

                val productionClass = currentClass;
                val productionClassFields = productionClass.allFields.map {
                    Field(it.name, it.type, it)
                }.toSet()

                val innerBuilderClass = motherClass.findInnerClassByName("Builder", false)
                if (innerBuilderClass == null) {
                    // There is no inner `Builder` class, we can't check the fields
                    return
                }

                val innerBuilderFields = innerBuilderClass.allFields.map {
                    Field(it.name, it.type, it)
                }.toSet()

                val extraFieldsInProductionClass =
                    Sets.difference(productionClassFields, innerBuilderFields)
                if (!extraFieldsInProductionClass.isEmpty()) {
                    for (field in extraFieldsInProductionClass) {
                        holder.registerProblem(
                            field.field,
                            MyBundle.message("inspection.mother.missing.field.in.mother"),
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }
    }

    private fun getMotherForClass(selectedClass: PsiClass): PsiClass? {
        val motherName = selectedClass.name + "Mother.java";
        val mother = selectedClass.containingFile.containingDirectory.findFile(motherName)
        if (mother != null) {
            return PsiTreeUtil.findChildOfType(mother, PsiClass::class.java)
        }
        return null;
    }

    /**
     * This class represents a field in a class to be able to compare fields from different classes
     * with each other. For that reason, the equals() method only takes `name` and `type` into account.
     */
    data class Field(
        var name: String,
        var type: PsiType,
        var field: PsiField
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Field

            if (name != other.name) return false
            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }
    }

}


