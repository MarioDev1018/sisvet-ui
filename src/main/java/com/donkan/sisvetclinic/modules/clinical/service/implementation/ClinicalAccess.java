package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.text.Normalizer;
import java.util.Locale;
import com.donkan.sisvetclinic.modules.clinical.domain.Actor;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.ActorPort;

public final class ClinicalAccess {

    private ClinicalAccess() {}

    public static Actor require(ActorPort actors, String... allowed) {
        Actor actor = actors.current();
        String actual = normalize(actor.role());

        for (String role : allowed) {
            if (actual.equals(normalize(role))) {
                return actor;
            }
        }

        throw new SecurityException(
            "No tiene permisos para esta operación clínica"
        );
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace("ROLE_", "")
            .toUpperCase(Locale.ROOT)
            .trim();
    }
}
