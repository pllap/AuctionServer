package com.pllapallpal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SelectorThread implements Runnable {

    private SelectionKey selectionKey;
    private Selector selector;
    private List<SocketChannel> socketChannelList;

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keys = selectedKeys.iterator();
                while (keys.hasNext()) {
                    SelectionKey selectionKey = keys.next();
                    if (selectionKey.isAcceptable()) {
                        // accept
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        if (Objects.isNull(socketChannel)) {
                            return;
                        }

                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        socketChannelList.add(socketChannel);
                    } else if (selectionKey.isReadable()) {
                        ByteBuffer byteBuffer = read(selectionKey);
                        System.out.println(StandardCharsets.UTF_8.newDecoder().decode(byteBuffer).toString()); // DEBUG
                        broadcast(byteBuffer);
                    }
                    // 사용한 키는 제거
                    keys.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return Data which is read from socketChannel
     */
    private ByteBuffer read(SelectionKey selectionKey) {
        SocketChannel socketChannelRead = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            socketChannelRead.read(byteBuffer);
        } catch (IOException e) {
            try {
                socketChannelRead.close();
                socketChannelList.remove(socketChannelRead);
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }

        byteBuffer.flip();
        return byteBuffer;
    }

    /**
     * @param byteBuffer Data that will be broadcast
     */
    private void broadcast(ByteBuffer byteBuffer) {
        for (SocketChannel socketChannelBroadcast : socketChannelList) {
            try {
                socketChannelBroadcast.write(byteBuffer);
            } catch (IOException e) {
                try {
                    socketChannelBroadcast.close();
                    socketChannelList.remove(socketChannelBroadcast);
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
            byteBuffer.rewind();
        }
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public void setSocketChannelList(List<SocketChannel> socketChannelList) {
        this.socketChannelList = socketChannelList;
    }
}
