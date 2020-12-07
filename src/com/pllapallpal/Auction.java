package com.pllapallpal;

import java.awt.image.BufferedImage;
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
}
