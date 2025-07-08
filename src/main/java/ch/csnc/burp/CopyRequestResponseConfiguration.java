package ch.csnc.burp;

public class CopyRequestResponseConfiguration {

    private static final String CUT_TEXT_KEY = "CopyRequestResponseCutText";
    private static final String CUT_TEXT_NBSP_KEY = "CopyRequestResponseCutTextNbsp";
    private static final String COPY_FULL_FULL_OR_SELECTION_KEY = "CopyRequestResponseCopyFullFullOrSelectionHotKey";
    private static final String COPY_FULL_HEADER_KEY = "CopyRequestResponseCopyFullHeaderHotKey";
    private static final String ENABLE_JSON_FORMATTING_KEY = "CopyRequestResponseEnableJSONFormatting";

    public static String cutText() {
        var cutText = CopyRequestResponseExtension.api().persistence().preferences().getString(CUT_TEXT_KEY);
        if (cutText == null) {
            cutText = "[...]";
            setCutText(cutText);
        }
        if (useNonBreakableSpace()) {
            cutText = cutText.replaceAll(" ", "\u00a0");
        }
        return cutText;
    }

    public static void setCutText(String cutText) {
        CopyRequestResponseExtension.api().persistence().preferences().setString(CUT_TEXT_KEY, cutText);
    }

    public static boolean useNonBreakableSpace() {
        var useNbsp = CopyRequestResponseExtension.api().persistence().preferences().getBoolean(CUT_TEXT_NBSP_KEY);
        if (useNbsp == null) {
            useNbsp = false;
        }
        setUseNonBreakableSpace(useNbsp);
        return useNbsp;
    }

    public static void setUseNonBreakableSpace(boolean enabled) {
        CopyRequestResponseExtension.api().persistence().preferences().setBoolean(CUT_TEXT_NBSP_KEY, enabled);
    }

    public static String copyFullFullOrSelectionHotKey() {
        var hotKey = CopyRequestResponseExtension.api().persistence().preferences().getString(COPY_FULL_FULL_OR_SELECTION_KEY);
        if (hotKey == null) {
            hotKey = "Ctrl+Shift+C";
            setCopyFullFullOrSelectionHotKey(hotKey);
        }
        return hotKey;
    }

    public static void setCopyFullFullOrSelectionHotKey(String hotKey) {
        CopyRequestResponseExtension.api().persistence().preferences().setString(COPY_FULL_FULL_OR_SELECTION_KEY, hotKey);
    }

    public static String copyFullHeaderHotKey() {
        var hotKey = CopyRequestResponseExtension.api().persistence().preferences().getString(COPY_FULL_HEADER_KEY);
        if (hotKey == null) {
            hotKey = "Ctrl+Alt+C";
            setCopyFullHeaderHotKey(hotKey);
        }
        return hotKey;
    }

    public static void setCopyFullHeaderHotKey(String hotKey) {
        CopyRequestResponseExtension.api().persistence().preferences().setString(COPY_FULL_HEADER_KEY, hotKey);
    }

    public static boolean enableJsonFormatting() {
        var enableJson = CopyRequestResponseExtension.api().persistence().preferences().getBoolean(ENABLE_JSON_FORMATTING_KEY);
        if (enableJson == null) {
            enableJson = false;
        }
        setEnableJsonFormatting(enableJson);
        return enableJson;
    }

    public static void setEnableJsonFormatting(boolean enabled) {
        CopyRequestResponseExtension.api().persistence().preferences().setBoolean(ENABLE_JSON_FORMATTING_KEY, enabled);
    }
    
    private CopyRequestResponseConfiguration() {
        // static class
    }

}
