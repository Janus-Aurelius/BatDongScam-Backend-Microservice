package com.se100.bds.controllers.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class AbstractBaseController {

    @Autowired
    protected ResponseFactory responseFactory;

    public static Pageable createPageable(int page, int limit, String sortType, String sortBy) {
        int offset = (page - 1) * limit;
        int pageNumber = offset / limit;

        // If sortBy is null, return unsorted pageable
        if (sortBy == null) {
            return PageRequest.of(pageNumber, limit);
        }

        Sort.Direction direction = (sortType != null && sortType.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = !sortBy.isEmpty() ? sortBy : "id";
        Sort sort = Sort.by(direction, sortField);

        return PageRequest.of(pageNumber, limit, sort);
    }

    // Other common methods for controllers can be added here...
}
