package com.se361.iam_service.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleResponse<T> {
    private int statusCode;
    private String message;
    private T data;
}
