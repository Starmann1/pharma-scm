package pharma.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import pharma.agent.ontology.AgentActions;
import pharma.config.JsonConfig;

class AgentEnvelopeSerializationTest {
    @Test
    void requestEnvelopeRoundTripsThroughJson() throws Exception {
        ManufacturingFeasibilityDTO payload = new ManufacturingFeasibilityDTO();
        payload.setMaterialCode("DRG001");
        payload.setBomId(1);
        payload.setPlannedQuantity(1000);

        AgentRequestEnvelope<ManufacturingFeasibilityDTO> request = new AgentRequestEnvelope<>(
                AgentActions.MANUFACTURING_FEASIBILITY,
                1,
                5000,
                payload);

        String json = JsonConfig.objectMapper().writeValueAsString(request);
        AgentRequestEnvelope<ManufacturingFeasibilityDTO> restored = JsonConfig.objectMapper().readValue(
                json,
                new TypeReference<AgentRequestEnvelope<ManufacturingFeasibilityDTO>>() {
                });

        assertNotNull(restored.getTransactionId());
        assertEquals(AgentActions.MANUFACTURING_FEASIBILITY, restored.getAction());
        assertEquals("DRG001", restored.getPayload().getMaterialCode());
        assertEquals(5000, restored.getDeadlineMillis());
    }
}
