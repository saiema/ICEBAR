package ar.edu.unrc.exa.dc.openai.managers;

import ar.edu.unrc.exa.dc.openai.connector.OpenAIChatConnectorException;
import ar.edu.unrc.exa.dc.openai.models.Message;

public class OpenAIResponse {

    private Message message;
    private boolean error;
    private OpenAIChatConnectorException exception;

    public static OpenAIResponse validResponse(Message message) {
        return new OpenAIResponse(message, null);
    }

    public static OpenAIResponse invalidResponse(OpenAIChatConnectorException exception) {
        return new OpenAIResponse(null, exception);
    }

    private OpenAIResponse(Message message, OpenAIChatConnectorException exception) {
        this.error = exception != null;
        this.message = message;
        this.exception = exception;
    }

    public Message message() {
        return message;
    }

    public boolean isValid() {
        return !error;
    }

    public OpenAIChatConnectorException exception() {
        return exception;
    }

}
