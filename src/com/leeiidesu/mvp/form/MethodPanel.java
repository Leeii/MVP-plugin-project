package com.leeiidesu.mvp.form;

import com.intellij.psi.PsiMethod;

import javax.swing.*;
import java.awt.*;

/**
 * Created by leeiidesu on 2017/11/8.
 */
public class MethodPanel extends JPanel {
    private JLabel label;

    private PsiMethod method;


    public MethodPanel(PsiMethod method) {
        this.method = method;

        label = new JLabel();

        label.setPreferredSize(new Dimension(300, 26));

        String text = method.getText();
        int i = text.indexOf(')');
        String substring = text.substring(0, i + 1);

        label.setText(substring);

        add(label);
    }


}
