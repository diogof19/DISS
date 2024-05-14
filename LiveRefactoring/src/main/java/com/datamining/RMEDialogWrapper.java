package com.datamining;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RMEDialogWrapper extends DialogWrapper {
    private final JRadioButton localRepoButton = new JRadioButton("Local Repository");
    private final JRadioButton remoteRepoButton = new JRadioButton("Remote Repository");
    private final JLabel repositoryPathLabel = new JLabel("Repository Path");
    private final JBTextField repositoryPathTextField = new JBTextField();
    private final JLabel branchLabel = new JLabel("Branch");
    private final JBTextField branchTextField = new JBTextField("main");
    private final JLabel commitLabel = new JLabel("Starting Commit SHA (Optional)");
    private final JBTextField commitTextField = new JBTextField();
    private final JLabel warningLabel =
            new JLabel("Warning: Depending on the number of commits, this may take a while to complete.");

    protected RMEDialogWrapper() {
        super(true);
        init();
        setTitle("Refactoring Metrics Extraction");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        Box box = Box.createHorizontalBox();
        box.setPreferredSize(new Dimension(200, 100));

        JPanel panel = new JPanel(new GridLayout(5, 2));

        localRepoButton.setSelected(true);
        localRepoButton.addActionListener(e -> {
            remoteRepoButton.setSelected(false);
        });

        remoteRepoButton.addActionListener(e -> {
            localRepoButton.setSelected(false);
        });

        panel.add(localRepoButton, BorderLayout.WEST);
        panel.add(remoteRepoButton, BorderLayout.EAST);
        panel.add(repositoryPathLabel, BorderLayout.WEST);
        panel.add(repositoryPathTextField, BorderLayout.EAST);
        panel.add(branchLabel, BorderLayout.WEST);
        panel.add(branchTextField, BorderLayout.EAST);
        panel.add(commitLabel, BorderLayout.WEST);
        panel.add(commitTextField, BorderLayout.EAST);
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

    public Boolean isLocalRepo() {
        return localRepoButton.isSelected();
    }

    public String getCommit() {
        if (commitTextField.getText().isBlank()) {
            return null;
        }
        return commitTextField.getText();
    }
}