package com.donkan.sisvetclinic.modules.clinical.domain.state;

public final class AttentionStates {

    private AttentionStates() {}

    public static AttentionState fromName(String name) {
        return switch (name) {
            case "EN_ESPERA" -> new WaitingState();
            case "ATENDIENDOSE" -> new InProgressState();
            case "EN_BANO" -> new BathingState();
            case "EN_PELUQUERIA" -> new GroomingState();
            case "LISTO_RECOJO" -> new ReadyForPickupState();
            case "ATENDIDO" -> new PickedUpState();

            default -> throw new IllegalArgumentException(
                "Estado de atención desconocido: " + name
            );
        };
    }
}
