package org.open4goods.nudgerfrontapi.dto;

import java.util.List;

public record SearchRequest(String query,
                            Integer fromPrice,
                            Integer toPrice,
                            List<String> categories,
                            String condition,
                            Integer page,
                            Integer size,
                            boolean sort) {
}
