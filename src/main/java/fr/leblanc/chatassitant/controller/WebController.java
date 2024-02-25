package fr.leblanc.chatassitant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

	@GetMapping("/")
	public String home() {
		return "home";
	}
	
	@GetMapping("/chat")
	public String chat(@RequestParam(required = false) String chatId, Model model) {
		model.addAttribute("chatId", chatId);
		return "chat";
	}
	
}
