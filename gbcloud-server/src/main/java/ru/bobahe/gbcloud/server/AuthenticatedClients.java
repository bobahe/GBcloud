package ru.bobahe.gbcloud.server;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuthenticatedClients {
    private static AuthenticatedClients ourInstance = new AuthenticatedClients();
    public ConcurrentMap<String, Channel> clients = new ConcurrentHashMap<>();

    public static AuthenticatedClients getInstance() {
        return ourInstance;
    }

    private AuthenticatedClients() {
    }
}
