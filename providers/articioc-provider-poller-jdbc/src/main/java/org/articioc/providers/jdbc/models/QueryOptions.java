package org.articioc.providers.jdbc.models;

public record QueryOptions(String tableName, int limit, String providerId) {}
