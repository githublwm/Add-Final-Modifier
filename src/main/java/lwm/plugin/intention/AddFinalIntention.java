package lwm.plugin.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author longwm
 */
public class AddFinalIntention implements IntentionAction {
    @Override
    public @IntentionName @NotNull String getText() {
        return "Add final modifier(添加final修饰)";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Add final modifier";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file instanceof PsiJavaFile;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());

        final PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method != null) {
            final PsiParameterList parameterList = method.getParameterList();
            Arrays.stream(parameterList.getParameters())
                    .forEach(psiParameter -> PsiUtil.setModifierProperty(psiParameter, PsiModifier.FINAL, true));
            final PsiCodeBlock methodBody = method.getBody();
            if (methodBody != null) {
                final PsiStatement[] statements = methodBody.getStatements();
                Arrays.stream(statements)
                        .filter(PsiDeclarationStatement.class::isInstance)
                        .map(psiStatement -> ((PsiDeclarationStatement) psiStatement).getDeclaredElements())
                        .flatMap(Arrays::stream)
                        .filter(PsiLocalVariable.class::isInstance)
                        .forEach(psiElement -> PsiUtil.setModifierProperty((PsiLocalVariable) psiElement, PsiModifier.FINAL, true));
            }
        }

        final PsiVariable psiVariable = PsiTreeUtil.getParentOfType(element, PsiVariable.class);
        if (psiVariable != null) {
            PsiUtil.setModifierProperty(psiVariable, PsiModifier.FINAL, true);
        }

    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
