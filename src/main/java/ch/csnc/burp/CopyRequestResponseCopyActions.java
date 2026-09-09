package ch.csnc.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CopyRequestResponseCopyActions {

  private static final int DEFAULT_INDENT_WIDTH = 4;
  private static final int MAX_INDENT_WIDTH = 64;

  /**
   * Matches {request}, {response}, {request:indent}, {response:indent=2},
   * {request:indent=\t} and alike.
   */
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(request|response)(:indent(?:=([^}]*))?)?\\}");

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
    // Control characters are expanded on the template only, never on the
    // request/response payload itself.
    var template = unescape(CopyRequestResponseConfiguration.template());

    var matcher = PLACEHOLDER.matcher(template);
    var text = new StringBuilder();
    while (matcher.find()) {
      var value = "request".equals(matcher.group(1)) ? request : response;
      if (matcher.group(2) != null) {
        value = indent(value, indentPrefix(matcher.group(3)));
      }
      // quoteReplacement, so that a payload containing $ or \ is copied verbatim
      // and a payload containing a placeholder is not substituted again.
      matcher.appendReplacement(text, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(text);

    return text.toString();
  }

  /**
   * Prefixes every non-empty line with the given prefix. Empty lines are kept
   * empty, so that no trailing whitespace is copied.
   */
  private static String indent(String text, String prefix) {
    if (text.isEmpty()) {
      return text;
    }
    return text.lines()
        .map(line -> line.isEmpty() ? line : prefix + line)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Resolves the argument of an {@code :indent} placeholder: no argument means
   * four spaces, a number means that many spaces, anything else is taken
   * literally (control characters such as \t are expanded).
   */
  private static String indentPrefix(String argument) {
    String prefix;
    if (argument == null || argument.isEmpty()) {
      prefix = " ".repeat(DEFAULT_INDENT_WIDTH);
    } else if (argument.chars().allMatch(Character::isDigit)) {
      var width = argument.length() > 2 ? MAX_INDENT_WIDTH : Integer.parseInt(argument);
      prefix = " ".repeat(Math.min(width, MAX_INDENT_WIDTH));
    } else {
      prefix = unescape(argument);
    }
    if (CopyRequestResponseConfiguration.useNonBreakableSpace()) {
      prefix = prefix.replace(' ', '\u00a0');
    }
    return prefix;
  }

  /**
   * Expands the control characters \n, \r, \t, \0 and the escaped backslash \\.
   * Unknown escape sequences are kept as they are.
   */
  private static String unescape(String text) {
    var unescaped = new StringBuilder(text.length());
    for (var index = 0; index < text.length(); index++) {
      var character = text.charAt(index);
      if (character != '\\' || index + 1 >= text.length()) {
        unescaped.append(character);
        continue;
      }
      var escaped = text.charAt(++index);
      switch (escaped) {
        case 'n' -> unescaped.append('\n');
        case 'r' -> unescaped.append('\r');
        case 't' -> unescaped.append('\t');
        case '0' -> unescaped.append('\0');
        case '\\' -> unescaped.append('\\');
        default -> unescaped.append('\\').append(escaped);
      }
    }
    return unescaped.toString();
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
