package tz.market.api;

public record AuthUser(long userId, int roleId, String username, String name) {}
