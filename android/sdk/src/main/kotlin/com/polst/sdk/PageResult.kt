package com.polst.sdk

public data class PageResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
