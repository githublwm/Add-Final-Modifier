package lwm.plugin.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author longwm
 */
public class AddFinalIntention implements IntentionAction {
    @Override
    public @IntentionName @NotNull String getText() {
        return "给方法参数和局部变量添加final修饰";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "添加final修饰";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());

        final PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return;
        }
        final PsiParameterList parameterList = method.getParameterList();

        Arrays.stream(parameterList.getParameters())
                .map(PsiModifierListOwner::getModifierList).filter(Objects::nonNull)
                .forEach(psiModifierList -> psiModifierList.setModifierProperty(PsiModifier.FINAL, true));

        final PsiCodeBlock methodBody = method.getBody();
        if (methodBody == null) {
            return;
        }
        final PsiStatement[] statements = methodBody.getStatements();

        Arrays.stream(statements)
                .filter(PsiDeclarationStatement.class::isInstance)
                .map(psiStatement -> ((PsiDeclarationStatement) psiStatement).getDeclaredElements())
                .flatMap(Arrays::stream)
                .filter(PsiLocalVariable.class::isInstance)
                .forEach(psiElement -> {
                    final PsiModifierList modifierList = ((PsiLocalVariable) psiElement).getModifierList();
                    modifierList.setModifierProperty(PsiModifier.FINAL, true);
                });
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
