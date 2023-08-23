package ar.edu.unrc.exa.dc.openai.connector;
import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.openai.managers.OpenAIResponse;
import ar.edu.unrc.exa.dc.openai.models.Message;
import ar.edu.unrc.exa.dc.openai.models.Role;
import okhttp3.*;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenAIChatConnector {

    public static Optional<OpenAIResponse> queryOpenAI(OpenAIRequest openAIRequest) throws OpenAIChatConnectorException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .callTimeout(0, TimeUnit.SECONDS)
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS).build();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        Optional<String> openAIRequestJSON = openAIRequest.asJson();
        if (openAIRequestJSON.isEmpty())
            throw new OpenAIChatConnectorException("Error generating JSON body", openAIRequest.error());
        RequestBody body = RequestBody.create(openAIRequestJSON.get(), JSON);
        Dotenv dotenv = Dotenv.configure().filename(ICEBARProperties.getInstance().icebarOpenAIEnvFile().toString()).load();
        String openaiSecretKey = dotenv.get("ICEBAR_OPENAI_SECRET_KEY");
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + openaiSecretKey)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                return Optional.empty();
            }
            ObjectMapper mapper = new ObjectMapper();
            String json = response.body().string();
            JsonNode rootNode = mapper.readTree(json);
            JsonNode firstChoiceNode = rootNode.path("choices").path(0);
            JsonNode messageNode = firstChoiceNode.path("message");
            Role role = Role.valueOf(messageNode.path("role").asText().toUpperCase());
            String message = messageNode.path("content").asText();
            Message responseMessage = new Message(role, message);
            openAIRequest.chat().addMessage(responseMessage);
            return Optional.of(OpenAIResponse.validResponse(responseMessage));
        } catch (IOException e) {
            return Optional.of(
                    OpenAIResponse.invalidResponse(
                            new OpenAIChatConnectorException(
                                    "Exception when querying openai Chat Completion API",
                                    e
                            )
                    )
            );
        }
    }

}
