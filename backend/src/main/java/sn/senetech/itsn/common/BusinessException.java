package sn.senetech.itsn.common;

/** Violation d'une règle métier (transition d'état interdite, conflit de planning, budget dépassé...). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
