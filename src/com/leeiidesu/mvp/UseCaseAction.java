package com.leeiidesu.mvp;

import com.leeiidesu.mvp.form.MethodList;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by leeiidesu on 2017/11/8.
 */
public class UseCaseAction extends BaseGenerateAction {
    public UseCaseAction() {
        this(null);
    }

    public UseCaseAction(CodeInsightActionHandler handler) {
        super(handler);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        actionPerformedImpl(project, editor);
    }

    @Override
    public void actionPerformedImpl(@NotNull Project project, Editor editor) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) file;
            final PsiClass psiClass = psiJavaFile.getClasses()[0];
            if (isUseCase(psiClass)) {
                PsiFile[] mUseCaseModule = Utils.resolveFile(psiClass, project, "UseCaseModule.java");
                if (mUseCaseModule == null || mUseCaseModule.length == 0) {
                    Utils.showInfoNotification(project, "没有找到 UseCaseModule.java");
                    return;
                }

                PsiJavaFile javaFile = (PsiJavaFile) mUseCaseModule[0];

                PsiMethod[] methodsByName = psiClass.findMethodsByName(psiClass.getName(), false);
                if (methodsByName.length == 0) {
                    insertInjectMethod(psiClass.getQualifiedName(), psiClass.getName(), null, javaFile);
                } else if (methodsByName.length == 1) {
                    insertInjectMethod(psiClass.getQualifiedName(), psiClass.getName(), methodsByName[0], javaFile);
                } else {
                    final JFrame mDialog;
                    mDialog = new JFrame();
                    mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

                    MethodList methodList = new MethodList(methodsByName);
                    methodList.setOnItemClickListener(
                            new MethodList.OnItemClickListener() {
                                @Override
                                public void onClick(PsiMethod method) {
                                    mDialog.setVisible(false);
                                    mDialog.dispose();
                                    insertInjectMethod(psiClass.getQualifiedName(), psiClass.getName(), method, javaFile);
                                }
                            });

                    mDialog.getContentPane().add(methodList);
                    mDialog.pack();
                    mDialog.setLocationRelativeTo(null);
                    mDialog.setVisible(true);

                }


                for (PsiMethod pm :
                        methodsByName) {
                    System.out.println("方法：" + pm.getName() + " ==== " + pm.getText());

                    PsiParameterList parameterList = pm.getParameterList();
                    PsiParameter[] parameters = parameterList.getParameters();

                    for (PsiParameter parameter :
                            parameters
                            ) {
                        System.out.println("参数类型 ： " + parameter.getName() + " === " + parameter.getTypeElement());
                    }
                }
            } else {
                Utils.showInfoNotification(project, "这不是一个UseCase类");
            }
        } else {
            Utils.showInfoNotification(project, "这不是一个Java文件");
        }
    }

    private void insertInjectMethod(String qualifiedName, String name, PsiMethod method, PsiJavaFile javaFile) {
        StringBuilder builder = new StringBuilder();

        builder.append("@Provides").append('\n')
                .append("@ModuleSingleton").append('\n')
                .append(qualifiedName).append(" ").append("provider").append(name).append("(");

        if (method != null) {
            PsiParameter[] parameters = method.getParameterList().getParameters();

            for (PsiParameter parameter :
                    parameters) {
                builder.append(parameter.getTypeElement().getText()).append(" ").append(parameter.getName()).append(",");

            }

            builder.deleteCharAt(builder.length() - 1);
        }

        builder.append("){");
        builder.append("return new ").append(qualifiedName).append("(");
        if (method != null) {
            PsiParameter[] parameters = method.getParameterList().getParameters();

            for (PsiParameter parameter :
                    parameters) {
                builder.append(parameter.getName()).append(",");
            }

            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(");");
        builder.append("}");

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(javaFile.getProject());
        PsiClass aClass = javaFile.getClasses()[0];
        PsiMethod methodFromText = factory.createMethodFromText(builder.toString(), aClass);


        PsiClass exproses = aClass.getInnerClasses()[0];
        PsiMethod methodFromText1 = factory.createMethodFromText(qualifiedName + " get" + name + "();", exproses);

        new WriteCommandAction.Simple(javaFile.getProject()) {
            @Override
            protected void run() throws Throwable {
                aClass.add(methodFromText);
                exproses.add(methodFromText1);

                Utils.showInfoNotification(javaFile.getProject(), "success");
            }
        }.execute();
    }

    private boolean isUseCase(PsiClass aClass) {
        PsiClass[] interfaces = aClass.getInterfaces();
        for (PsiClass psiClass : interfaces) {
            String name = psiClass.getName();
            if (name != null && name.contains("UseCase")) {
                return true;
            }
        }
        return false;
    }
}
