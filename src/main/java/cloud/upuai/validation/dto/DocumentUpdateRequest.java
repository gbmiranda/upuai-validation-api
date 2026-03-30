package cloud.upuai.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentUpdateRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
