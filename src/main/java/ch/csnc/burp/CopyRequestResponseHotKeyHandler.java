package ch.csnc.burp;

import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import java.util.List;

public class CopyRequestResponseHotKeyHandler {

    public static void register() {
        CopyRequestResponseExtension
                .api()
                .userInterface()
                .registerHotKeyHandler(
                        HotKeyContext.HTTP_MESSAGE_EDITOR,
                        CopyRequestResponseConfiguration.copyFullFullOrSelectionHotKey(),
                        event -> {
                            event.messageEditorRequestResponse().ifPresent(messageEditorHttpRequestResponse -> {
                                if (messageEditorHttpRequestResponse.selectionContext() == MessageEditorHttpRequestResponse.SelectionContext.RESPONSE && messageEditorHttpRequestResponse.selectionOffsets().isPresent()) {
                                    CopyRequestResponseCopyActions.copyFullHeaderPlusSelectedData(messageEditorHttpRequestResponse);
                                } else {
                                    CopyRequestResponseCopyActions.copyFullFull(List.of(messageEditorHttpRequestResponse.requestResponse()));
                                }
                            });
                        });

        CopyRequestResponseExtension
                .api()
                .userInterface()
                .registerHotKeyHandler(
                        HotKeyContext.HTTP_MESSAGE_EDITOR,
                        CopyRequestResponseConfiguration.copyFullHeaderHotKey(),
                        event -> event.messageEditorRequestResponse()
                                .map(MessageEditorHttpRequestResponse::requestResponse)
                                .map(List::of)
                                .ifPresent(CopyRequestResponseCopyActions::copyFullHeader));
    }

    private CopyRequestResponseHotKeyHandler() {
        // static class
    }
}
