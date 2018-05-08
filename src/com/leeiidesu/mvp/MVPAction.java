package com.leeiidesu.mvp;

import com.google.common.base.CaseFormat;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.JavaCreateTemplateInPackageAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by leeiidesu on 2017/11/8.
 */
public class MVPAction extends JavaCreateTemplateInPackageAction<PsiClass> {
    private static final Logger log = Logger.getInstance(MVPAction.class);


    private static final String MODE_SINGLE_ACTIVITY = "single_activity";
    private static final String MODE_MULTI = "activity and fragment";
    private static final String MODE_FRAGMENT = "only fragment";
    private String projectPackage;


    protected MVPAction() {
        super("", IdeBundle.message("action.create.new.class.description"),
                PlatformIcons.METHOD_ICON, true);
    }

    @Nullable
    @Override
    protected PsiElement getNavigationElement(@NotNull PsiClass psiClass) {
        return psiClass.getLBrace();
    }

    /**
     * 执行创建
     * @param dir
     * @param className
     * @param templateName
     * @return
     * @throws IncorrectOperationException
     */
    @Nullable
    @Override
    protected PsiClass doCreate(PsiDirectory dir, String className, String templateName) throws IncorrectOperationException {
        projectPackage = Utils.getProjectPackage(dir, dir.getProject());
        switch (templateName) {
            case MODE_SINGLE_ACTIVITY:
                // activity创建模式
                String activityName = className + "Activity";

                PsiClass activity = JavaDirectoryService.getInstance()
                        .createClass(dir, activityName
                                , "DggActivity");
                onProcessSameFile(dir, className, activity);
                createSameFile(dir, className);

                String packageName = activity.getQualifiedName().replace("." + activityName, "");

                new MethodWriter(dir.getProject(), "Generate Method dagger", className, activity, packageName, false).execute();

                return activity;
            case MODE_MULTI:
                break;
            case MODE_FRAGMENT:
                // fragment创建模式
                String fragmentName = className + "Fragment";
                PsiClass fragment = JavaDirectoryService.getInstance()
                        .createClass(dir, fragmentName
                                , "DggFragment");
                onProcessSameFile(dir, className, fragment);
                createSameFile(dir, className);

                String packageName2 = fragment.getQualifiedName().replace("." + fragmentName, "");

                new MethodWriter(dir.getProject(), "Generate Method dagger", className, fragment, packageName2, true).execute();
                return fragment;
        }
        return null;
    }

    /**
     * 创建模式中相同的文件 presenter跟contract
     * @param dir
     * @param className
     */
    private void createSameFile(PsiDirectory dir, String className) {
        //创建Presenter
        PsiClass leePresenter = JavaDirectoryService.getInstance()
                .createClass(dir, className + "Presenter",
                        "DggPresenter");
        //创建Contract
        PsiClass leeContract = JavaDirectoryService.getInstance()
                .createClass(dir, className + "Contract",
                        "DggContract");

        onProcessSameFile(dir, className, leeContract, leePresenter);
    }

    private void onProcessSameFile(PsiDirectory dir, String className, PsiClass... itemClass) {
        for (PsiClass item :
                itemClass) {
            processFile(dir, className, item);
        }
    }

    /**
     * 处理文件 替换一些代码
     * @param dir
     * @param className
     * @param itemClass
     */
    private void processFile(PsiDirectory dir, String className, PsiClass itemClass) {

        PsiFile file = itemClass.getContainingFile();
        final PsiDocumentManager manager = PsiDocumentManager.getInstance(itemClass.getProject());
        final Document document = manager.getDocument(file);
        if (document == null) {
            return;
        }

        new WriteCommandAction.Simple(itemClass.getProject()) {
            @Override
            protected void run() throws Throwable {
                manager.doPostponedOperationsAndUnblockDocument(document);

                String replace = document.getText()
                        .replace("CLASS_ORIGIN", className)
                        .replace("ALL_LOWER_NAME",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className))
                        .replace("START_LOWER_NAME",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, className));
                if (projectPackage != null) {
                    replace = replace.replace("PROJECT_PACKAGE",
                            projectPackage
                    );
                }
                document.setText(replace);
                CodeStyleManager.getInstance(itemClass.getProject()).reformat(itemClass);
            }
        }.execute();

    }

    @Override
    protected void buildDialog(Project project, PsiDirectory psiDirectory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle("Auto Generate")
                .addKind("Activity", PlatformIcons.CLASS_ICON, MODE_SINGLE_ACTIVITY)
                .addKind("Fragment", PlatformIcons.CLASS_ICON, MODE_FRAGMENT);
//                .addKind("无逻辑Activity + Fragment", PlatformIcons.CLASS_ICON, MODE_MULTI);

        builder.setValidator(new InputValidatorEx() {
            @Nullable
            @Override
            public String getErrorText(String inputString) {
                if (inputString.length() > 0 &&
                        !PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
                    return "This is not a valid Java qualified name";
                }
                return null;
            }

            @Override
            public boolean checkInput(String inputString) {
                return true;
            }

            @Override
            public boolean canClose(String inputString) {
                return !StringUtil.isEmptyOrSpaces(inputString) &&
                        getErrorText(inputString) == null;
            }
        });

    }

    @Override
    protected String getActionName(PsiDirectory psiDirectory, String newName, String s1) {
        return IdeBundle.message("progress.creating.class",
                StringUtil.getQualifiedName(
                        JavaDirectoryService.getInstance().
                                getPackage(psiDirectory).
                                getQualifiedName(),
                        newName
                )
        );
    }
}
