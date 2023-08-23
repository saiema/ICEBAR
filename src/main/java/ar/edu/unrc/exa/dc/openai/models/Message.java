package ar.edu.unrc.exa.dc.openai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    @JsonProperty
    private final Role role;
    @JsonProperty("content")
    private final String message;

    public Message(Role role, String message) {
        this.role = role;
        this.message = message;
    }

    public Role role() { return role; }
    public String message() { return message; }

}
