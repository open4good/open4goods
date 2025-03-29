package org.open4goods.ui.controllers.advice;

import org.open4goods.services.favicon.service.FaviconService;
import org.open4goods.ui.services.DatasourceImageService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice that makes the FaviconService available in all views.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final FaviconService faviconService;
    private final DatasourceImageService datasourceImageService;
   

    public GlobalControllerAdvice(FaviconService faviconService, DatasourceImageService datasourceImageService) {
        this.faviconService = faviconService;
        this.datasourceImageService = datasourceImageService;
    }

    /**
     * Adds the faviconService attribute to the model for all controllers.
     *
     * @return the injected FaviconService
     */
    @ModelAttribute("faviconService")
    public FaviconService faviconService() {
        return faviconService;
    }
    
    
    /**
     * Adds the faviconService attribute to the model for all controllers.
     *
     * @return the injected FaviconService
     */
    @ModelAttribute("datasourceImageService")
    public DatasourceImageService datasourceImageService() {
        return datasourceImageService;
    }
    
}
