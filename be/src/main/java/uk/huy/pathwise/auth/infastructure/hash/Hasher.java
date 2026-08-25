package uk.huy.pathwise.auth.infastructure.hash;

public interface Hasher {
    String hashString(String rawString);
}
