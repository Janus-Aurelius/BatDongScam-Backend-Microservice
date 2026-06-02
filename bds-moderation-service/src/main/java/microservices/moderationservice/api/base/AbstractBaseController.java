package microservices.moderationservice.api.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class AbstractBaseController {
    @Autowired
    protected ResponseFactory responseFactory;

    public static Pageable createPageable(int page, int limit, String sortType, String sortBy) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        int offset = (safePage - 1) * safeLimit;
        int pageNumber = offset / safeLimit;

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(pageNumber, safeLimit);
        }

        Sort.Direction direction = (sortType != null && sortType.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(pageNumber, safeLimit, sort);
    }
}
