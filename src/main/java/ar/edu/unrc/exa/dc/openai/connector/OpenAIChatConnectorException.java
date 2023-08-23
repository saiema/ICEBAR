package ar.edu.unrc.exa.dc.openai.connector;

public class OpenAIChatConnectorException extends RuntimeException {

    public OpenAIChatConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenAIChatConnectorException(Throwable cause) {
        super(cause);
    }
}
