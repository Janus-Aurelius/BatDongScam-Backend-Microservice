package com.se361.financial_service.controllers;

import com.se361.financial_service.config.ResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractBaseController {
    @Autowired
    protected ResponseFactory responseFactory;
}
