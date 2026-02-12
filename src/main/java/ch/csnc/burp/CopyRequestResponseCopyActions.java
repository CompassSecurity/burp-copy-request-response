package ch.csnc.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CopyRequestResponseCopyActions {

  public static void copyFullFull(List<HttpRequestResponse> requestResponses) {
    requestResponses = requestResponses.stream().map(CopyRequestResponseCopyActions::hideHeaders).toList();

    var text = requestResponses.stream()
        .map(requestResponse -> {
          var requestString = requestResponse.request().toString().strip();
          var responseString = Optional.ofNullable(requestResponse.response())
              .map(HttpResponse::toString)
              .map(String::strip)
              .orElse("");
          return format(requestString, responseString);
        })
        .collect(Collectors.joining("\n\n"));

    toClipboard(text);
  }

  public static void copyFullHeader(List<HttpRequestResponse> requestResponses) {
    requestResponses = requestResponses.stream().map(CopyRequestResponseCopyActions::hideHeaders).toList();

    var text = requestResponses
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
          return format(requestString, responseString);
        })
        .collect(Collectors.joining("\n\n"));

    toClipboard(text);
  }

  public static void copyFullHeaderPlusSelectedData(MessageEditorHttpRequestResponse editor) {
    var requestResponse = hideHeaders(editor.requestResponse());

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

    var text = format(requestString, responseStringSupplier.get());

    // Ugly hack because VMware is messing up the clipboard if a text is still
    // selected, the function
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

  private static HttpRequestResponse hideHeaders(HttpRequestResponse requestResponse) {
    var request = requestResponse.request();

    for (var header : request.headers()) {
      for (var pattern : CopyRequestResponseConfiguration.hideRequestHeaders()) {
        if (pattern.matcher(header.name()).matches()) {
          request = request.withRemovedHeader(header);
        }
      }
    }

    var response = requestResponse.response();
    if (response != null) {
      for (var header : response.headers()) {
        for (var pattern : CopyRequestResponseConfiguration.hideResponseHeaders()) {
          if (pattern.matcher(header.name()).matches()) {
            response = response.withRemovedHeader(header);
          }
        }
      }
    }

    return HttpRequestResponse.httpRequestResponse(request, response);
  }

  private static String format(String request, String response) {
    return CopyRequestResponseConfiguration.template()
        .replace("\\n", "\n")
        .replace("{request}", request)
        .replace("{response}", response);
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
