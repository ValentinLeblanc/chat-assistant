package fr.leblanc.chatassistant.plugin;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.TokenStream;

public interface ChatPlugin {

	String getId();
	
	void createChatAgent(StreamingChatLanguageModel chatModel);

	TokenStream chat(String message);
	
}
