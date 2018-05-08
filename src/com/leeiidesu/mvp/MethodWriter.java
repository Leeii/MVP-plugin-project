package com.leeiidesu.mvp;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.Locale;

/**
 * Created by leeiidesu on 2017/11/8.
 */
public class MethodWriter extends WriteCommandAction.Simple {
    private final String className;
    private final PsiElement element;
    private final PsiElementFactory mFactory;
    private final boolean isFragment;

    private final String packageName;

    private PsiClass injectsClass;
    private PsiClass viewModule;
    private PsiClass presenterModule;

    protected MethodWriter(Project project, String commandName, String className, PsiElement element, String packageName, boolean isFragment) {
        super(project, commandName);
        this.className = className;
        this.element = element;
        mFactory = JavaPsiFacade.getElementFactory(project);
        this.packageName = packageName;
        this.isFragment = isFragment;

        if (isFragment) {
            PsiFile[] psiFiles = Utils.resolveFile(element, project, "FragmentModule.java");
            if (psiFiles != null && psiFiles.length != 0) {
                viewModule = ((PsiJavaFile) psiFiles[0]).getClasses()[0];
            }
            PsiFile[] psiFiles2 = Utils.resolveFile(element, project, "FragmentPresenterModule.java");
            if (psiFiles2 != null && psiFiles2.length != 0) {
                presenterModule = ((PsiJavaFile) psiFiles2[0]).getClasses()[0];
            }

            PsiFile[] injects = Utils.resolveFile(element, project, "FragmentComponentInjects.java");
            if (injects != null && injects.length != 0) {
                injectsClass = ((PsiJavaFile) injects[0]).getClasses()[0];
            }
        } else {
            PsiFile[] psiFiles = Utils.resolveFile(element, project, "ActivityModule.java");
            if (psiFiles != null && psiFiles.length != 0) {
                viewModule = ((PsiJavaFile) psiFiles[0]).getClasses()[0];
            }
            PsiFile[] psiFiles2 = Utils.resolveFile(element, project, "ActivityPresenterModule.java");
            if (psiFiles2 != null && psiFiles2.length != 0) {
                presenterModule = ((PsiJavaFile) psiFiles2[0]).getClasses()[0];
            }

            PsiFile[] injects = Utils.resolveFile(element, project, "ActivityComponentInjects.java");
            if (injects != null && injects.length != 0) {
                injectsClass = ((PsiJavaFile) injects[0]).getClasses()[0];
            }
        }
    }

    @Override
    protected void run() throws Throwable {
        generateProviderView();
        generateProviderPresenter();
        generateProviderInject();
    }

    private void generateProviderPresenter() {
        if (presenterModule == null) {
            Utils.showInfoNotification(getProject(), "没有找到PresenterModule.java");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("@Provides")
                .append(isFragment ? "@FragmentScope" : "@ActivityScope")
                .append("\n")
                .append(String.format(Locale.CHINA, "%s.%sContract.I%sPresenter provider%sPresenter() {", packageName, className, className, className)).append("\n")
                .append(String.format(Locale.CHINA, "%s.%sPresenter presenter = new %s.%sPresenter();", packageName,  className, packageName, className)).append("\n")
                .append(isFragment ? "getFragmentComponent().inject(presenter);" : "getActivityComponent().inject(presenter);").append("\n")
                .append("return presenter;").append("\n")
                .append("}");

        presenterModule.add(mFactory.createMethodFromText(builder.toString(), presenterModule));
    }

    private void generateProviderView() {
        if (viewModule == null) {
            Utils.showInfoNotification(getProject(), "没有找到ViewModule");

            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("@Provides").append("\n")
                .append(isFragment ? "@FragmentScope" : "@ActivityScope").append("\n")
                .append(String.format(Locale.CHINA, "%s.%sContract.I%sView provider%sView() {", packageName, className, className, className)).append("\n")
                .append(String.format(Locale.CHINA, "return (%s.%sContract.I%sView) %s();", packageName, className, className, isFragment ? "getDaggerFragment" : "getDaggerActivity")).append('\n')
                .append("}");
        viewModule.add(mFactory.createMethodFromText(builder.toString(), viewModule));
    }

    private void generateProviderInject() {
        if (injectsClass == null) {
            Utils.showInfoNotification(getProject(), "没有找到**Injects.java");

            return;
        }

        injectsClass.add(mFactory.createMethodFromText(String.format(Locale.CHINA, "void inject(%s.%sPresenter presenter);", packageName,  className), injectsClass));
        if (isFragment) {
            injectsClass.add(mFactory.createMethodFromText(String.format(Locale.CHINA, "void inject(%s.%sFragment fragment);", packageName, className), injectsClass));
        } else
            injectsClass.add(mFactory.createMethodFromText(String.format(Locale.CHINA, "void inject(%s.%sActivity activity);", packageName, className), injectsClass));
    }
}
