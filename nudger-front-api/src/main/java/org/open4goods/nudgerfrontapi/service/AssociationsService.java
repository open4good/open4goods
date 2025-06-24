package org.open4goods.nudgerfrontapi.service;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.AssociationDto;
import org.springframework.stereotype.Service;

/**
 * Returns list of supported associations.
 */
@Service
public class AssociationsService {

    private final List<AssociationDto> associations = List.of(
            new AssociationDto("A1", "Association One"),
            new AssociationDto("A2", "Association Two"));

    public List<AssociationDto> getAssociations() {
        return associations;
    }
}
