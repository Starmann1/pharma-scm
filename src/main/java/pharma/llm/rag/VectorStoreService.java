package pharma.llm.rag;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import pharma.dto.CitationDTO;

/**
 * In-memory vector store service for the pharmaceutical RAG pipeline.
 *
 * <p>Handles the full embedding lifecycle:
 * <ol>
 *   <li>Splitting documents into overlapping text segments.</li>
 *   <li>Encoding segments via a {@link EmbeddingModel}.</li>
 *   <li>Storing vectors in an {@link InMemoryEmbeddingStore}.</li>
 *   <li>Retrieving semantically relevant segments for a natural-language query.</li>
 * </ol>
 *
 * <p>Segment parameters (300 chars, 30-char overlap) are tuned for typical
 * pharmaceutical SOP documents whose paragraphs are short and self-contained.
 */
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    /** Maximum characters per text segment. */
    private static final int SEGMENT_SIZE = 300;

    /** Overlap between consecutive segments to preserve context at boundaries. */
    private static final int SEGMENT_OVERLAP = 30;

    private final InMemoryEmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;
    private int totalSegments;

    /**
     * Creates a new vector store service with the given embedding model.
     *
     * @param embeddingModel the model used to convert text into embeddings
     */
    public VectorStoreService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.store = new InMemoryEmbeddingStore<>();
        this.totalSegments = 0;
    }

    /**
     * Splits, embeds, and stores the given documents.
     *
     * <p>Each document is split into segments of {@value #SEGMENT_SIZE} characters
     * with {@value #SEGMENT_OVERLAP} characters of overlap.  Segments are embedded
     * individually and added to the in-memory vector store.
     *
     * @param documents the documents to ingest (must not be null)
     */
    public void ingest(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("No documents provided for ingestion — skipping.");
            return;
        }

        log.info("Ingesting {} document(s) into vector store...", documents.size());

        var splitter = DocumentSplitters.recursive(SEGMENT_SIZE, SEGMENT_OVERLAP);

        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            store.addAll(embeddings, segments);
            totalSegments += segments.size();

            String docName = document.metadata().getString("file_name");
            log.info("  ✓ Ingested '{}' — {} segment(s)", docName, segments.size());
        }

        log.info("Ingestion complete. Total segments in store: {}", totalSegments);
    }

    /**
     * Finds the most relevant text segments for the given natural-language query.
     *
     * @param query the user's search query
     * @param topK  maximum number of citations to return
     * @return a list of {@link CitationDTO}s ordered by descending relevance
     */
    public List<CitationDTO> findRelevant(String query, int topK) {
        if (!isReady()) {
            log.warn("Vector store is empty — returning no results for query: {}", query);
            return List.of();
        }

        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(topK)
                .build();

        List<Content> results = retriever.retrieve(dev.langchain4j.rag.query.Query.from(query));

        List<CitationDTO> citations = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Content content = results.get(i);
            TextSegment segment = content.textSegment();

            String docName = segment.metadata().getString("file_name");
            if (docName == null) {
                docName = "unknown";
            }

            int pageNumber = 0;
            try {
                Integer page = segment.metadata().getInteger("page_number");
                if (page != null) {
                    pageNumber = page;
                }
            } catch (Exception ignored) {
                // metadata key may not exist
            }

            // Relevance score decreases linearly; top result gets 1.0
            double relevance = 1.0 - (i * (1.0 / Math.max(results.size(), 1)));

            citations.add(new CitationDTO(docName, pageNumber, segment.text(), relevance));
        }

        log.info("Found {} citation(s) for query: '{}'", citations.size(), query);
        return citations;
    }

    /**
     * Returns {@code true} if the store has been populated with at least one segment.
     *
     * @return whether the vector store is ready for queries
     */
    public boolean isReady() {
        return totalSegments > 0;
    }
}
