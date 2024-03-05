package fr.leblanc.chatassistant.service;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import fr.leblanc.chatassistant.plugin.ChatPlugin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ChatService implements InitializingBean {

	private static final String OLLAMA = "ollama";
	private static final String OPEN_AI = "openai";

	@Value("${chat.model.provider}")
	private String chatModelProvider;
	
	@Value("${chat.model.ollama.url:}")
	private String ollamaModelUrl;
	
	@Value("${chat.model.name}")
	private String chatModelName;
	
	@Value("${chat.model.openai.key:}")
	private String openAIKey;
	
	@Autowired
	private List<ChatPlugin> chatPlugins;
	
	public List<ChatPlugin> getChatPlugins() {
		return chatPlugins;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		ChatLanguageModel chatModel = createChatModel();
		StreamingChatLanguageModel streamingChatModel = createStreamingChatModel();
		for (ChatPlugin chatPlugin : chatPlugins) {
			chatPlugin.initChatAgent(chatModel);
			chatPlugin.initStreamingChatAgent(streamingChatModel);
		}
	}

	private ChatLanguageModel createChatModel() {
		if (OLLAMA.equals(chatModelProvider)) {
			return OllamaChatModel.builder()
					.baseUrl(ollamaModelUrl)
					.modelName(chatModelName)
					.temperature(0.0)
					.build();
		}
		if (OPEN_AI.equals(chatModelProvider)) {
			return OpenAiChatModel.builder()
					.modelName(chatModelName)
					.temperature(0.0)
					.apiKey(openAIKey)
					.build();
		}
		throw new IllegalStateException("Chat model provider unknown: " + chatModelProvider);
	}
	
	private StreamingChatLanguageModel createStreamingChatModel() {
		if (OLLAMA.equals(chatModelProvider)) {
			return OllamaStreamingChatModel.builder()
					.baseUrl(ollamaModelUrl)
					.modelName(chatModelName)
					.temperature(0.0)
					.build();
		}
		if (OPEN_AI.equals(chatModelProvider)) {
			return OpenAiStreamingChatModel.builder()
					.modelName(chatModelName)
					.temperature(0.0)
					.apiKey(openAIKey)
					.build();
		}
		throw new IllegalStateException("Chat model provider unknown: " + chatModelProvider);
	}
	
	public Flux<String> sendStreamingMessage(String chatId, String message) {
		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		TokenStream tokenStream = getChatPlugin(formatChatId(chatId)).streamChat(message);
		tokenStream
			.onNext(t -> {
				if (t.matches("\\d.*")) {
					t = " " + t;
				}
				sink.tryEmitNext(t.replace(" ","$SPACE"));
			})
			.onComplete(c -> sink.tryEmitNext("$COMPLETE"))
			.onError(sink::tryEmitError)
			.start();
		return sink.asFlux();
	}

	public String sendMessage(String chatId, String message) {
		return getChatPlugin(formatChatId(chatId)).chat(message);
	}

	private String formatChatId(String chatId) {
		if (chatId == null || chatId.isBlank() || "null".equals(chatId)) {
			chatId = "default";
		}
		return chatId;
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
