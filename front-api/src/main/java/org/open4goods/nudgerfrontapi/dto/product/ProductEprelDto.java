package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.eprel.EprelProduct;
import org.springframework.beans.BeanUtils;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sanitised copy of an {@link EprelProduct} tailored for transport through the
 * frontend API.
 * <p>
 * The EPREL catalogue stores a very large and heterogeneous payload in the
 * {@code categorySpecificAttributes} map which is not required by the
 * frontend. Returning a clone avoids leaking this heavy structure while
 * keeping the rest of the metadata available to consumers.
 * </p>
 */
@Schema(name = "ProductEprelDto", description = "EPREL product metadata without category specific attributes")
public class ProductEprelDto extends EprelProduct {

    /**
     * Create an empty DTO instance.
     */
    public ProductEprelDto() {
        super();
    }

    /**
     * Build a DTO from the provided {@link EprelProduct} while removing the
     * {@code categorySpecificAttributes} section.
     *
     * @param source original EPREL entity coming from the repository
     * @return cloned DTO instance or {@code null} when the source is {@code null}
     */
    public static ProductEprelDto from(EprelProduct source) {
        if (source == null) {
            return null;
        }
        ProductEprelDto target = new ProductEprelDto();
        BeanUtils.copyProperties(source, target);
        target.setCategorySpecificAttributes(null);
        return target;
    }
}
