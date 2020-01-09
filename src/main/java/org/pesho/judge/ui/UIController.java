package org.pesho.judge.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UIController {
	
	@RequestMapping(value = "/")
	public String index() {
		return "index";
	}
	
}
