package ar.edu.unrc.exa.dc.openai.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAIParams {

    @JsonProperty
    private float temperature;
    @JsonProperty
    private int n;
    @JsonProperty("max_tokens")
    private int maxTokens;
    @JsonProperty
    private String model;

    public OpenAIParams(String model, float temperature, int n, int maxTokens) {
        this.model = model;
        this.temperature = temperature;
        this.n = n;
        this.maxTokens = maxTokens;
    }

    public OpenAIParams() {}

    public OpenAIParams temperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public float temperature() { return temperature; }

    public OpenAIParams n(int n) {
        this.n = n;
        return this;
    }

    public int n() { return n; }

    public OpenAIParams maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public int maxTokens() { return maxTokens; }

    public OpenAIParams model(String model) {
        this.model = model;
        return this;
    }

    public String model() { return model; }

}
