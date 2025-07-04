package ch.csnc.burp;

import burp.api.montoya.http.handler.TimingData;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CopyRequestResponseCopyActions {

    public static void copyFullFull(List<HttpRequestResponse> requestResponses) {
        var text =
                requestResponses
                        .stream()
                        .map(requestResponse ->
                                "%s\n\n%s".formatted(
                                        requestResponse.request().toString().strip(),
                                        Optional.ofNullable(requestResponse.response())
                                                .map(HttpResponse::toString)
                                                .map(String::strip)
                                                .orElse("")))
                        .collect(Collectors.joining("\n\n"));

        toClipboard(text);
    }

    public static void copyFullHeader(List<HttpRequestResponse> requestResponses) {
        var text =
                requestResponses
                        .stream()
                        .map(requestResponse -> {
                            var requestString = requestResponse.request().toString().strip();

                            var responseString = "";
                            if (requestResponse.hasResponse()) {
                                var response = requestResponse.response();
                                responseString = response.toString().substring(0, response.bodyOffset()).strip();
                                responseString += "\n\n";
                                responseString += CopyRequestResponseConfiguration.cutText();
                            }

                            return "%s\n\n%s".formatted(requestString, responseString);
                        })
                        .collect(Collectors.joining("\n\n"));

        toClipboard(text);
    }

    public static void copyFullHeaderPlusSelectedData(MessageEditorHttpRequestResponse editor) {
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
                responseString += CopyRequestResponseConfiguration.cutText();
                return responseString;
            }

            var startIndex = selectionOffsets.startIndexInclusive();
            if (startIndex < response.bodyOffset()) {
                startIndex = response.bodyOffset();
            }

            var endIndex = selectionOffsets.endIndexExclusive();
            CopyRequestResponseExtension.api().logging().logToError("start: %d, end %d".formatted(startIndex, endIndex));
            if (endIndex <= startIndex) {
                responseString += CopyRequestResponseConfiguration.cutText();
                return responseString;
            }

            var selectedText = response.toByteArray().subArray(startIndex, endIndex);

            if (startIndex == response.bodyOffset() && endIndex < response.toByteArray().length()) {
                responseString += selectedText;
                responseString += CopyRequestResponseConfiguration.cutText();
                return responseString;
            }

            if (startIndex > response.bodyOffset() && endIndex == response.toByteArray().length()) {
                responseString += CopyRequestResponseConfiguration.cutText();
                responseString += selectedText;
                return responseString;
            }

            if (startIndex == response.bodyOffset() && endIndex == response.toByteArray().length()) {
                responseString += selectedText;
                return responseString;
            }

            responseString += CopyRequestResponseConfiguration.cutText();
            responseString += selectedText;
            responseString += CopyRequestResponseConfiguration.cutText();
            return responseString;
        };

        var text = "%s\n\n%s".formatted(requestString, responseStringSupplier.get());

        // Ugly hack because VMware is messing up the clipboard if a text is still selected, the function
        // has to be run in a separate thread which sleeps for 0.2 seconds.
        var thread = new Thread(() -> {
            try {
                Thread.sleep(200);
                toClipboard(text);
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setName("ToClipboardThread");
        thread.setDaemon(true);
        thread.start();
    }

    private static void toClipboard(String text0) {
        var text1 = text0.replaceAll("\r\n", "\n");
        var systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var systemSelection = Toolkit.getDefaultToolkit().getSystemSelection();
        var transferText = new StringSelection(text1);
        systemClipboard.setContents(transferText, null);
        systemSelection.setContents(transferText, null);
    }

    private CopyRequestResponseCopyActions() {
        // static class
    }

}
