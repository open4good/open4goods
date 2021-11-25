package org.open4goods.ui.controllers.ui;

import javax.servlet.http.HttpServletRequest;

import org.open4goods.dao.AggregatedDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController extends AbstractUiController {

	private @Autowired AggregatedDataRepository aggregatedDataRepository;
	
	@GetMapping("/")
	public ModelAndView index(final HttpServletRequest request) {

		ModelAndView model = defaultModelAndView("index", request);
		model.addObject("totalItems", aggregatedDataRepository.countMainIndexHavingPrice());
		
		return model;
	}
	
	
	
}
