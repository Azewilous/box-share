package org.azelabs.boxshare.dtos;

import jakarta.validation.constraints.NotNull;
import org.azelabs.boxshare.application.enums.FileVisibility;

public record VisibilityRequest(Long id, @NotNull FileVisibility visibility) {}
