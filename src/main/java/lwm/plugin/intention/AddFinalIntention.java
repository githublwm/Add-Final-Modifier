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
import java.util.Collection;

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
        if (!(file instanceof PsiJavaFile)) {
            return false;
        }
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return false;
        }

        if (element instanceof PsiVariable) {
            return true;
        }

        return PsiTreeUtil.getParentOfType(element, PsiMethod.class) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return;
        }

        PsiVariable specificVariable = PsiTreeUtil.getParentOfType(element, PsiVariable.class, false);
        if (specificVariable != null) {
            addFinalModifierIfNotPresent(specificVariable);
            return; 
        }

        final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (containingMethod != null) {
            // Add final to parameters
            final PsiParameterList parameterList = containingMethod.getParameterList();
            for (PsiParameter parameter : parameterList.getParameters()) {
                addFinalModifierIfNotPresent(parameter);
            }

            // Add final to local variables
            final PsiCodeBlock methodBody = containingMethod.getBody();
            if (methodBody != null) {
                Collection<PsiLocalVariable> localVariables = PsiTreeUtil.collectElementsOfType(methodBody, PsiLocalVariable.class);
                for (PsiLocalVariable localVariable : localVariables) {
                    addFinalModifierIfNotPresent(localVariable);
                }
            }
        }
    }

    private void addFinalModifierIfNotPresent(PsiModifierListOwner element) {
        if (element != null) {
            PsiModifierList modifierList = element.getModifierList();
            if (modifierList != null && !modifierList.hasExplicitModifier(PsiModifier.FINAL)) {
                PsiUtil.setModifierProperty(element, PsiModifier.FINAL, true);
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
