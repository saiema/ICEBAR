package ar.edu.unrc.exa.dc.openai.managers;

import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIChatConnector;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIParams;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIRequest;
import ar.edu.unrc.exa.dc.openai.models.Chat;
import ar.edu.unrc.exa.dc.openai.models.Message;
import ar.edu.unrc.exa.dc.openai.models.Role;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Optional;

public class ChatManager {

    private static ChatManager instance;

    public static ChatManager getInstance() {
        if (instance == null)
            instance = new ChatManager();
        return instance;
    }

    private final OpenAIParams openAIParams;
    private final Chat chat;

    private ChatManager() {
        Dotenv dotenv = Dotenv.configure().filename(ICEBARProperties.getInstance().icebarOpenAIEnvFile().toString()).load();
        String model = dotenv.get("ICEBAR_OPENAI_MODEL", "gpt-3.5-turbo");
        float temperature = Float.parseFloat(dotenv.get("ICEBAR_OPENAI_TEMPERATURE", "1.0f"));
        int n = Integer.parseInt(dotenv.get("ICEBAR_OPENAI_N", "1"));
        int maxTokens = Integer.parseInt(dotenv.get("ICEBAR_OPENAI_MAX_TOKENS", "1000"));
        openAIParams = new OpenAIParams(model, temperature, n, maxTokens);
        chat = new Chat();
    }

    public void clearChat() {
        chat.clear();
    }

    public boolean isChatEmpty() {
        return chat.isEmpty();
    }

    public void addRestriction(Message restriction) {
        chat.addMessage(restriction);
    }

    public Optional<OpenAIResponse> queryOpenAI(Message query) {
        chat.addMessage(query);
        OpenAIRequest openAIRequest = new OpenAIRequest(openAIParams, chat);
        return OpenAIChatConnector.queryOpenAI(openAIRequest);
    }

}
