package ar.edu.unrc.exa.dc.openai.connector;

import ar.edu.unrc.exa.dc.openai.models.Chat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class OpenAIRequest {

    final private OpenAIParams openAIParams;
    final private Chat chat;
    private Exception error;

    public OpenAIRequest(OpenAIParams openAIParams, Chat chat) {
        this.openAIParams = openAIParams;
        this.chat = chat;
    }

    public Exception error() {
        Exception error = this.error;
        this.error = null;
        return error;
    }

    public Chat chat() {
        return chat;
    }

    public Optional<String> asJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode chatNode = mapper.valueToTree(chat);
        ObjectNode paramsNode = mapper.valueToTree(openAIParams);
        ObjectNode resultNode = chatNode.deepCopy();
        resultNode.setAll(paramsNode);
        try {
            String json = mapper.writeValueAsString(resultNode);
            return Optional.of(json);
        } catch (JsonProcessingException e) {
            this.error = e;
            return Optional.empty();
        }
    }

}
