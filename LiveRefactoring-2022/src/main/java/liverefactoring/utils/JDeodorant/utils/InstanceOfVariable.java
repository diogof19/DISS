package liverefactoring.utils.JDeodorant.utils;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;

public class InstanceOfVariable implements ExpressionInstanceChecker {

    public boolean instanceOf(PsiExpression expression) {
        return (expression instanceof PsiReferenceExpression &&
                ((PsiReferenceExpression) expression).resolve() instanceof PsiVariable);
    }

}
