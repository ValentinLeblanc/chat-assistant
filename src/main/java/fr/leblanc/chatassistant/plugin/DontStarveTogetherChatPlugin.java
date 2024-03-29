package fr.leblanc.chatassistant.plugin;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@Service
public class DontStarveTogetherChatPlugin implements ChatPlugin {

	private interface ChatAgent {
		@SystemMessage({
			"You act as an assistant for the Don't Starve Together game.",
			"You need to provide information about the game, the mechanics and the ingredients needed for each crafting operation."
		})
		String chat(String userMessage);
	}
	
	private interface StreamingChatAgent {
		@SystemMessage({
			"You act as an assistant for the Don't Starve Together game.",
			"You need to provide information about the game, the mechanics and the ingredients needed for each crafting operation."
		})
		TokenStream streamChat(String userMessage);
	}

	private ChatAgent chatAgent;
	private StreamingChatAgent streamingChatAgent;
	
	@Override
	public String getId() {
		return "dontstarvetogether";
	}
	
	@Override
	public String getDisplayName() {
		return "Don't Starve Together";
	}

	@Override
	public void initChatAgent(ChatLanguageModel chatModel) {
	
		ContentRetriever contentRetriever = createContentRetriever();
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	
	    chatAgent = AiServices.builder(ChatAgent.class)
	            .chatLanguageModel(chatModel)
	            .contentRetriever(contentRetriever)
	            .chatMemory(chatMemory)
	            .build();
	}

	@Override
	public void initStreamingChatAgent(StreamingChatLanguageModel chatModel) {
	
		ContentRetriever contentRetriever = createContentRetriever();
	    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
	
	    streamingChatAgent = AiServices.builder(StreamingChatAgent.class)
	            .streamingChatLanguageModel(chatModel)
	            .contentRetriever(contentRetriever)
	            .chatMemory(chatMemory)
	            .build();
	}

	private ContentRetriever createContentRetriever() {
		DocumentParser documentParser = new TextDocumentParser();
		
	    Path documentPath = toPath("dontstarvetogether.txt");
	    Document document = FileSystemDocumentLoader.loadDocument(documentPath, documentParser);
	
	    DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
	    List<TextSegment> segments = splitter.split(document);
	
	    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
	    List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
	
	    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
	    embeddingStore.addAll(embeddings, segments);
	
	    return EmbeddingStoreContentRetriever.builder()
	            .embeddingStore(embeddingStore)
	            .embeddingModel(embeddingModel)
	            .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
	            .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
	            .build();
	}
	
	private Path toPath(String fileName) {
        try {
        	URL fileUrl = getClass().getClassLoader().getResource(fileName);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error while retrieving file: " + fileName);
        }
    }

	@Override
	public String chat(String message) {
		return chatAgent.chat(message);
	}
	
	@Override
	public TokenStream streamChat(String message) {
		return streamingChatAgent.streamChat(message);
	}

}
