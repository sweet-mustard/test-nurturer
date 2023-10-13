package com.github.wimdeblauwe.testnurturer

import com.github.wimdeblauwe.testnurturer.MyBundle.message
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil

class CodeInspector : AbstractBaseJavaLocalInspectionTool() {
    private val myQuickFix = ReplaceWithEqualsQuickFix()

    /**
     * This method is overridden to provide a custom visitor
     * that inspects expressions with relational operators '==' and '!='.
     * The visitor must not be recursive and must be thread-safe.
     *
     * @param holder     object for the visitor to register problems found
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return non-null visitor for this inspection
     * @see JavaElementVisitor
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            /**
             * Evaluate binary PSI expressions to see if they contain relational operators '==' and '!=',
             * AND they are of String type.
             * The evaluation ignores expressions comparing an object to null.
             * IF these criteria are met, register the problem in the ProblemsHolder.
             *
             * @param expression The binary expression to be evaluated.
             */
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                super.visitBinaryExpression(expression)
                val opSign = expression.operationTokenType
                if (opSign === JavaTokenType.EQEQ || opSign === JavaTokenType.NE) {
                    // The binary expression is the correct type for this inspection
                    val lOperand = expression.lOperand
                    val rOperand = expression.rOperand
                    if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
                        return
                    }
                    // Nothing is compared to null, now check the types being compared
                    if (isStringType(lOperand) || isStringType(rOperand)) {
                        // Identified an expression with potential problems, register problem with the quick fix object
                        holder.registerProblem(
                            expression,
                            message("inspection.comparing.string.references.problem.descriptor"),
                            myQuickFix
                        )
                    }
                }
            }

            private fun isStringType(operand: PsiExpression): Boolean {
                val psiClass = PsiTypesUtil.getPsiClass(operand.type) ?: return false
                return "java.lang.String" == psiClass.qualifiedName
            }

            private fun isNullLiteral(expression: PsiExpression): Boolean {
                return expression is PsiLiteralExpression &&
                        expression.value == null
            }
        }
    }

    /**
     * This class provides a solution to inspection problem expressions by manipulating the PSI tree to use 'a.equals(b)'
     * instead of '==' or '!='.
     */
    private class ReplaceWithEqualsQuickFix : LocalQuickFix {
        /**
         * Returns a partially localized string for the quick fix intention.
         * Used by the test code for this plugin.
         *
         * @return Quick fix short name.
         */
        override fun getName(): String {
            return message("inspection.comparing.string.references.use.quickfix")
        }

        /**
         * This method manipulates the PSI tree to replace 'a==b' with 'a.equals(b)' or 'a!=b' with '!a.equals(b)'.
         *
         * @param project    The project that contains the file being edited.
         * @param descriptor A problem found by this inspection.
         */
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binaryExpression = descriptor.psiElement as PsiBinaryExpression
            val opSign = binaryExpression.operationTokenType
            val lExpr = binaryExpression.lOperand
            val rExpr = binaryExpression.rOperand ?: return
            val factory = JavaPsiFacade.getInstance(project).elementFactory
            val equalsCall = factory.createExpressionFromText("a.equals(b)", null) as PsiMethodCallExpression
            val qualifierExpression = equalsCall.methodExpression.qualifierExpression!!
            qualifierExpression.replace(lExpr)
            equalsCall.argumentList.expressions[0].replace(rExpr)
            val result = binaryExpression.replace(equalsCall) as PsiExpression
            if (opSign === JavaTokenType.NE) {
                val negation = factory.createExpressionFromText("!a", null) as PsiPrefixExpression
                val operand = negation.operand!!
                operand.replace(result)
                result.replace(negation)
            }
        }

        override fun getFamilyName(): String {
            return name
        }
    }
}
