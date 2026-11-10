package sn.senetech.itsn.ai;

/** Résultat d'une exécution de l'agent : sortie validée + métriques d'exécution. */
public record TriageResult(TriageOutput output, String model, int toolCalls, int inputTokens, int outputTokens,
                           long latencyMs, String rawResponse, String error) {

    public boolean isSuccess() { return error == null && output != null; }

    public static TriageResult failure(String model, int toolCalls, int in, int out, long latency, String raw, String error) {
        return new TriageResult(null, model, toolCalls, in, out, latency, raw, error);
    }
}
