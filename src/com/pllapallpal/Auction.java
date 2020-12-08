package com.pllapallpal;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Auction {

    private String key;
    private String creatorName;
    private BufferedImage itemImage;
    private String itemName;
    private int startingPrice;

    public Auction() {
        byte[] array = new byte[8];
        new Random().nextBytes(array);
        key = new String(array, StandardCharsets.UTF_8);
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
}
