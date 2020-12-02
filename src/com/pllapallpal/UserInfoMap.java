package com.pllapallpal;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class UserInfoMap {

    private Map<SocketChannel, UserInfo> userMap;

    private UserInfoMap() {
        userMap = new HashMap<>();
    }

    private static class UserStatusHolder {
        public static UserInfoMap instance = new UserInfoMap();
    }

    public static UserInfoMap getInstance() {
        return UserStatusHolder.instance;
    }

    public Map<SocketChannel, UserInfo> getUserMap() {
        return userMap;
    }
}
