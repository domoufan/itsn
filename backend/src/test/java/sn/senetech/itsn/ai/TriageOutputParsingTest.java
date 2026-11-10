package sn.senetech.itsn.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TU-04 : extraction robuste du JSON final de l'agent (exigence EF-12). */
class TriageOutputParsingTest {

    @Test
    void extractsPlainJson() {
        assertThat(ClaudeTriageAgent.extractJson("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void extractsJsonFromCodeFence() {
        String text = "```json\n{\"categoryCode\":\"NETWORK\",\"impact\":2}\n```";
        assertThat(ClaudeTriageAgent.extractJson(text)).isEqualTo("{\"categoryCode\":\"NETWORK\",\"impact\":2}");
    }

    @Test
    void extractsJsonSurroundedByText() {
        String text = "Voici ma proposition : {\"categoryCode\":\"EMAIL\",\"impact\":3} Fin.";
        assertThat(ClaudeTriageAgent.extractJson(text)).isEqualTo("{\"categoryCode\":\"EMAIL\",\"impact\":3}");
    }

    @Test
    void failsWithoutJson() {
        assertThatThrownBy(() -> ClaudeTriageAgent.extractJson("Je ne peux pas répondre."))
            .isInstanceOf(ClaudeTriageAgent.ValidationException.class);
    }
}
