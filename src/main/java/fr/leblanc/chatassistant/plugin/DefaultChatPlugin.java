package fr.leblanc.chatassistant.plugin;

import org.springframework.stereotype.Service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

@Service
public class DefaultChatPlugin implements ChatPlugin {

	private interface ChatAgent {
		String chat(String userMessage);
	}
	
	private interface StreamingChatAgent {
		TokenStream streamChat(String userMessage);
	}

	private ChatAgent chatAgent;
	private StreamingChatAgent streamingChatAgent;
	
	@Override
	public String getId() {
		return "default";
	}
	
	@Override
	public String getDisplayName() {
		return "Default";
	}
	
	@Override
	public void initChatAgent(ChatLanguageModel chatModel) {
		 ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
		 chatAgent = AiServices.builder(ChatAgent.class)
		            .chatLanguageModel(chatModel)
		            .chatMemory(chatMemory)
		            .build();
	}

	@Override
	public void initStreamingChatAgent(StreamingChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    streamingChatAgent = AiServices.builder(StreamingChatAgent.class)
	            .streamingChatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .build();
	}
	
	@Override
	public TokenStream streamChat(String message) {
		return streamingChatAgent.streamChat(message);
	}

	@Override
	public String chat(String message) {
		return chatAgent.chat(message);
	}

}
