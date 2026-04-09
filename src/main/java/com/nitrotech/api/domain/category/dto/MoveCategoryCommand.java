package com.nitrotech.api.domain.category.dto;

import java.util.List;

public record MoveCategoryCommand(
        Long movedId,
        Long fromParentId,          // null = từ root
        Long toParentId,            // null = move lên root
        List<Long> sourceOrderedIds,  // thứ tự mới của siblings ở parent cũ
        List<Long> targetOrderedIds   // thứ tự mới của siblings ở parent mới
) {}
