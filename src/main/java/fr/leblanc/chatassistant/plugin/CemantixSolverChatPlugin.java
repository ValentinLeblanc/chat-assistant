package fr.leblanc.chatassistant.plugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import fr.leblanc.solver.cemantix.CemantixSolver;

@Service
public class CemantixSolverChatPlugin  implements ChatPlugin {

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
		return "cemantixsolver";
	}

	@Override
	public String getDisplayName() {
		return "Cemantix Solver";
	}

	@Override
	public void initStreamingChatAgent(StreamingChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    streamingChatAgent = AiServices.builder(StreamingChatAgent.class)
	            .streamingChatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .tools(new CemantixSolverTool())
	            .build();
	}

	@Override
	public void initChatAgent(ChatLanguageModel chatModel) {
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	    chatAgent = AiServices.builder(ChatAgent.class)
	            .chatLanguageModel(chatModel)
	            .chatMemory(chatMemory)
	            .tools(new CemantixSolverTool())
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
	
	private static class CemantixSolverTool {
		
		@Tool("Gets the cemantix word")
		String getCemantixWord(String word) {
			LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Paris"));
	        String date = currentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
			CemantixSolver solver = new CemantixSolver(date);
			return solver.solve();
		}
		
}

}
