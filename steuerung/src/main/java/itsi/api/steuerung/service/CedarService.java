package itsi.api.steuerung.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Cedar Policy Authorization Service
 *
 * HINWEIS: Dieser Service ist vorerst deaktiviert, weil die cedar-java Bibliothek
 * eine native Bibliothek (cedar_java_ffi.dll) benötigt, die nicht automatisch geladen wird.
 *
 * Um Cedar zu aktivieren:
 * 1. Kommentiere @Service wieder ein
 * 2. Füge die Cedar-Imports wieder hinzu
 * 3. Stelle sicher, dass die native cedar_java_ffi Bibliothek im java.library.path ist
 *
 * Für jetzt wird eine einfache Platzhalter-Implementation verwendet.
 */
// @Service  // Vorerst deaktiviert wegen fehlender nativer Bibliothek
@Service
@Slf4j
public class CedarService {

    @Value("classpath:policy/CedarPolicy.cedar")
    private Resource policyResource;

    public CedarService() {
        log.warn("CedarService ist deaktiviert - Authorization wird vorerst nicht durchgeführt");
    }

    /**
     * Prüft ob ein Subject die Aktion auf der Resource ausführen darf.
     *
     * @param subject Der Benutzer (z.B. "user123")
     * @param action Die Aktion (z.B. "start", "stop", "reset")
     * @param resource Die Resource (z.B. "container456")
     * @return true wenn erlaubt, false sonst
     */
    public boolean authorize(String subject, String action, String resource) {
        // Temporäre Implementierung - alle Requests erlauben
        log.debug("Authorization Check (DEAKTIVIERT): subject={}, action={}, resource={} -> ERLAUBT",
                  subject, action, resource);
        return true;
    }
}

