package fr.leblanc.chatassistant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.leblanc.chatassistant.service.ChatService;

@Controller
public class WebController {

	@Autowired
	private ChatService chatService;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("plugins", chatService.getChatPlugins());
		return "home";
	}
	
	@GetMapping("/chat")
	public String chat(@RequestParam(required = false) String chatId, Model model) {
		model.addAttribute("chatId", chatId);
		return "chat";
	}
	
}
