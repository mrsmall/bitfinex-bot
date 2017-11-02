package com.klein.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class CustomWebsocketClient extends WebSocketClient {
    private static final Logger LOG= LoggerFactory.getLogger(CustomWebsocketClient.class);

    private WebSocketListener listener;


    private CustomWebsocketClient(URI serverUri, Map<String, String> httpHeaders, WebSocketListener listener) {
        super(serverUri, new Draft_6455(), httpHeaders, 60000);
        this.listener = listener;
        try {
            setSocket(SSLSocketFactory.getDefault().createSocket(serverUri.getHost(), serverUri.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        connect();
    }


    public CustomWebsocketClient(String url, WebSocketListener listener) {
        this(URI.create(url), new HashMap<>(), listener);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOG.trace("Connected");
        if (listener!=null){
            listener.onOpen(handshakedata);
        }
    }

    @Override
    public void onMessage(String message) {
        LOG.trace("onMessage: {}", message);
        if (listener!=null){
            listener.onMessage(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.trace("onClose, code: {}, reason: {}, remote: {}", code, reason, remote);
        if (listener!=null){
            listener.onClose(code, reason, remote);
        }
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("onError: "+ex.getMessage(), ex);
        if (listener!=null){
            listener.onError(ex);
        }
    }
}
