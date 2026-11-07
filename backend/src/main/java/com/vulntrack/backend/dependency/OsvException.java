package com.vulntrack.backend.dependency;

public class OsvException extends RuntimeException {
    public OsvException(Throwable cause) { super("No se ha podido completar la consulta a OSV", cause); }
}
