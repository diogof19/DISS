package com.ui.metrics;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.io.IOException;
import java.net.URL;

public class CustomResourceHandler implements CefResourceHandler {
    private ResourceHandlerState state = new ClosedConnection();

    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback cefCallback) {
        String url = cefRequest.getURL();
        String pathToResource = url.replace("http://myapp", "webview/");
        URL newUrl = getClass().getClassLoader().getResource(pathToResource);
        try {
            state = new OpenedConnection(newUrl.openConnection());
            cefCallback.Continue();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef intRef, StringRef stringRef) {
        state.getResponseHeaders(cefResponse, intRef, stringRef);
    }

    @Override
    public boolean readResponse(byte[] bytes, int i, IntRef intRef, CefCallback cefCallback) {
        return state.readResponse(bytes, i, intRef, cefCallback);
    }

    @Override
    public void cancel() {
        state.close();
        state = new ClosedConnection();
    }


}
