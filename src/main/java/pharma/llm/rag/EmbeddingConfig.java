package pharma.llm.rag;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;

/**
 * Configuration factory for embedding models used in the RAG pipeline.
 *
 * <p>Creates a Google AI embedding model ({@code text-embedding-004}) suitable for
 * pharmaceutical SOP document retrieval.  The model converts text segments into
 * high-dimensional vectors for semantic similarity search.
 */
public final class EmbeddingConfig {

    /** Google AI embedding model name used for vector encoding. */
    private static final String MODEL_NAME = "text-embedding-004";

    private EmbeddingConfig() {
        // utility class — no instantiation
    }

    /**
     * Creates a new {@link EmbeddingModel} backed by Google AI.
     *
     * @param apiKey the Gemini / Google AI API key
     * @return a fully-configured embedding model ready for encoding text
     * @throws IllegalArgumentException if {@code apiKey} is null or blank
     */
    public static EmbeddingModel createEmbeddingModel(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank.");
        }
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(MODEL_NAME)
                .build();
    }
}
