package pharma.dto;

public class CitationDTO {
    private String documentName;
    private int pageNumber;
    private String chunkText;
    private double relevanceScore;

    public CitationDTO() {
    }

    public CitationDTO(String documentName, int pageNumber, String chunkText, double relevanceScore) {
        this.documentName = documentName;
        this.pageNumber = pageNumber;
        this.chunkText = chunkText;
        this.relevanceScore = relevanceScore;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
