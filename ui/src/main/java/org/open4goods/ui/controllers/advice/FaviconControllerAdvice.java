package org.open4goods.ui.controllers.advice;

import org.open4goods.services.favicon.service.FaviconService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice that makes the FaviconService available in all views.
 */
@ControllerAdvice
public class FaviconControllerAdvice {

    private final FaviconService faviconService;

   
    
    public FaviconControllerAdvice(FaviconService faviconService) {
        this.faviconService = faviconService;
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
    
}
