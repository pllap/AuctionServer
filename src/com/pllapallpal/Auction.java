package com.pllapallpal;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Auction {

    private String key;
    private String creatorName;
    private BufferedImage itemImage;
    private String itemName;
    private int startingPrice;
    private int leftTime;

    ScheduledExecutorService executor;
    public static Runnable onUpdateAuctionList;

    private List<SocketChannel> userSocketChannelList;

    public Auction() {
        byte[] array = new byte[8];
        new Random().nextBytes(array);
        key = new String(array, StandardCharsets.UTF_8);
        userSocketChannelList = Collections.synchronizedList(new ArrayList<>());
        leftTime = 60;

        Runnable timer = () -> {

            if (leftTime > 1) {
                --leftTime;

                int capacity = Integer.BYTES + Integer.BYTES; // protocol + leftTime
                ByteBuffer capacityBuffer = ByteBuffer.allocate(Integer.BYTES);
                capacityBuffer.putInt(capacity);
                capacityBuffer.flip();

                ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
                byteBuffer.putInt(Protocol.AUCTION_LEFT_TIME);
                byteBuffer.putInt(leftTime);
                byteBuffer.flip();

                try {
                    for (SocketChannel socketChannel : userSocketChannelList) {
                        socketChannel.write(capacityBuffer);
                        socketChannel.write(byteBuffer);

                        capacityBuffer.rewind();
                        byteBuffer.rewind();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Left time for " + itemName + ": " + leftTime);
            } else {
                int capacity = Integer.BYTES; // protocol
                ByteBuffer capacityBuffer = ByteBuffer.allocate(Integer.BYTES);
                capacityBuffer.putInt(capacity);
                capacityBuffer.flip();

                ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
                byteBuffer.putInt(Protocol.AUCTION_QUIT);
                byteBuffer.flip();

                try {
                    for (SocketChannel socketChannel : userSocketChannelList) {
                        socketChannel.write(capacityBuffer);
                        socketChannel.write(byteBuffer);

                        capacityBuffer.rewind();
                        byteBuffer.rewind();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AuctionList.getInstance().getAuctionList().remove(this);
                stop();
            }
        };
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
    }

    private void stop() {
        executor.shutdownNow();
        onUpdateAuctionList.run();
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public BufferedImage getItemImage() {
        return itemImage;
    }

    public void setItemImage(BufferedImage itemImage) {
        this.itemImage = itemImage;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(int startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getKey() {
        return key;
    }

    public int getBytesNumber() throws IOException {

        int bytes = 0;

        byte[] byteKey = this.key.getBytes();
        bytes = bytes + Integer.BYTES + byteKey.length;

        byte[] byteCreatorName = this.creatorName.getBytes();
        bytes = bytes + Integer.BYTES + byteCreatorName.length; // creator name length + item name data

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(this.itemImage, "png", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        byte[] byteItemImage = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        bytes = bytes + Integer.BYTES + byteItemImage.length; // image length + image bytes data

        byte[] itemNameBytes = this.itemName.getBytes();
        bytes = bytes + Integer.BYTES + itemNameBytes.length; // item name length + item name data

        bytes = bytes + Integer.BYTES; // starting price

        return bytes;
    }

    public static void addOnUpdateAuctionList(Runnable onUpdateAuctionList) {
        Auction.onUpdateAuctionList = onUpdateAuctionList;
    }

    public void addUser(SocketChannel userSocketChannel) {
        userSocketChannelList.add(userSocketChannel);
    }

    public void removeUser(SocketChannel userSocketChannel) {
        userSocketChannelList.remove(userSocketChannel);
    }

    public boolean contains(SocketChannel userSocketChannel) {
        return userSocketChannelList.contains(userSocketChannel);
    }

    public List<SocketChannel> getUserSocketChannelList() {
        return userSocketChannelList;
    }
}
