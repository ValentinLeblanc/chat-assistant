package fr.leblanc.chatassistant.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

@Service
public class WebChatPlugin implements ChatPlugin {

	private static final Logger logger = LoggerFactory.getLogger(WebChatPlugin.class);

	private static final int MAX_GOOGLE_RESULTS = 10;

	@SystemMessage({
		"You search on the Web to retrieve relevant information",
	})
	private interface ChatAgent {
		String chat(String userMessage);
	}
	
	@SystemMessage({
		"You search on the Web to retrieve relevant information",
		})
	private interface StreamingChatAgent {
		TokenStream streamChat(String userMessage);
	}

	private ChatAgent chatAgent;
	private StreamingChatAgent streamingChatAgent;
	
	@Override
	public String getId() {
		return "web";
	}
	
	@Override
	public String getDisplayName() {
		return "Web";
	}

	@Override
	public void initStreamingChatAgent(StreamingChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    streamingChatAgent = AiServices.builder(StreamingChatAgent.class)
	            .streamingChatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .tools(new WebScrappingTool())
	            .build();
	}

	@Override
	public void initChatAgent(ChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    chatAgent = AiServices.builder(ChatAgent.class)
	            .chatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .tools(new WebScrappingTool())
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
	
	private static class WebScrappingTool {
		
		@Tool("Gets information from the Web, given a query")
		String searchOnWeb(String query) {
			List<String> links = extractGoogleLinks(query);
			return extractWebPages(links);
		}

		private List<String> extractGoogleLinks(String query) {
			logger.info("search from Google: {}", query);
			List<String> links = new ArrayList<>();
		    try {
		        Document doc = Jsoup.connect("https://www.google.com/search?q=" + query).get();
		        Elements results = doc.select("div.g");
		        Elements urlElements = results.select("div.yuRUbf a[href]");
		        int count = 0;
		        for (Element element : urlElements) {
		            String link = element.attr("href");
		            links.add(link);
		            count++;
		            if (count >= MAX_GOOGLE_RESULTS) {
		            	break;
		            }
		        }
		    } catch (IOException e) {
		    	logger.error("error while searching: {}", e.getMessage());
		    }
			return links;
		}

		private String extractWebPages(List<String> links) {
			List<String> texts = new ArrayList<>();
	        for (String link : links) {
	        	logger.info("scrapping data from: {}", link);
	        	try {
	        		Document doc = Jsoup.connect(link).get();
	        		String text = doc.text();
	        		if (text == null || text.isBlank()) {
	        			text = "#EMPTY";
	        		}
	        		texts.add(text.substring(0, Math.min(5000, text.length())));
	        	} catch (IOException e) {
	        		logger.error("error while scrapping: {}", e.getMessage());
	        	}
	        }
			String join = String.join(",", texts);
			return join.substring(0, Math.min(16000, join.length()));
		}
	}
}
