package fr.leblanc.chatassistant.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import fr.leblanc.chatassistant.plugin.ChatPlugin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ChatService implements InitializingBean {

	private static final String OLLAMA = "ollama";

	@Value("${chat.model.provider}")
	private String chatModelProvider;
	
	@Value("${chat.model.url}")
	private String chatModelUrl;
	
	@Value("${chat.model.name}")
	private String chatModelName;
	
	@Autowired
	private ChatPlugin[] chatPlugins;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		StreamingChatLanguageModel chatModel = createChatModel();
		for (ChatPlugin chatPlugin : chatPlugins) {
			chatPlugin.createChatAgent(chatModel);
		}
	}

	private StreamingChatLanguageModel createChatModel() {
		if (OLLAMA.equals(chatModelProvider)) {
			return OllamaStreamingChatModel.builder()
					.baseUrl(chatModelUrl)
					.modelName(chatModelName)
					.temperature(0.0)
					.build();
		}
		throw new IllegalStateException("Chat model provider unknown: " + chatModelProvider);
	}
	
	public Flux<String> sendMessage(String chatId, String message) {
		
		if (chatId == null || chatId.isBlank()) {
			chatId = "default";
		}
		
		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		TokenStream tokenStream = getChatPlugin(chatId).chat(message);
		tokenStream
			.onNext(t -> sink.tryEmitNext(t.replace(" ","$SPACE")))
			.onComplete(c -> sink.tryEmitNext("$CLOSE"))
			.onError(sink::tryEmitError)
			.start();
		
		return sink.asFlux();
		
	}
	
	private ChatPlugin getChatPlugin(String chatId) {
		for (ChatPlugin chatPlugin : chatPlugins) {
			if (chatPlugin.getId().equals(chatId)) {
				return chatPlugin;
			}
		}
		throw new IllegalStateException("Chat ID unknown: " + chatId);
	}

}
