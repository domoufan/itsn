package sn.senetech.itsn.common;

import java.time.Instant;
import java.util.Map;

/** Format uniforme des erreurs renvoyées par l'API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, Map<String, String> fieldErrors) {
}
