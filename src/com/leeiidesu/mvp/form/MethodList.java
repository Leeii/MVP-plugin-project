package com.leeiidesu.mvp.form;

import com.intellij.psi.PsiMethod;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Created by leeiidesu on 2017/11/8.
 */
public class MethodList extends JPanel {
    private PsiMethod[] methods;

    public MethodList(PsiMethod[] methods) {
        this.methods = methods;
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(300, 26));
        label.setText("请选择一个构造函数进行生成");

        JPanel methodPanel = new JPanel();
        methodPanel.setLayout(new BoxLayout(methodPanel, BoxLayout.PAGE_AXIS));
        methodPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        for (int i = 0; i < methods.length; i++) {
            final PsiMethod method = methods[i];
            MethodPanel panel = new MethodPanel(method);
            methodPanel.add(panel);

            panel.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onClick(method);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });
        }

        contentPanel.add(label);
        contentPanel.add(methodPanel);

        add(contentPanel);


        revalidate();
    }

    public interface OnItemClickListener {
        void onClick(PsiMethod method);
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
