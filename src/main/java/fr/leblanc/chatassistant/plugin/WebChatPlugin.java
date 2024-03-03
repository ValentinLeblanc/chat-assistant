package fr.leblanc.chatassistant.plugin;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import fr.leblanc.solver.cemantix.CemantixSolver;

@Service
public class WebChatPlugin implements ChatPlugin {

	@SystemMessage({
		"You can search on google to get web pages URL with relevant information",
		"You can extract a web page content from its URL",
		"You can search the cemantix word",
	})
	private interface ChatAgent {
		String chat(String userMessage);
	}
	
	@SystemMessage({
		"You can search on google to get web pages URL with relevant information",
		"You can extract a web page content from its URL",
		"You can search the cemantix word",
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
	            .tools(new WebTool())
	            .build();
	}

	@Override
	public void initChatAgent(ChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    chatAgent = AiServices.builder(ChatAgent.class)
	            .chatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .tools(new WebTool())
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
	
	private static class WebTool {
		
		@Tool("Gets the cemantix word")
		String getCemantixWord(String word) {
			LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Paris"));
	        String date = currentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
			CemantixSolver solver = new CemantixSolver(date);
			return solver.solve();
		}
		
		@Tool("Extracts text from a web page, given its URL")
		String extractTextFromWebPage(String url) {
			try {
				String html = fetchHTML(url);
				Document doc = Jsoup.parse(html);
				String text = doc.text();
				if (text == null || text.isBlank()) {
					text = "#EMPTY";
				}
				return text;
			} catch (HttpClientErrorException e) {
				return "#ERROR";
			}
		}
		
		@Tool("Search URL results from google, given a query")
		List<String> searchFromGoogle(String query) {
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
                    if (count >= 2) {
                    	break;
                    }
                }
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return links;
		}
		
		private String fetchHTML(String url) {
	        RestTemplate restTemplate = new RestTemplate();
	        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
	        return response.getBody();
	    }

	}

}
