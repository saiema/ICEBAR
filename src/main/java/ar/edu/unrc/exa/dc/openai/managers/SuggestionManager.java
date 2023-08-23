package ar.edu.unrc.exa.dc.openai.managers;

import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIChatConnector;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIChatConnectorException;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIRequest;
import ar.edu.unrc.exa.dc.openai.models.Chat;
import ar.edu.unrc.exa.dc.openai.models.Message;
import ar.edu.unrc.exa.dc.openai.models.Role;
import ar.edu.unrc.exa.dc.util.Utils;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestionManager {

    private final Path suggestionsFolder;
    private final int maximumContext;
    private final String promptsInitialBuggyModel;
    private final String promptsStillBuggyModel;
    private final String oraclePrompt;
    private final boolean defineSpecificContext;
    private final String context;
    private int currentContext = 0;
    private final TransactionLimiter transactionLimiter;

    public SuggestionManager(Path suggestionsFolder, int maximumContext) {
        this.suggestionsFolder = suggestionsFolder;
        this.maximumContext = maximumContext;
        Dotenv dotenv = Dotenv.configure().filename(ICEBARProperties.getInstance().icebarOpenAIEnvFile().toString()).load();
        this.transactionLimiter = new TransactionLimiter(Integer.parseInt(dotenv.get("ICEBAR_OPENAI_TPM", "1")));
        String promptsInitialBuggyModel = dotenv.get("ICEBAR_OPENAI_PROMPTS_INITIAL_BUGGY_MODEL",
                "The following Alloy model do not satisfy the included oracle, " +
                        "provide a fix modifying only the model without modifying the oracle. " +
                        "Surround the proposed Alloy model with lines containing 5 consecutive equal symbols. " +
                        "Start your response with Yes if you were able to find a suggested model.");
        String promptsStillBuggyModel = dotenv.get("ICEBAR_OPENAI_PROMPTS_STILL_BUGGY_MODEL",
                "Your previous suggestions did not fix the model. " +
                        "I tried to fix the Alloy model as shown below but its still buggy. " +
                        "Provide a fix only modifying the model but not the oracle. " +
                        "Surround the proposed Alloy model with lines containing 5 consecutive equal symbols. " +
                        "Start your response with Yes if you were able to find a suggested model.");
        this.promptsInitialBuggyModel = promptsInitialBuggyModel;
        this.promptsStillBuggyModel = promptsStillBuggyModel;
        this.defineSpecificContext = Boolean.parseBoolean(dotenv.get("ICEBAR_OPENAI_SPECIFY_CONTEXT", "False"));
        this.oraclePrompt = dotenv.get("ICEBAR_OPENAI_PROMPTS_ORACLE",
                "Any mention to 'oracle' will be referring to the following Alloy code");
        this.context = dotenv.get("ICEBAR_OPENAI_CONTEXT", "You are a helpful assistant.");
    }

    public Optional<Path> askForSuggestion(Path model, Path oracle) throws IOException, OpenAIChatConnectorException {
        if (!transactionLimiter.markTransaction()) {
            return Optional.empty();
        }
        String modelCode = Utils.readFile(model);
        String oracleCode = Utils.readFile(oracle);
        if (currentContext++ >= maximumContext) {
            ChatManager.getInstance().clearChat();
            currentContext = 0;
        }
        String suggestionQuery;
        if (ChatManager.getInstance().isChatEmpty()) {
            initializeConversation(oracleCode);
            suggestionQuery = this.promptsInitialBuggyModel;
        } else {
            suggestionQuery = this.promptsStillBuggyModel;
        }
        Message suggestionMessage = new Message(Role.USER, suggestionQuery + "\n" + modelCode);
        Optional<OpenAIResponse> suggestion = ChatManager.getInstance().queryOpenAI(suggestionMessage);
        if (suggestion.isEmpty()) {
            return Optional.empty();
        }
        if (!suggestion.get().isValid()) {
            throw suggestion.get().exception();
        }
        String rawSuggestion = suggestion.get().message().message();
        if (rawSuggestion.startsWith("Yes")) {
            String suggestedModel = getModelFromResponse(rawSuggestion);
            String modelFileName = model.getFileName().toString();
            Path suggestedModelPath = Utils.writeSuggestedModel(suggestionsFolder, modelFileName.replace(".als", ""), suggestedModel);
            return Optional.of(suggestedModelPath);
        } else {
            return Optional.empty();
        }
    }

    private void initializeConversation(String oracleCode) {
        if (defineSpecificContext) {
            ChatManager.getInstance().addRestriction(new Message(Role.SYSTEM, context));
        }
        String oraclePrompt = this.oraclePrompt + "\n" + oracleCode;
        ChatManager.getInstance().addRestriction(new Message(Role.SYSTEM, oraclePrompt));
    }

    private String getModelFromResponse(String rawSuggestion) {
        StringBuilder model = new StringBuilder();
        AtomicBoolean codeStarts = new AtomicBoolean(false);
        rawSuggestion.lines().forEach(line -> {
                if (!codeStarts.get() && !line.startsWith("Yes") && !line.isBlank()) {
                    codeStarts.set(true);
                    model.append(line).append("\n");
                } else if (codeStarts.get()) {
                    model.append(line).append("\n");
                }
        });
        return model.toString();
    }

}
