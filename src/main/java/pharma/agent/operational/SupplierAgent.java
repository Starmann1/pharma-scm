package pharma.agent.operational;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.SupplierCheckRequestDTO;
import pharma.dto.SupplierScoreDTO;

/**
 * SupplierAgent — Phase 6 operational agent.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link AgentActions#CHECK_SUPPLIER} — returns a ranked list of approved suppliers
 *       for a given material code, ordered by composite score descending.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code this.services.getSupplierService()}.  No JDBC calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class SupplierAgent extends BasePharmaAgent {

    private Integer supplierId = null;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length > 1 && args[1] instanceof Integer) {
            supplierId = (Integer) args[1];
            jade.lang.acl.MessageTemplate template = jade.lang.acl.MessageTemplate.MatchPerformative(jade.lang.acl.ACLMessage.CFP);
            addBehaviour(new pharma.agent.behaviour.SupplierProposalBehaviour(this, template, services, supplierId));
            logger.info("[SupplierAgent-{}] Ready to propose for Contract-Net.", supplierId);
        } else {
            addBehaviour(new SupplierRequestBehaviour());
            logger.info("[SupplierAgent] Ready to rank suppliers.");
        }
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class SupplierRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            if (!AgentActions.CHECK_SUPPLIER.equals(request.getAction())) {
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "SupplierAgent: unsupported action '" + request.getAction() + "'");
            }

            @SuppressWarnings("null")
            SupplierCheckRequestDTO req = new ObjectMapper()
                    .convertValue(request.getPayload(), SupplierCheckRequestDTO.class);

            logger.info("[SupplierAgent] CHECK_SUPPLIER materialCode='{}'", req.getMaterialCode());

            List<SupplierScoreDTO> ranked = services.getSupplierService()
                    .rankApprovedSuppliersForMaterial(req.getMaterialCode());

            logger.info("[SupplierAgent] Found {} approved supplier(s) for '{}'.",
                    ranked.size(), req.getMaterialCode());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), ranked);
        }
    }
}
