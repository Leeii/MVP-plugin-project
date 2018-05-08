package com.leeiidesu.mvp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;

import java.util.ArrayList;

/**
 * Created by leeiidesu on 2017/7/28.
 */
public class Utils {
    private static final Logger log = Logger.getInstance(Utils.class);

    public static XmlFile findAndroidManifest(PsiElement element, Project project) {
        PsiFile[] psiFiles = resolveFile(element, project, "AndroidManifest.xml");

        return filterAndroidManifestFile(psiFiles);
    }

    private static XmlFile filterAndroidManifestFile(PsiFile[] psiFiles) {
        ArrayList<PsiFile> xmlFiles = new ArrayList<>();
        if (psiFiles == null) return null;

        for (PsiFile file : psiFiles) {
            if (file.getModificationStamp() != 0) {
                String path = file.getViewProvider().getVirtualFile().getPath();
                boolean contains = path.contains("/build/intermediates/manifest") || path.contains("/app/src/main/");
                if (!contains)
                    xmlFiles.add(file);
            }
        }

        return xmlFiles.size() > 0 ? (XmlFile) xmlFiles.get(0) : (XmlFile) psiFiles[0];
    }

    public static String getProjectPackage(PsiElement element, Project project) {
        XmlFile androidManifest = findAndroidManifest(element, project);

        if (androidManifest == null) return null;
        XmlTag manifest = androidManifest.getRootTag();

        if (manifest != null) {
            return manifest.getAttributeValue("package");
        }
        return null;
    }

    public static PsiFile[] resolveFile(PsiElement element, Project project, String name) {

        // restricting the search to the current module - searching the whole project could return wrong layouts
        Module module = ModuleUtil.findModuleForPsiElement(element);
        PsiFile[] files = null;
        if (module != null) {
            // first omit libraries, it might cause issues like (#103)
            GlobalSearchScope moduleScope = module.getModuleContentScope();
            files = FilenameIndex.getFilesByName(project, name, moduleScope);
        }
        if (files == null || files.length <= 0) {
            // fallback to search through the whole project
            // useful when the project is not properly configured - when the resource directory is not configured
            files = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            if (files.length <= 0) {
                return null; //no matching files
            }
        }

        // TODO - we have a problem here - we still can have multiple layouts (some coming from a dependency)
        // we need to resolve R class properly and find the proper layout for the R class
        for (PsiFile file : files) {
            log.info("Resolved file for name [" + name + "]: " + file.getVirtualFile());
        }


        return files;
    }

    /**
     * Display simple notification - information
     *
     * @param project
     * @param text
     */
    public static void showInfoNotification(Project project, String text) {
        showNotification(project, MessageType.INFO, text);
    }

    /**
     * Display simple notification - error
     *
     * @param project
     * @param text
     */
    public static void showErrorNotification(Project project, String text) {
        showNotification(project, MessageType.ERROR, text);
    }

    /**
     * Display simple notification of given type
     *
     * @param project
     * @param type
     * @param text
     */
    public static void showNotification(Project project, MessageType type, String text) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, type, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

}
