package fr.leblanc.chatassitant.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.leblanc.chatassitant.service.ChatService;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

	private final ChatService chatService;
	
	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}
	
	@GetMapping(value = "/send-message" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> sendMessage(@RequestParam(required = false) String chatId, @RequestParam String message) {
	    return chatService.sendMessage(chatId, message);
	}
}
