package pharma.agent.operational;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.embedding.EmbeddingModel;
import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.CitationDTO;
import pharma.dto.KnowledgeQueryDTO;
import pharma.dto.KnowledgeResultDTO;
import pharma.llm.rag.DocumentIngestionPipeline;
import pharma.llm.rag.EmbeddingConfig;
import pharma.llm.rag.VectorStoreService;

/**
 * KnowledgeAgent — Phase 11 RAG-powered operational agent.
 *
 * <p>Provides semantic search over pharmaceutical SOP (Standard Operating
 * Procedure) documents using a Retrieval-Augmented Generation pipeline:
 * <ol>
 *   <li>On startup, loads and embeds SOP documents into an in-memory vector store.</li>
 *   <li>On {@link AgentActions#KNOWLEDGE_QUERY} requests, performs similarity search
 *       and returns the top-K most relevant document excerpts as citations.</li>
 * </ol>
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code GEMINI_API_KEY} — required; used to access the Google AI embedding model.</li>
 *   <li>{@code SOP_DOCUMENTS_PATH} — optional; defaults to {@code ./sop_documents/}.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates to {@link VectorStoreService} for all embedding
 * operations.  No JDBC or repository calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class KnowledgeAgent extends BasePharmaAgent {

    private VectorStoreService vectorStoreService;
    private boolean ragReady;

    @Override
    protected void setup() {
        super.setup();

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("[KnowledgeAgent] GEMINI_API_KEY environment variable is not set. "
                    + "RAG pipeline will be unavailable.");
            ragReady = false;
        } else {
            initRagPipeline(apiKey);
        }

        addBehaviour(new KnowledgeRequestBehaviour());
        logger.info("[KnowledgeAgent] Ready to handle knowledge queries. RAG ready={}",
                ragReady);
    }

    /**
     * Initialises the embedding model, vector store, and ingestion pipeline,
     * then attempts to ingest documents from the configured SOP directory.
     */
    private void initRagPipeline(String apiKey) {
        try {
            EmbeddingModel embeddingModel = EmbeddingConfig.createEmbeddingModel(apiKey);
            vectorStoreService = new VectorStoreService(embeddingModel);
            DocumentIngestionPipeline pipeline = new DocumentIngestionPipeline(vectorStoreService);

            String sopPath = System.getenv("SOP_DOCUMENTS_PATH");
            if (sopPath == null || sopPath.isBlank()) {
                sopPath = DocumentIngestionPipeline.DEFAULT_SOP_PATH;
            }

            logger.info("[KnowledgeAgent] Ingesting SOP documents from: {}", sopPath);
            pipeline.ingestDirectory(sopPath);

            ragReady = vectorStoreService.isReady();
            if (!ragReady) {
                logger.warn("[KnowledgeAgent] No documents were ingested — "
                        + "queries will return empty results until documents are available.");
            }

        } catch (Exception e) {
            logger.error("[KnowledgeAgent] Failed to initialise RAG pipeline: {}", e.getMessage(), e);
            ragReady = false;
        }
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class KnowledgeRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            return switch (request.getAction()) {
                case AgentActions.KNOWLEDGE_QUERY -> handleKnowledgeQuery(request);
                default -> AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "KnowledgeAgent: unsupported action '" + request.getAction() + "'");
            };
        }

        private AgentResponseEnvelope<KnowledgeResultDTO> handleKnowledgeQuery(
                AgentRequestEnvelope<?> request) {

            KnowledgeQueryDTO query = extractPayload(request, KnowledgeQueryDTO.class);

            logger.info("[KnowledgeAgent] KNOWLEDGE_QUERY query='{}' topK={}",
                    query.getQuery(), query.getTopK());

            if (!ragReady || vectorStoreService == null) {
                logger.warn("[KnowledgeAgent] RAG pipeline not ready — returning empty result.");
                KnowledgeResultDTO emptyResult = new KnowledgeResultDTO(List.of());
                AgentResponseEnvelope<KnowledgeResultDTO> response = AgentResponseEnvelope.success(
                        request.getTransactionId(), request.getAction(), emptyResult);
                response.getAgentTrace().add("KnowledgeAgent: RAG not ready — "
                        + "no SOP documents have been ingested.");
                return response;
            }

            int topK = query.getTopK() > 0 ? query.getTopK() : 3;
            List<CitationDTO> citations = vectorStoreService.findRelevant(
                    query.getQuery(), topK);

            KnowledgeResultDTO result = new KnowledgeResultDTO(citations);

            logger.info("[KnowledgeAgent] Returning {} citation(s) for query: '{}'",
                    citations.size(), query.getQuery());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), result);
        }

        /**
         * Converts the raw Object payload (Jackson deserialises maps) into the target DTO type.
         */
        private <T> T extractPayload(AgentRequestEnvelope<?> request, Class<T> type) {
            ObjectMapper mapper = MAPPER;
            return mapper.convertValue(request.getPayload(), type);
        }
    }
}
