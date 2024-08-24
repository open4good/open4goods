package org.open4goods.ui.controllers.ui;

import org.open4goods.ui.services.todo.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller renders the /todos page
 * 
 */

@Controller
public class TodoController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TodoController.class);


	private @Autowired TodoService todoService;
	private @Autowired UiService uiService;
	
	public TodoController() {
		super();
	}

	@GetMapping("/todos")
	protected ModelAndView todo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mv = uiService.defaultModelAndView("todo", request);
		mv.addObject("todos", todoService.getTodos());
		mv.addObject("byPriority", todoService.byPriority());
		mv.addObject("byComponent", todoService.byComponents());
		mv.addObject("byCategory", todoService.byCategory());
		return mv;
	}

}
