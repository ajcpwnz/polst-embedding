package com.polst.sdk.core.tokens

public interface TokenStore {
    public suspend fun read(): TokenSnapshot?
    public suspend fun write(snapshot: TokenSnapshot)
    public suspend fun clear()
}
