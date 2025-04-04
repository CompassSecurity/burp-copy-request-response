package ch.csnc.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import static java.util.Objects.requireNonNull;

public class CopyRequestResponseExtension implements BurpExtension {

    private static MontoyaApi api;

    public static MontoyaApi api() {
        return api;
    }

    @Override
    public void initialize(MontoyaApi api) {
        CopyRequestResponseExtension.api = api;
        api.extension().setName("Copy Request Response");
        api.userInterface().registerContextMenuItemsProvider(new CopyRequestResponseContextMenuItemsProvider());

        CopyRequestResponseHotKeyHandler.register();

        api.logging().logToOutput("Copy Request Response loaded");

        var versionTxt = "/version.txt";
        try (var stream = getClass().getResourceAsStream(versionTxt)) {
            var reader = new BufferedReader(new InputStreamReader(requireNonNull(stream, versionTxt)));
            reader.lines().forEach(api.logging()::logToOutput);
        } catch (Exception exc) {
            api.logging().logToError("Could not read %s".formatted(versionTxt));
            api.logging().logToError(exc);
        }
    }
}
