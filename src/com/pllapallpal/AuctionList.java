package com.pllapallpal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionList {
    private List<Auction> auctionList;

    private AuctionList() {
        auctionList = Collections.synchronizedList(new ArrayList<>());
    }

    private static class AuctionListHolder {
        public static AuctionList instance = new AuctionList();
    }

    public static AuctionList getInstance() {
        return AuctionListHolder.instance;
    }

    public List<Auction> getAuctionList() {
        return auctionList;
    }
}
