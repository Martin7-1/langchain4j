package dev.langchain4j.model.huggingface.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextGenerationResponse {

    private String generatedText;
}
