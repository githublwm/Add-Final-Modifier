package lwm.plugin.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

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
    public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
        if (!(file instanceof PsiJavaFile)) {
            return false;
        }
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return false;
        }

        final PsiField field = PsiTreeUtil.getParentOfType(element, PsiField.class, false);
        if (field != null) {
            return true;
        }
        if (element instanceof PsiVariable) {
            return true;
        }
        
        // 检查是否在类名标识符上
        final PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        if (psiClass != null && isCaretOnClassIdentifier(element, psiClass)) {
            return true;
        }

        return PsiTreeUtil.getParentOfType(element, PsiMethod.class) != null;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return;
        }

        // 优先检查是否直接在变量上
        final PsiVariable specificVariable = PsiTreeUtil.getParentOfType(element, PsiVariable.class, false);
        if (specificVariable != null) {
            addFinalModifierIfNotPresent(specificVariable);
            return;
        }

        // 检查是否在类名标识符上（而不是在类体内的其他地方）
        final PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        if (psiClass != null && isCaretOnClassIdentifier(element, psiClass)) {
            processClass(psiClass);
            return;
        }

        final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (containingMethod != null) {
            processMethod(containingMethod);
        }
    }

    /**
     * 处理整个类
     */
    private void processClass(final PsiClass psiClass) {
        // 处理类字段
        for (final PsiField field : psiClass.getFields()) {
            addFinalModifierIfNotPresent(field);
        }

        // 处理类中所有方法
        for (final PsiMethod method : psiClass.getMethods()) {
            processMethod(method);
        }
    }

    /**
     * 处理单个方法
     */
    private void processMethod(final PsiMethod method) {
        // 给方法参数添加 final
        final PsiParameterList parameterList = method.getParameterList();
        for (final PsiParameter parameter : parameterList.getParameters()) {
            addFinalModifierIfNotPresent(parameter);
        }

        // 给方法内局部变量添加 final
        final PsiCodeBlock methodBody = method.getBody();
        if (methodBody != null) {
            final Collection<PsiLocalVariable> localVariables = PsiTreeUtil.collectElementsOfType(methodBody, PsiLocalVariable.class);
            for (final PsiLocalVariable localVariable : localVariables) {
                addFinalModifierIfNotPresent(localVariable);
            }
        }
    }

    private void addFinalModifierIfNotPresent(final PsiModifierListOwner element) {
        if (element != null) {
            final PsiModifierList modifierList = element.getModifierList();
            if (modifierList != null && !modifierList.hasExplicitModifier(PsiModifier.FINAL) && canAddFinal(element)) {
                PsiUtil.setModifierProperty(element, PsiModifier.FINAL, true);
            }
        }
    }

    /**
     * 判断是否可以添加 final 修饰符
     */
    private boolean canAddFinal(final PsiModifierListOwner element) {
        final PsiModifierList modifierList = element.getModifierList();
        if (modifierList == null) {
            return false;
        }

        // 对于字段，需要更严格的检查
        if (element instanceof PsiField) {
            final PsiField field = (PsiField) element;
            // 如果字段没有初始化器，需要检查是否在构造器中被初始化
            if (field.getInitializer() == null) {
                // 如果是静态字段但没有初始化器，不能添加 final
                if (modifierList.hasExplicitModifier(PsiModifier.STATIC)) {
                    return false;
                }
                // 对于实例字段，检查是否在构造器中被初始化
                if (!isFieldInitializedInConstructor(field)) {
                    return false;
                }
            }
        }

        // 抽象方法的参数不能添加 final（虽然抽象方法本身没有方法体，但为了安全起见）
        if (element instanceof PsiParameter) {
            final PsiParameter parameter = (PsiParameter) element;
            final PsiElement parent = parameter.getParent();
            if (parent instanceof PsiParameterList) {
                final PsiElement grandParent = parent.getParent();
                if (grandParent instanceof PsiMethod) {
                    final PsiMethod method = (PsiMethod) grandParent;
                    return !method.hasModifierProperty(PsiModifier.ABSTRACT);
                }
            }
        }

        return true;
    }

    /**
     * 检查字段是否在构造器中被初始化
     */
    private boolean isFieldInitializedInConstructor(final PsiField field) {
        final PsiClass containingClass = field.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        final PsiMethod[] constructors = containingClass.getConstructors();
        if (constructors.length == 0) {
            // 没有显式构造器，Java会提供默认构造器，字段不会被初始化
            return false;
        }

        // 检查所有构造器是否都初始化了该字段
        for (final PsiMethod constructor : constructors) {
            if (!isFieldInitializedInMethod(field, constructor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查字段是否在指定方法中被初始化
     */
    private boolean isFieldInitializedInMethod(final PsiField field, final PsiMethod method) {
        final PsiCodeBlock body = method.getBody();
        if (body == null) {
            return false;
        }

        final Collection<PsiAssignmentExpression> assignments = PsiTreeUtil.collectElementsOfType(body, PsiAssignmentExpression.class);
        for (final PsiAssignmentExpression assignment : assignments) {
            final PsiExpression lhs = assignment.getLExpression();
            if (lhs instanceof PsiReferenceExpression) {
                final PsiReferenceExpression ref = (PsiReferenceExpression) lhs;
                if (field.equals(ref.resolve())) {
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
     * 检查光标是否在类名标识符上
     */
    private boolean isCaretOnClassIdentifier(final PsiElement element, final PsiClass psiClass) {
        // 检查光标所在元素是否是类名标识符
        return element.getParent() == psiClass.getNameIdentifier() || element == psiClass.getNameIdentifier();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
