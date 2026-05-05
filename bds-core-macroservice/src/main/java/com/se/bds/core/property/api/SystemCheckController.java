package com.se.bds.core.property.api;


import com.se.bds.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemCheckController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("Core macroservice is up and using bds-common");
    }
}
