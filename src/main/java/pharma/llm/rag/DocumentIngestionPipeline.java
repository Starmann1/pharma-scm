package pharma.llm.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;

/**
 * Pipeline that discovers, loads, and ingests pharmaceutical SOP documents
 * into the RAG vector store.
 *
 * <p>On startup the {@link pharma.agent.operational.KnowledgeAgent} creates an
 * instance of this pipeline and calls {@link #ingestDirectory(String)} with the
 * path to the SOP documents folder.  All loadable files in that directory are
 * read by the LangChain4j {@link FileSystemDocumentLoader}, then handed to
 * {@link VectorStoreService#ingest(List)} for splitting and embedding.
 *
 * <p>If the directory does not exist or is empty the pipeline logs a warning
 * and returns gracefully — the agent will still start but will report "no
 * documents indexed" to callers.
 */
public class DocumentIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    /** Default path used when the {@code SOP_DOCUMENTS_PATH} env var is unset. */
    public static final String DEFAULT_SOP_PATH = "./sop_documents/";

    private final VectorStoreService vectorStoreService;

    /**
     * Creates a new ingestion pipeline backed by the given vector store.
     *
     * @param vectorStoreService the service that will embed and store the documents
     */
    public DocumentIngestionPipeline(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Loads every document in {@code directoryPath} and ingests them into the
     * vector store.
     *
     * <p>If the directory does not exist, is not readable, or contains no
     * loadable files, the method logs a warning and returns without throwing.
     *
     * @param directoryPath absolute or relative path to the SOP documents folder
     */
    public void ingestDirectory(String directoryPath) {
        Path dir = Path.of(directoryPath);

        if (!Files.exists(dir)) {
            log.warn("SOP documents directory does not exist: {} — skipping ingestion.", dir.toAbsolutePath());
            return;
        }
        if (!Files.isDirectory(dir)) {
            log.warn("Path is not a directory: {} — skipping ingestion.", dir.toAbsolutePath());
            return;
        }

        try {
            boolean isEmpty;
            try (var stream = Files.list(dir)) {
                isEmpty = stream.findAny().isEmpty();
            }
            if (isEmpty) {
                log.warn("SOP documents directory is empty: {} — no documents to ingest.", dir.toAbsolutePath());
                return;
            }

            log.info("Loading documents from: {}", dir.toAbsolutePath());
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(dir);

            if (documents.isEmpty()) {
                log.warn("FileSystemDocumentLoader returned 0 loadable documents from: {}", dir.toAbsolutePath());
                return;
            }

            log.info("Loaded {} document(s). Beginning ingestion pipeline...", documents.size());
            vectorStoreService.ingest(documents);
            log.info("Document ingestion pipeline completed successfully.");

        } catch (IOException e) {
            log.error("I/O error while scanning SOP documents directory '{}': {}",
                    dir.toAbsolutePath(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during document ingestion from '{}': {}",
                    dir.toAbsolutePath(), e.getMessage(), e);
        }
    }
}
