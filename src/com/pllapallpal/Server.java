package com.pllapallpal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Server {

    ServerSocketChannel serverSocketChannel = null;
    Selector selector = null;
    SelectionKey selectionKey = null;
    List<SocketChannel> socketChannelList = new ArrayList<>();

    public Server(final int PORT) {
        initServer(PORT);
        SelectorThread selectorThread = new SelectorThread();
        selectorThread.setSelectionKey(selectionKey);
        selectorThread.setSelector(selector);
        selectorThread.setSocketChannelList(socketChannelList);
        new Thread(selectorThread).start();
    }

    private void initServer(final int PORT) {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(PORT));

            selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
