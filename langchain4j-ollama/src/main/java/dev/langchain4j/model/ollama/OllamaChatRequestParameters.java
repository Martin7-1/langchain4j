package dev.langchain4j.model.ollama;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.quoted;

import dev.langchain4j.Experimental;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import java.util.Objects;

@Experimental
public class OllamaChatRequestParameters extends DefaultChatRequestParameters {

    private final Integer mirostat;
    private final Double mirostatEta;
    private final Double mirostatTau;
    private final Integer numCtx;
    private final Integer repeatLastN;
    private final Double repeatPenalty;
    private final Integer seed;
    private final Integer numPredict;
    private final Double minP;

    private OllamaChatRequestParameters(Builder builder) {
        super(builder);
        this.mirostat = builder.mirostat;
        this.mirostatEta = builder.mirostatEta;
        this.mirostatTau = builder.mirostatTau;
        this.numCtx = builder.numCtx;
        this.repeatLastN = builder.repeatLastN;
        this.repeatPenalty = builder.repeatPenalty;
        this.seed = builder.seed;
        this.numPredict = builder.numPredict;
        this.minP = builder.minP;
    }

    public Integer mirostat() {
        return mirostat;
    }

    public Double mirostatEta() {
        return mirostatEta;
    }

    public Double mirostatTau() {
        return mirostatTau;
    }

    public Integer numCtx() {
        return numCtx;
    }

    public Integer repeatLastN() {
        return repeatLastN;
    }

    public Double repeatPenalty() {
        return repeatPenalty;
    }

    public Integer seed() {
        return seed;
    }

    public Integer numPredict() {
        return numPredict;
    }

    public Double minP() {
        return minP;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OllamaChatRequestParameters that = (OllamaChatRequestParameters) o;
        return Objects.equals(mirostat, that.mirostat)
                && Objects.equals(mirostatEta, that.mirostatEta)
                && Objects.equals(mirostatTau, that.mirostatTau)
                && Objects.equals(numCtx, that.numCtx)
                && Objects.equals(repeatLastN, that.repeatLastN)
                && Objects.equals(repeatPenalty, that.repeatPenalty)
                && Objects.equals(seed, that.seed)
                && Objects.equals(numPredict, that.numPredict)
                && Objects.equals(minP, that.minP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                mirostat,
                mirostatEta,
                mirostatTau,
                numCtx,
                repeatLastN,
                repeatPenalty,
                seed,
                numPredict,
                minP);
    }

    @Override
    public String toString() {
        return "OllamaChatRequestParameters{" + "modelName="
                + quoted(modelName()) + ", temperature="
                + temperature() + ", topP="
                + topP() + ", topK="
                + topK() + ", frequencyPenalty="
                + frequencyPenalty() + ", presencePenalty="
                + presencePenalty() + ", maxOutputTokens="
                + maxOutputTokens() + ", stopSequences="
                + stopSequences() + ", toolSpecifications="
                + toolSpecifications() + ", toolChoice="
                + toolChoice() + ", responseFormat="
                + responseFormat() + ", mirostat="
                + mirostat + ", mirostatEta="
                + mirostatEta + ", mirostatTau="
                + mirostatTau + ", numCtx="
                + numCtx + ", repeatLastN="
                + repeatLastN + ", repeatPenalty="
                + repeatPenalty + ", seed="
                + seed + ", numPredict="
                + numPredict + ", minP="
                + minP + '}';
    }

    @Override
    public OllamaChatRequestParameters overrideWith(ChatRequestParameters that) {
        return OllamaChatRequestParameters.builder()
                .overrideWith(this)
                .overrideWith(that)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DefaultChatRequestParameters.Builder<Builder> {

        private Integer mirostat;
        private Double mirostatEta;
        private Double mirostatTau;
        private Integer numCtx;
        private Integer repeatLastN;
        private Double repeatPenalty;
        private Integer seed;
        private Integer numPredict;
        private Double minP;

        @Override
        public Builder overrideWith(ChatRequestParameters parameters) {
            super.overrideWith(parameters);
            if (parameters instanceof OllamaChatRequestParameters ollamaChatRequestParameters) {
                mirostat(getOrDefault(ollamaChatRequestParameters.mirostat, mirostat));
                mirostatEta(getOrDefault(ollamaChatRequestParameters.mirostatEta, mirostatEta));
                mirostatTau(getOrDefault(ollamaChatRequestParameters.mirostatTau, mirostatTau));
                numCtx(getOrDefault(ollamaChatRequestParameters.numCtx, numCtx));
                repeatLastN(getOrDefault(ollamaChatRequestParameters.repeatLastN, repeatLastN));
                repeatPenalty(getOrDefault(ollamaChatRequestParameters.frequencyPenalty(), repeatPenalty));
                seed(getOrDefault(ollamaChatRequestParameters.seed, seed));
                numPredict(getOrDefault(ollamaChatRequestParameters.numPredict, numPredict));
                minP(getOrDefault(ollamaChatRequestParameters.minP, minP));
            }
            return this;
        }

        /**
         * Enable Mirostat sampling for controlling perplexity.
         * <p>Default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0</p>
         *
         * @return builder
         */
        public Builder mirostat(Integer mirostat) {
            this.mirostat = mirostat;
            return this;
        }

        /**
         * Influences how quickly the algorithm responds to feedback from the generated text.
         * <p>A lower learning rate will result in slower adjustments, while a higher learning rate will make the algorithm more responsive.</p>
         * <p>Default: 0.1</p>
         *
         * @return builder
         */
        public Builder mirostatEta(Double mirostatEta) {
            this.mirostatEta = mirostatEta;
            return this;
        }

        /**
         * Controls the balance between coherence and diversity of the output.
         * <p>A lower value will result in more focused and coherent text.</p>
         * <p>Default: 5.0</p>
         *
         * @return builder
         */
        public Builder mirostatTau(Double mirostatTau) {
            this.mirostatTau = mirostatTau;
            return this;
        }

        public Builder numCtx(Integer numCtx) {
            this.numCtx = numCtx;
            return this;
        }

        public Builder repeatLastN(Integer repeatLastN) {
            this.repeatLastN = repeatLastN;
            return this;
        }

        public Builder repeatPenalty(Double repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder numPredict(Integer numPredict) {
            this.numPredict = numPredict;
            return this;
        }

        /**
         * Alternative to the {@code topP}, and aims to ensure a balance of quality and variety. The parameter p represents the minimum probability for a token to be considered, relative to the probability of the most likely token.
         * <p>For example, with p=0.05 and the most likely token having a probability of 0.9, logits with a value less than 0.045 are filtered out.</p>
         * <p>Default: 0.0</p>
         *
         * @return builder
         */
        public Builder minP(Double minP) {
            this.minP = minP;
            return this;
        }

        @Override
        public OllamaChatRequestParameters build() {
            return new OllamaChatRequestParameters(this);
        }
    }
}
