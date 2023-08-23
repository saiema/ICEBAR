package ar.edu.unrc.exa.dc.openai.managers;

import ar.edu.unrc.exa.dc.openai.connector.OpenAIChatConnector;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIParams;
import ar.edu.unrc.exa.dc.openai.connector.OpenAIRequest;
import ar.edu.unrc.exa.dc.openai.models.Chat;
import ar.edu.unrc.exa.dc.openai.models.Message;
import ar.edu.unrc.exa.dc.openai.models.Role;
import ar.edu.unrc.exa.dc.util.Utils;

import java.util.Optional;

public class test {

    public static void main(String[] args) {
        Message hello = new Message(Role.USER, "Hello, my name is Giovanni Giorgio, but everybody calls me Giorgio");
        Chat chat = new Chat();
        chat.addMessage(hello);
        OpenAIParams openAIParams = new OpenAIParams();
        OpenAIRequest openAIRequest = new OpenAIRequest(openAIParams, chat);
        checkIfResponseIsInvalid(openAIRequest);
        Message whatIsMyName = new Message(Role.USER, "How does everybody calls me?");
        chat.addMessage(whatIsMyName);
        checkIfResponseIsInvalid(openAIRequest);
        System.out.println(chat);
    }

    private static void checkIfResponseIsInvalid(OpenAIRequest openAIRequest) {
        Optional<OpenAIResponse> responseHello = OpenAIChatConnector.queryOpenAI(openAIRequest);
        if (responseHello.isEmpty() || !responseHello.get().isValid()) {
            System.out.println("Failed to generate response");
            responseHello.ifPresent(openAIResponse -> System.out.println(Utils.exceptionToString(openAIResponse.exception())));
            System.exit(1);
        }
    }

}
