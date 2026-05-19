package com.se.bds.core.property.internal.application.command.pattern;

public interface ReversibleCommand<T> {

    T execute();

    // Yêu cầu bắt buộc của Architecture Docs (Restoration)
    void undo();
}
