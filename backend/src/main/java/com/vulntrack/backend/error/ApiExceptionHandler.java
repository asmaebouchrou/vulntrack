package com.vulntrack.backend.error;

import com.vulntrack.backend.dependency.OsvException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OsvException.class)
    ResponseEntity<?> osv(OsvException exception) {
        log.warn("OSV analysis failed", exception);
        return ResponseEntity.status(502).body(Map.of("error", "No se ha podido completar la consulta a OSV"));
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<?> persistence(RuntimeException exception) {
        log.error("Scan persistence failed", exception);
        return ResponseEntity.status(503).body(Map.of("error", "No se ha podido acceder al historial de análisis"));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> incomplete(IllegalStateException exception) {
        log.error("Inconsistent scan results", exception);
        return ResponseEntity.internalServerError().body(Map.of("error", "No se ha podido completar el análisis"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("error", "El archivo supera el tamaño permitido"));
    }
}
