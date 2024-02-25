package fr.leblanc.chatassistant.plugin;

import org.springframework.stereotype.Service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

@Service
public class DefaultChatPlugin implements ChatPlugin {

	private interface ChatAgent {
		TokenStream chat(String userMessage);
	}

	private ChatAgent chatAgent;
	
	@Override
	public String getId() {
		return "default";
	}

	@Override
	public void createChatAgent(StreamingChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    chatAgent = AiServices.builder(ChatAgent.class)
	            .streamingChatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .build();
	}
	
	@Override
	public TokenStream chat(String message) {
		return chatAgent.chat(message);
	}

}
