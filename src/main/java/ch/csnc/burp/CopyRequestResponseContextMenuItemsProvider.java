package ch.csnc.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class CopyRequestResponseContextMenuItemsProvider implements ContextMenuItemsProvider {

    private static final String CUT_TEXT = Optional.ofNullable(System.getProperty("copyRequestResponseCutText")).orElse("[...]");

    private final MontoyaApi api;

    public CopyRequestResponseContextMenuItemsProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        var editor = event.messageEditorRequestResponse().orElse(null);

        if (editor == null) {
            // context menu not opened in editor
            return null;
        }

        var menuItems = new ArrayList<Component>();

        var copyFullFull = new JMenuItem("Copy HTTP Request & Response (Full/Full)");
        copyFullFull.addActionListener(actionEvent -> this.copyFullFull(editor));
        menuItems.add(copyFullFull);

        var copyFullHeader = new JMenuItem("Copy HTTP Request & Response (Full/Header)");
        copyFullHeader.addActionListener(actionEvent -> this.copyFullHeader(editor));
        menuItems.add(copyFullHeader);

        if (editor.selectionContext() == MessageEditorHttpRequestResponse.SelectionContext.RESPONSE) {
            var copyFullHeaderPlusSelectedData = new JMenuItem("Copy HTTP Request & Response (Full/Header + Selected Data)");
            copyFullHeaderPlusSelectedData.addActionListener(actionEvent -> this.copyFullHeaderPlusSelectedData(editor));
            menuItems.add(copyFullHeaderPlusSelectedData);
        }

        return List.copyOf(menuItems);
    }

    private void copyFullFull(MessageEditorHttpRequestResponse editor) {
        var requestResponse = editor.requestResponse();

        var text = "%s\n\n%s".formatted(
                requestResponse.request().toString().strip(),
                Optional.ofNullable(requestResponse.response())
                        .map(HttpResponse::toString)
                        .map(String::strip)
                        .orElse(""));

        this.toClipboard(text);
    }

    private void copyFullHeader(MessageEditorHttpRequestResponse editor) {
        var requestResponse = editor.requestResponse();
        var requestString = requestResponse.request().toString().strip();

        var responseString = "";
        if (requestResponse.hasResponse()) {
            var response = requestResponse.response();
            responseString = response.toString().substring(0, response.bodyOffset()).strip();
            responseString += "\n\n";
            responseString += CUT_TEXT;
        }

        var text = "%s\n\n%s".formatted(requestString, responseString);
        this.toClipboard(text);
    }

    private void copyFullHeaderPlusSelectedData(MessageEditorHttpRequestResponse editor) {
        var requestResponse = editor.requestResponse();
        var requestString = requestResponse.request().toString().strip();

        Supplier<String> responseStringSupplier = () -> {
            if (!requestResponse.hasResponse()) {
                return "";
            }

            var response = requestResponse.response();
            var responseString = response.toString().substring(0, response.bodyOffset()).strip();
            responseString += "\n\n";


            var selectionOffsets = editor.selectionOffsets().orElse(null);

            if (selectionOffsets == null) {
                // nothing selected
                responseString += CUT_TEXT;
                return responseString;
            }

            var startIndex = selectionOffsets.startIndexInclusive();
            if (startIndex < response.bodyOffset()) {
                startIndex = response.bodyOffset();
            }

            var endIndex = selectionOffsets.endIndexExclusive();
            this.api.logging().logToError("start: %d, end %d".formatted(startIndex, endIndex));
            if (endIndex <= startIndex) {
                responseString += CUT_TEXT;
                return responseString;
            }

            var selectedText = response.toByteArray().subArray(startIndex, endIndex);

            if (startIndex == response.bodyOffset() && endIndex < response.toByteArray().length()) {
                responseString += selectedText;
                responseString += CUT_TEXT;
                return responseString;
            }

            if (startIndex > response.bodyOffset() && endIndex == response.toByteArray().length()) {
                responseString += CUT_TEXT;
                responseString += selectedText;
                return responseString;
            }

            if (startIndex == response.bodyOffset() && endIndex == response.toByteArray().length()) {
                responseString += selectedText;
                return responseString;
            }

            responseString += CUT_TEXT;
            responseString += selectedText;
            responseString += CUT_TEXT;
            return responseString;
        };

        var text = "%s\n\n%s".formatted(requestString, responseStringSupplier.get());

        // Ugly hack because VMware is messing up the clipboard if a text is still selected, the function
        // has to be run in a separate thread which sleeps for 0.2 seconds.
        var thread = new Thread(() -> {
            try {
                Thread.sleep(200);
                this.toClipboard(text);
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setName("ToClipboardThread");
        thread.setDaemon(true);
        thread.start();
    }

    private void toClipboard(String text0) {
        var text1 = text0.replaceAll("\r\n", "\n");
        var systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var systemSelection = Toolkit.getDefaultToolkit().getSystemSelection();
        var transferText = new StringSelection(text1);
        systemClipboard.setContents(transferText, null);
        systemSelection.setContents(transferText, null);
    }
}
