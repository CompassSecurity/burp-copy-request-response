package ch.csnc.burp;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class CopyRequestResponseConfigurationDialog {

    private final JDialog dialog;

    public CopyRequestResponseConfigurationDialog() {
        var cutTextLabel = new JLabel("Cut Text:");
        var copyFullFullOrSelectionHotKeyLabel = new JLabel("HotKey for Copy Full/Full or Full/Header + Selected Data:");
        var copyFullHeaderLabel = new JLabel("HotKey for Copy Full/Header:");

        var cutTextTextField = new JTextField(CopyRequestResponseConfiguration.cutText());
        var copyFullFullOrSelectionHotKeyTextField = new JTextField(CopyRequestResponseConfiguration.copyFullFullOrSelectionHotKey());
        var copyFullHeaderTextField = new JTextField(CopyRequestResponseConfiguration.copyFullHeaderHotKey());

        cutTextTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
                CopyRequestResponseConfiguration.setCutText(cutTextTextField.getText());
            }
        });

        copyFullFullOrSelectionHotKeyTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
                CopyRequestResponseConfiguration.setCopyFullFullOrSelectionHotKey(copyFullFullOrSelectionHotKeyTextField.getText());
            }
        });

        copyFullHeaderTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
                CopyRequestResponseConfiguration.setCopyFullHeaderHotKey(copyFullHeaderTextField.getText());
            }
        });

        var textFieldColumns = 40;
        cutTextTextField.setColumns(textFieldColumns);
        copyFullFullOrSelectionHotKeyTextField.setColumns(textFieldColumns);
        copyFullHeaderTextField.setColumns(textFieldColumns);

        var useNbspCheckbox = new JCheckBox("Use Non-Breakable Spaces");
        useNbspCheckbox.setSelected(CopyRequestResponseConfiguration.useNonBreakableSpace());
        useNbspCheckbox.addActionListener(event -> CopyRequestResponseConfiguration.setUseNonBreakableSpace(useNbspCheckbox.isSelected()));

        var panel = new JPanel();
        panel.setLayout(new MigLayout());
        panel.add(cutTextLabel);
        panel.add(cutTextTextField, "grow, wrap");
        panel.add(useNbspCheckbox, "skip, grow, wrap");
        panel.add(copyFullFullOrSelectionHotKeyLabel);
        panel.add(copyFullFullOrSelectionHotKeyTextField, "grow, wrap");
        panel.add(copyFullHeaderLabel);
        panel.add(copyFullHeaderTextField, "grow, wrap");

        this.dialog = new JDialog(CopyRequestResponseExtension.api().userInterface().swingUtils().suiteFrame(), true);
        this.dialog.setLocationRelativeTo(CopyRequestResponseExtension.api().userInterface().swingUtils().suiteFrame());
        this.dialog.setTitle("CopyRequestResponse Configuration Dialog");
        this.dialog.setContentPane(panel);
        this.dialog.setResizable(false);
        this.dialog.setSize(610, 170);
    }

    public void show() {
        this.dialog.setVisible(true);
    }
}
