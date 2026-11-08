package sn.senetech.intervit.common;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource, Object id) {
        super(resource + " introuvable : " + id);
    }
}
