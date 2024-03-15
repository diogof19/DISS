package com.datamining;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RMEDialogWrapper extends DialogWrapper {
    private final JLabel repositoryPathLabel = new JLabel("Repository Path");
    private JBTextField repositoryPathTextField = new JBTextField();
    private final JLabel branchLabel = new JLabel("Branch");
    private JBTextField branchTextField = new JBTextField("main");
    private final JLabel warningLabel =
            new JLabel("Warning: Depending on the number of commits, this may take a while to complete.");
    protected RMEDialogWrapper() {
        super(true);
        init();
        setTitle("Refactoring Miner Extraction");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        Box box = Box.createHorizontalBox();
        box.setPreferredSize(new Dimension(300, 100));

        JPanel panel = new JPanel(new GridLayout(3, 2));

        panel.add(repositoryPathLabel, BorderLayout.WEST);
        panel.add(repositoryPathTextField, BorderLayout.EAST);
        panel.add(branchLabel, BorderLayout.WEST);
        panel.add(branchTextField, BorderLayout.EAST);
        panel.add(warningLabel, BorderLayout.CENTER);

        box.add(panel);

        return box;
    }

    public String getRepositoryPath() {
        return repositoryPathTextField.getText();
    }

    public String getBranch() {
        return branchTextField.getText();
    }
}