package pharma.dto;

public class KnowledgeQueryDTO {
    private String query;
    private int topK = 3;
    private String filterByDocType;

    public KnowledgeQueryDTO() {
    }

    public KnowledgeQueryDTO(String query, int topK, String filterByDocType) {
        this.query = query;
        this.topK = topK;
        this.filterByDocType = filterByDocType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getFilterByDocType() {
        return filterByDocType;
    }

    public void setFilterByDocType(String filterByDocType) {
        this.filterByDocType = filterByDocType;
    }
}
