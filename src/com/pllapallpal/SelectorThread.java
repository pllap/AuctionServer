package com.pllapallpal;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SelectorThread implements Runnable {

    private SelectionKey selectionKey;
    private Selector selector;
    private List<SocketChannel> socketChannelList;
    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    @Override
    public void run() {
        System.out.println("Server start");
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keys = selectedKeys.iterator();
                while (keys.hasNext()) {
                    SelectionKey selectionKey = keys.next();
                    keys.remove();
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
                        UserInfoMap.getInstance().getUserMap().put(socketChannel, new UserInfo());
                    } else if (selectionKey.isReadable()) {

                        SocketChannel clientSocketChannel = (SocketChannel) selectionKey.channel();
                        ByteBuffer receivedByteBuffer = read(selectionKey);
                        Map<SocketChannel, UserInfo> userMap = UserInfoMap.getInstance().getUserMap();
                        int protocol = receivedByteBuffer.getInt();
                        switch (protocol) {
                            case Protocol.LOGIN: {
                                String username = decoder.decode(receivedByteBuffer).toString();
                                userMap.get(clientSocketChannel).setUsername(username);
                                System.out.println("Client " + username + " has connected.");

                                ByteBuffer[] userListBuffers = makeUserList();
                                ByteBuffer userListCapacityBuffer = userListBuffers[0];
                                ByteBuffer userListBuffer = userListBuffers[1];
                                broadcast(userListCapacityBuffer);
                                broadcast(userListBuffer);

                                ByteBuffer[] auctionListBuffers = makeAuctionList();
                                ByteBuffer auctionListCapacityBuffer = userListBuffers[0];
                                ByteBuffer auctionListBuffer = userListBuffers[1];
                                write(clientSocketChannel, auctionListCapacityBuffer);
                                write(clientSocketChannel, auctionListBuffer);
                                break;
                            }
                            case Protocol.LOGOUT: {
                                ByteBuffer[] userListBuffers = makeUserList();
                                ByteBuffer capacityBuffer = userListBuffers[0];
                                ByteBuffer userListBuffer = userListBuffers[1];
                                broadcast(capacityBuffer);
                                broadcast(userListBuffer);
                                break;
                            }
                            case Protocol.LIST_AUCTION: {
                                break;
                            }
                            case Protocol.LIST_USER: {
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return ByteBuffer which is read from socketChannel
     */
    private ByteBuffer read(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer capacityBuffer = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer byteBuffer = null;
        try {
            socketChannel.read(capacityBuffer);
            capacityBuffer.flip();
            int capacity = capacityBuffer.getInt();

            byteBuffer = ByteBuffer.allocate(capacity);
            while (byteBuffer.hasRemaining()) {
                synchronized (socketChannel) {
                    if (socketChannel.isConnected()) {
                        socketChannel.read(byteBuffer);
                    }
                }
            }
            byteBuffer.flip();
        } catch (IOException e) {
            try {
                // Handle client who terminated connection
                System.out.println("Client " + UserInfoMap.getInstance().getUserMap().get(socketChannel).getUsername() + " has left the server.");
                socketChannel.close();
                socketChannelList.remove(socketChannel);
                UserInfoMap.getInstance().getUserMap().remove(socketChannel);
                ByteBuffer logoutBuffer = ByteBuffer.allocate(Integer.BYTES);
                logoutBuffer.putInt(Protocol.LOGOUT);
                logoutBuffer.flip();
                return logoutBuffer;
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }

        return byteBuffer;
    }

    private ByteBuffer[] makeUserList() {

        // capacityBuffer, byteBuffer
        ByteBuffer[] byteBuffers = new ByteBuffer[2];

        // Put userList information into bytebuffer
        // Structure: ByteBuffer capacity - Protocol - List Size - List Data(String Bytes Length - String Bytes - ...)
        Map<SocketChannel, UserInfo> userMap = UserInfoMap.getInstance().getUserMap();
        int capacity = Integer.BYTES + Integer.BYTES; // protocol bytes length + list size bytes length
        for (Map.Entry<SocketChannel, UserInfo> socketChannelUserInfoEntry : userMap.entrySet()) {
            int nameBytesLength = socketChannelUserInfoEntry.getValue().getUsername().getBytes().length;
            capacity = capacity + Integer.BYTES + nameBytesLength; // name length + name bytes data
        }
        byteBuffers[0] = ByteBuffer.allocate(Integer.BYTES);
        byteBuffers[0].putInt(capacity);
        byteBuffers[0].flip();

        byteBuffers[1] = ByteBuffer.allocate(capacity);
        byteBuffers[1].putInt(Protocol.LIST_USER);
        byteBuffers[1].putInt(userMap.size());
        for (Map.Entry<SocketChannel, UserInfo> socketChannelUserInfoEntry : userMap.entrySet()) {
            byte[] listItem = socketChannelUserInfoEntry.getValue().getUsername().getBytes();
            byteBuffers[1].putInt(listItem.length);
            byteBuffers[1].put(listItem);
        }
        byteBuffers[1].flip();
        return byteBuffers;
    }

    private ByteBuffer[] makeAuctionList() {

        // capacityBuffer, byteBuffer
        ByteBuffer[] byteBuffers = new ByteBuffer[2];

        // Structure: Protocol - List Size - Byte Image Length - Byte Image - Byte Name Length - Byte Name - Byte Image Length - Byte Image - Byte Name Length - Byte Name - ...
        List<Auction> auctionList = AuctionList.getInstance().getAuctionList();

        int capacity = Integer.BYTES + Integer.BYTES; // protocol bytes length + auction list length bytes length
        try {
            for (Auction auction : auctionList) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(auction.getItemImage(), "png", byteArrayOutputStream);
                byteArrayOutputStream.flush();
                byte[] itemImageBytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                capacity = capacity + Integer.BYTES + itemImageBytes.length; // image length + image bytes data

                byte[] itemNameBytes = auction.getItemName().getBytes();
                capacity = capacity + Integer.BYTES + itemNameBytes.length; // item name length + item name data
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        byteBuffers[0] = ByteBuffer.allocate(Integer.BYTES);
        byteBuffers[0].putInt(capacity);
        byteBuffers[0].flip();

        byteBuffers[1] = ByteBuffer.allocate(capacity);
        byteBuffers[1].putInt(Protocol.LIST_AUCTION);
        byteBuffers[1].putInt(auctionList.size());
        try {
            for (Auction auction : auctionList) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(auction.getItemImage(), "png", byteArrayOutputStream);
                byteArrayOutputStream.flush();
                byte[] byteImage = byteArrayOutputStream.toByteArray();
                byteBuffers[1].putInt(byteImage.length);
                byteBuffers[1].put(byteImage);
                byteArrayOutputStream.close();
                byteBuffers[1].putInt(auction.getItemName().getBytes().length);
                byteBuffers[1].put(auction.getItemName().getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byteBuffers[1].flip();

        return byteBuffers;
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

    /**
     * @param socketChannel SocketChannel that data will be written
     * @param byteBuffer    Buffer that will be sent to the SocketChannel
     */
    private void write(SocketChannel socketChannel, ByteBuffer byteBuffer) {
        try {
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
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
