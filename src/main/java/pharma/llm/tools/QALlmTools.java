package pharma.llm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.QAResultDTO;
import pharma.service.QAService;

/**
 * LangChain4j tool wrapper that exposes {@link QAService} methods
 * as LLM-callable tools.
 *
 * <p>Provides QA batch review functionality to the LLM agent,
 * enabling it to check the quality status of production batches.
 */
public class QALlmTools {

    private static final Logger log = LoggerFactory.getLogger(QALlmTools.class);

    private final QAService qaService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param qaService the QA service instance
     */
    public QALlmTools(QAService qaService) {
        this.qaService = qaService;
    }

    /**
     * Reviews a production batch for quality compliance.
     *
     * @param batchNumber the batch number to review
     * @return QA review result with decision, findings, and status transitions
     */
    @Tool("Review a production batch for quality compliance. Returns the QA decision " +
          "(PASS/FAIL/HOLD), any findings or deviations, and the batch status transition.")
    public QAResultDTO reviewBatch(
            @P("The batch number to review, e.g. 'BATCH-2025-001'") String batchNumber) {
        log.info("[QALlmTools] reviewBatch batchNumber='{}'", batchNumber);
        try {
            return qaService.reviewBatch(batchNumber);
        } catch (Exception e) {
            log.error("[QALlmTools] reviewBatch failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to review batch " + batchNumber + ": " + e.getMessage(), e);
        }
    }
}
