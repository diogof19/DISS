package com.datamining;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfirmationPopulDialogWrapper extends DialogWrapper {
    protected ConfirmationPopulDialogWrapper(String message) {
        super(true);
        init();
        setTitle(message);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));

        return null;
    }

    @Override
    protected @NotNull Action getOKAction() {

        return super.getOKAction();
    }
}
