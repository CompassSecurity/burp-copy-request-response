package ch.csnc.burp;

import burp.api.montoya.ui.settings.SettingsPanelBuilder;
import burp.api.montoya.ui.settings.SettingsPanelPersistence;
import burp.api.montoya.ui.settings.SettingsPanelSetting;
import burp.api.montoya.ui.settings.SettingsPanelWithData;

public class CopyRequestResponseConfiguration {

  private static final String CUT_TEXT_LABEL = "Cut Text";
  private static final String CUT_TEXT_DEFAULT = "[...]";
  private static final String COPY_FULL_FULL_OR_SELECTION_HOT_KEY_LABEL = "HotKey for Copy Full/Full or Full/Header + Selected Data";
  private static final String COPY_FULL_FULL_OR_SELECTION_HOT_KEY_DEFAULT = "Ctrl+Shift+C";
  private static final String COPY_FULL_HEADER_LABEL = "HotKey for Copy Full/Header";
  private static final String COPY_FULL_HEADER_DEFAULT = "Ctrl+Shift+Alt+C";
  private static final String USE_NBSP_LABEL = "Use Non-Breakable Spaces";
  private static final boolean USE_NBSP_DEFAULT = false;
  private static final String TEMPLATE_LABEL = "Template";
  private static final String TEMPLATE_DEFAULT = "%s%n%n%s";

  private final static SettingsPanelSetting[] settings = new SettingsPanelSetting[] {
      SettingsPanelSetting.stringSetting(CUT_TEXT_LABEL, CUT_TEXT_DEFAULT),
      SettingsPanelSetting.booleanSetting(USE_NBSP_LABEL, USE_NBSP_DEFAULT),
      SettingsPanelSetting.stringSetting(COPY_FULL_FULL_OR_SELECTION_HOT_KEY_LABEL, COPY_FULL_FULL_OR_SELECTION_HOT_KEY_DEFAULT),
      SettingsPanelSetting.stringSetting(COPY_FULL_HEADER_LABEL, COPY_FULL_HEADER_DEFAULT),
      SettingsPanelSetting.stringSetting(TEMPLATE_LABEL, TEMPLATE_DEFAULT),
  };

  private final static SettingsPanelWithData panel = SettingsPanelBuilder.settingsPanel()
      .withPersistence(SettingsPanelPersistence.USER_SETTINGS)
      .withTitle("CopyRequestResponse Configuration")
      .withDescription("The extension's behavior can be customized here.")
      .withKeywords("CopyRequestResponse", "Settings", "CutText")
      .withSettings(settings)
      .build();

  public static String cutText() {
    var cutText = panel.getString(CUT_TEXT_LABEL);
    if (useNonBreakableSpace()) {
      cutText = cutText.replaceAll(" ", "\u00a0");
    }
    return cutText;
  }

  public static boolean useNonBreakableSpace() {
    return panel.getBoolean(USE_NBSP_LABEL);
  }

  public static String copyFullFullOrSelectionHotKey() {
    return panel.getString(COPY_FULL_FULL_OR_SELECTION_HOT_KEY_LABEL);
  }

  public static String copyFullHeaderHotKey() {
    return panel.getString(COPY_FULL_HEADER_LABEL);
  }

  public static String template() {
    return panel.getString(TEMPLATE_LABEL);
  }

  public static void register() {
    CopyRequestResponseExtension.api().userInterface().registerSettingsPanel(panel);
  }

  private CopyRequestResponseConfiguration() {
    // static class
  }
}
