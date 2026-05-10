package microservices.moderationservice.api.base;

import microservices.moderationservice.api.response.ErrorResponse;
import microservices.moderationservice.api.response.PageResponse;
import microservices.moderationservice.api.response.SingleResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseFactory {
    public <T> SingleResponse<T> createSingleResponse(HttpStatus status, String message, T data) {
        return SingleResponse.<T>builder()
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }

    public <T> PageResponse<T> createPageResponse(
            HttpStatus status,
            String message,
            List<T> data,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        return PageResponse.<T>builder()
                .statusCode(status.value())
                .message(message)
                .data(data)
                .paging(new PageResponse.PagingResponse(page, size, totalElements, totalPages))
                .build();
    }

    public ErrorResponse createErrorResponse(HttpStatus status, String message, String error) {
        return ErrorResponse.builder()
                .statusCode(status.value())
                .message(message)
                .error(error)
                .build();
    }

    public <T> ResponseEntity<SingleResponse<T>> successSingle(T data, String message) {
        return ResponseEntity.ok(createSingleResponse(HttpStatus.OK, message, data));
    }

    public <T> ResponseEntity<PageResponse<T>> successPage(Page<T> page, String message) {
        PageResponse<T> response = createPageResponse(
                HttpStatus.OK,
                message,
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String error) {
        return ResponseEntity.status(status).body(createErrorResponse(status, message, error));
    }
}
