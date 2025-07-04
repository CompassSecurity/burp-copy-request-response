package ch.csnc.burp;

import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;

public class CopyRequestResponseContextMenuItemsProvider implements ContextMenuItemsProvider {

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        var requestResponses =
                event.messageEditorRequestResponse()
                        .map(MessageEditorHttpRequestResponse::requestResponse)
                        .map(List::of)
                        .orElseGet(event::selectedRequestResponses);

        var menuItems = new ArrayList<Component>();

        var copyFullFull = new JMenuItem("Copy HTTP Request & Response (Full/Full)");
        copyFullFull.addActionListener(actionEvent -> CopyRequestResponseCopyActions.copyFullFull(requestResponses));
        menuItems.add(copyFullFull);

        var copyFullHeader = new JMenuItem("Copy HTTP Request & Response (Full/Header)");
        copyFullHeader.addActionListener(actionEvent -> CopyRequestResponseCopyActions.copyFullHeader(requestResponses));
        menuItems.add(copyFullHeader);

        if (event.messageEditorRequestResponse().map(MessageEditorHttpRequestResponse::selectionContext).orElse(null) == MessageEditorHttpRequestResponse.SelectionContext.RESPONSE) {
            var copyFullHeaderPlusSelectedData = new JMenuItem("Copy HTTP Request & Response (Full/Header + Selected Data)");
            copyFullHeaderPlusSelectedData.addActionListener(actionEvent -> CopyRequestResponseCopyActions.copyFullHeaderPlusSelectedData(event.messageEditorRequestResponse().orElseThrow()));
            menuItems.add(copyFullHeaderPlusSelectedData);
        }

        var openConfiguration = new JMenuItem("Open Configuration");
        openConfiguration.addActionListener(actionEvent -> new CopyRequestResponseConfigurationDialog().show());
        menuItems.add(openConfiguration);

        return List.copyOf(menuItems);
    }

}
