package com.pllapallpal;

import java.awt.image.BufferedImage;

public class Auction {
    private String itemName;
    private BufferedImage itemImage;

    public Auction() {

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
}
