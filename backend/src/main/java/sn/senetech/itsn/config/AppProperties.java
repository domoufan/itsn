package sn.senetech.itsn.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/**
 * Paramètres applicatifs (préfixe {@code itsn}). Les secrets proviennent exclusivement
 * de variables d'environnement : ils ne sont jamais versionnés.
 */
@Validated
@ConfigurationProperties(prefix = "itsn")
public record AppProperties(Security security, Notifications notifications, Ai ai, Demo demo) {

    public record Security(
            @NotBlank @Size(min = 32, message = "Le secret JWT doit comporter au moins 32 caractères") String jwtSecret,
            @Min(5) int jwtExpirationMinutes,
            List<String> corsAllowedOrigins) {
    }

    public record Notifications(boolean emailEnabled, String from) {
    }

    public record Ai(
            boolean enabled,
            String provider,
            String apiKey,
            String baseUrl,
            String model,
            int maxTokens,
            int maxToolCalls,
            int timeoutSeconds,
            BigDecimal dailyBudgetUsd,
            BigDecimal priceInputPerMtok,
            BigDecimal priceOutputPerMtok) {
    }

    public record Demo(boolean seedEnabled) {
    }
}
