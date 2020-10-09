from burp import IBurpExtender, IContextMenuFactory, IHttpRequestResponse
from java.io import PrintWriter
from java.util import ArrayList
from javax.swing import JMenuItem
import subprocess
import tempfile
import threading
import time

class BurpExtender(IBurpExtender, IContextMenuFactory, IHttpRequestResponse):

    CUT_TEXT = [ord(c) for c in "[...]"]

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
                actionPerformed=self.copyRequestFullResponseFull))
        menuList.add(JMenuItem("Copy HTTP Request & Response (Full/Header + Selected Data)",
                actionPerformed=self.copyRequestFullResponseHeaderData))

        return menuList

    def copyRequestFullResponseFull(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()
        data = httpRequest + httpResponse
        self.copyToClipboard(data)

    def copyRequestFullResponseHeader(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()
        httpResponseBodyOffset = self.helpers.analyzeResponse(httpResponse).getBodyOffset()
        data = httpRequest + httpResponse[0:httpResponseBodyOffset]
        data.extend(self.CUT_TEXT)
        self.copyToClipboard(data)

    def copyRequestFullResponseHeaderData(self, event):
        httpTraffic = self.context.getSelectedMessages()[0]
        httpRequest = httpTraffic.getRequest()
        httpResponse = httpTraffic.getResponse()
        httpResponseBodyOffset = self.helpers.analyzeResponse(httpResponse).getBodyOffset()
        selectionBounds = self.context.getSelectionBounds()
        httpResponseData = httpResponse[selectionBounds[0]:selectionBounds[1]]
        data = httpRequest + httpResponse[0:httpResponseBodyOffset]
        data.extend(self.CUT_TEXT)
        data.append(13) # Line Break
        data.extend(httpResponseData)
        data.append(13)
        data.extend(self.CUT_TEXT)

        # Ugly hack because VMware is messing up the clipboard if a text is still selected, the function
        # has to be run in a separate thread which sleeps for 2 seconds.
        t = threading.Thread(target=self.copyToClipboard, args=(data,True))
        t.start()

    def copyToClipboard(self, data, sleep=False):
        if sleep is True:
            time.sleep(2)

        # Fix line endings of the headers
        data = self.helpers.bytesToString(data).replace('\r\n', '\n')

        temp = tempfile.TemporaryFile()
        temp.write(data)
        temp.seek(0)
        subprocess.Popen(["xclip", "-in", "-selection", "primary"], stdin=temp)
        temp.seek(0)
        subprocess.Popen(["xclip", "-in", "-selection", "secondary"], stdin=temp)
        temp.seek(0)
        subprocess.Popen(["xclip", "-in", "-selection", "clipboard"], stdin=temp)
        temp.close()

        os.system("notify-send 'Copied to clipboard!'")
