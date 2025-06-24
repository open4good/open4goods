package org.open4goods.nudgerfrontapi.controller;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.AssociationDto;
import org.open4goods.nudgerfrontapi.service.AssociationsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AssociationsController {

    private final AssociationsService associationsService;

    public AssociationsController(AssociationsService associationsService) {
        this.associationsService = associationsService;
    }

    @GetMapping("/associations")
    @Operation(summary = "List associations")
    public List<AssociationDto> associations() {
        return associationsService.getAssociations();
    }
}
