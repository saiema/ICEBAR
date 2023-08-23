package ar.edu.unrc.exa.dc.openai.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Chat {
    @JsonProperty
    private final List<Message> messages;

    public Chat() {
        this.messages = new LinkedList<>();
    }

    public List<Message> messages() { return messages; }

    public void addMessage(Message newMessage) {
        messages.add(newMessage);
    }

    public Optional<Message> lastMessage() {
        if (!messages.isEmpty()) {
            return Optional.of(messages.get(messages.size() - 1));
        }
        return Optional.empty();
    }

    public void clear() {
        messages.clear();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            sb.append("Role: ").append(message.role()).append("\n");
            sb.append("Message: ").append(message.message()).append("\n");
            sb.append("============").append("\n");
        }
        return sb.toString();
    }

}
