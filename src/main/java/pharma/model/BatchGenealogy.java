package pharma.model;

import java.time.LocalDateTime;

public class BatchGenealogy {
    private int genealogyId;
    private String parentBatch;
    private String childBatch;
    private int productionOrderId;
    private String relationshipType;
    private LocalDateTime createdAt;

    public BatchGenealogy() {
    }

    public BatchGenealogy(int genealogyId, String parentBatch, String childBatch, int productionOrderId,
            String relationshipType, LocalDateTime createdAt) {
        this.genealogyId = genealogyId;
        this.parentBatch = parentBatch;
        this.childBatch = childBatch;
        this.productionOrderId = productionOrderId;
        this.relationshipType = relationshipType;
        this.createdAt = createdAt;
    }

    public int getGenealogyId() {
        return genealogyId;
    }

    public void setGenealogyId(int genealogyId) {
        this.genealogyId = genealogyId;
    }

    public String getParentBatch() {
        return parentBatch;
    }

    public void setParentBatch(String parentBatch) {
        this.parentBatch = parentBatch;
    }

    public String getChildBatch() {
        return childBatch;
    }

    public void setChildBatch(String childBatch) {
        this.childBatch = childBatch;
    }

    public int getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(int productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
