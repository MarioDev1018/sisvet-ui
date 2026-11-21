package com.donkan.sisvetclinic.modules.clinical.service.port.out;

public interface ProfessionalLookupPort {

    boolean canAttend(long professionalId);

    boolean hasSpecialty(
        long professionalId,
        String specialty
    );

    default void assignSpecialty(long professionalId, String specialty) {
        throw new UnsupportedOperationException("No soportado");
    }

    default void removeSpecialty(long professionalId, String specialty) {
        throw new UnsupportedOperationException("No soportado");
    }
}
