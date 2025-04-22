package ch.csnc.burp;

import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyContext;

public class CopyRequestResponseHotKeyHandler {

    public static void register() {
        CopyRequestResponseExtension.api().userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, CopyRequestResponseConfiguration.copyFullFullOrSelectionHotKey(), event -> {
            event.messageEditorRequestResponse().ifPresent(messageEditorHttpRequestResponse -> {
                if (messageEditorHttpRequestResponse.selectionContext() == MessageEditorHttpRequestResponse.SelectionContext.RESPONSE && messageEditorHttpRequestResponse.selectionOffsets().isPresent()) {
                    CopyRequestResponseCopyActions.copyFullHeaderPlusSelectedData(messageEditorHttpRequestResponse);
                } else {
                    CopyRequestResponseCopyActions.copyFullFull(messageEditorHttpRequestResponse);
                }
            });
        });

        CopyRequestResponseExtension.api().userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, CopyRequestResponseConfiguration.copyFullHeaderHotKey(), event -> {
            event.messageEditorRequestResponse().ifPresent(CopyRequestResponseCopyActions::copyFullHeader);
        });
    }

    private CopyRequestResponseHotKeyHandler() {
        // static class
    }
}
