package be.sweetmustard.testnurturer

import com.google.common.collect.Sets
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType

class MotherInspector : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitClass(currentClass: PsiClass) {
                super.visitClass(currentClass)
                val motherClass = TestMotherHelper.getMotherForClass(currentClass)
                    ?: // There is no corresponding test mother for the current class
                    return

                val productionClassFields: Set<Field> =
                    if (currentClass.recordComponents.isNotEmpty()) {
                        currentClass.recordComponents.map {
                            Field(it.name, it.type, it)
                        }.toSet()
                    } else {
                        currentClass.allFields
                            .filter {
                                it.modifierList == null
                                        || !it.modifierList!!.hasModifierProperty(PsiModifier.STATIC)
                            }
                            .map {
                                val psiElement: PsiElement =
                                    if (it.containingClass?.equals(currentClass) == true) {
                                        it
                                    } else {
                                        // When there is a field missing that is from a parent class,
                                        // we can't highlight the field itself since it is not present in the current class
                                        // So we highlight the name of the class at the top.
                                        // Using `productionClass` would highlight the complete file, which would be very annoying.
                                        currentClass.childrenOfType<PsiIdentifier>().first()
                                    }
                                Field(it.name, it.type, psiElement)
                            }.toSet()
                    }
                val innerBuilderClass = motherClass.findInnerClassByName("Builder", false)
                    ?: // There is no inner `Builder` class, we can't check the fields
                    return

                val innerBuilderFields = innerBuilderClass.allFields.map {
                    Field(it.name, it.type, it)
                }.toSet()

                val extraFieldsInProductionClass =
                    Sets.difference(productionClassFields, innerBuilderFields)
                if (!extraFieldsInProductionClass.isEmpty()) {
                    for (field in extraFieldsInProductionClass) {
                        holder.registerProblem(
                            field.elementToHighlight,
                            MyBundle.message(
                                "inspection.mother.missing.field.in.mother",
                                field.name
                            ),
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }
    }

    /**
     * This class represents a field in a class to be able to compare fields from different classes
     * with each other. For that reason, the equals() method only takes `name` and `type` into account.
     */
    data class Field(
        var name: String,
        var type: PsiType,
        var elementToHighlight: PsiElement
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


