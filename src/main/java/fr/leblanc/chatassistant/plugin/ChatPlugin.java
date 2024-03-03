package fr.leblanc.chatassistant.plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.TokenStream;

public interface ChatPlugin {

	String getId();
	
	String getDisplayName();
	
	void initStreamingChatAgent(StreamingChatLanguageModel chatModel);
	
	void initChatAgent(ChatLanguageModel chatModel);

	TokenStream streamChat(String message);
	
	String chat(String message);
	
}
