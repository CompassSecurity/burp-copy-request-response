from burp import IBurpExtender, IContextMenuFactory, IHttpRequestResponse
from java.io import PrintWriter
from java.util import ArrayList
from javax.swing import JMenuItem
from java.awt import Toolkit
from java.awt.datatransfer import StringSelection
from javax.swing import JOptionPane
import subprocess
import tempfile
import threading
import time

class BurpExtender(IBurpExtender, IContextMenuFactory, IHttpRequestResponse):

    CUT_TEXT = "[...]"

    def str_to_array(self, string):
        return [ord(c) for c in string]

    def registerExtenderCallbacks(self, callbacks):
        callbacks.setExtensionName("Copy HTTP Request & Response")

        stdout = PrintWriter(callbacks.getStdout(), True)
        stderr = PrintWriter(callbacks.getStderr(), True)

        self.helpers = callbacks.getHelpers()
        self.callbacks = callbacks
        callbacks.registerContextMenuFactory(self)

    # Implement IContextMenuFactory
    def createMenuItems(self, invocation):
        self.context = invocation
        menuList = ArrayList()

        menuList.add(JMenuItem("Copy HTTP Request & Response (Full/Full)",
                actionPerformed=self.copyRequestFullResponseFull))
        menuList.add(JMenuItem("Copy HTTP Request & Response (Full/Header)",
                actionPerformed=self.copyRequestFullResponseHeader))
        menuList.add(JMenuItem("Copy HTTP Request & Response (Full/Header + Selected Data)",
                actionPerformed=self.copyRequestFullResponseHeaderData))

        return menuList

    def copyRequestFullResponseFull(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()

        data = self.stripTrailingNewlines(httpRequest)
        data.append(13) # Line Break
        data.append(13)
        data.extend(self.stripTrailingNewlines(httpResponse))

        self.copyToClipboard(data)

    def copyRequestFullResponseHeader(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()
        httpResponseBodyOffset = self.helpers.analyzeResponse(httpResponse).getBodyOffset()

        data = self.stripTrailingNewlines(httpRequest)
        data.append(13)
        data.append(13)
        data.extend(httpResponse[0:httpResponseBodyOffset])
        data.extend(self.str_to_array(self.CUT_TEXT))

        self.copyToClipboard(data)

    def copyRequestFullResponseHeaderData(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()
        httpResponseBodyOffset = self.helpers.analyzeResponse(httpResponse).getBodyOffset()
        selectionBounds = self.context.getSelectionBounds()
        httpResponseData = httpResponse[selectionBounds[0]:selectionBounds[1]]

        data = self.stripTrailingNewlines(httpRequest)
        data.append(13)
        data.append(13)
        data.extend(httpResponse[0:httpResponseBodyOffset])
        data.extend(self.str_to_array(self.CUT_TEXT))
        data.append(13)
        data.extend(self.stripTrailingNewlines(httpResponseData))
        data.append(13)
        data.extend(self.str_to_array(self.CUT_TEXT))

        # Ugly hack because VMware is messing up the clipboard if a text is still selected, the function
        # has to be run in a separate thread which sleeps for 1.5 seconds.
        t = threading.Thread(target=self.copyToClipboard, args=(data,True))
        t.start()

    def copyToClipboard(self, data, sleep=False):
        if sleep is True:
            time.sleep(1.5)

        # Fix line endings of the headers
        data = self.helpers.bytesToString(data).replace('\r\n', '\n')

        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        systemSelection = Toolkit.getDefaultToolkit().getSystemSelection()
        transferText = StringSelection(data)
        systemClipboard.setContents(transferText, None)
        systemSelection.setContents(transferText, None)

    def stripTrailingNewlines(self, data):
        while data[-1] in (10, 13):
            data = data[:-1]
        return data
