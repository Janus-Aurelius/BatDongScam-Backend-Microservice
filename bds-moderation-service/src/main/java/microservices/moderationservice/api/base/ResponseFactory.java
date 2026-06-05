package microservices.moderationservice.api.base;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFactory {
    public <T> ResponseEntity<ApiResponse<T>> successSingle(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>(true, message, data);
        return ResponseEntity.ok(response);
    }

    public <T> ResponseEntity<ApiResponse<PagedData<T>>> successPage(Page<T> page, String message) {
        PagedData<T> pagedData = PagedData.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        ApiResponse<PagedData<T>> response = new ApiResponse<>(true, message, pagedData);
        return ResponseEntity.ok(response);
    }

    public <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, String error) {
        ApiResponse<T> response = new ApiResponse<>(false, message, null);
        return ResponseEntity.status(status).body(response);
    }
}
