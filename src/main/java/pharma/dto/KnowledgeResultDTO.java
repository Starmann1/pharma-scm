package pharma.dto;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeResultDTO {
    private List<CitationDTO> citations = new ArrayList<>();

    public KnowledgeResultDTO() {
    }

    public KnowledgeResultDTO(List<CitationDTO> citations) {
        this.citations = citations;
    }

    public List<CitationDTO> getCitations() {
        return citations;
    }

    public void setCitations(List<CitationDTO> citations) {
        this.citations = citations;
    }
}
