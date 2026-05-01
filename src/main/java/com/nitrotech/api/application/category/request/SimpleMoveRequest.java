package com.nitrotech.api.application.category.request;

public record SimpleMoveRequest(
        Long newParentId,  // null = move to root, không truyền = giữ nguyên parent
        Long afterId       // null = đầu tiên, không truyền = cuối cùng
) {}
