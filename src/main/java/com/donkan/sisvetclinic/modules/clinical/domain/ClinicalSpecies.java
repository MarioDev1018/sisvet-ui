package com.donkan.sisvetclinic.modules.clinical.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public enum ClinicalSpecies {
    CANINE("CANINE", "Perro"),
    FELINE("FELINE", "Gato"),
    OTHER("OTHER", "Otra especie");

    private final String code;
    private final String label;

    ClinicalSpecies(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ClinicalSpecies fromInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .trim();

        if (normalized.equals("CANINE") || normalized.equals("CANINO")
                || normalized.equals("PERRO") || normalized.equals("DOG")) {
            return CANINE;
        }
        if (normalized.equals("FELINE") || normalized.equals("FELINO")
                || normalized.equals("GATO") || normalized.equals("CAT")) {
            return FELINE;
        }
        return OTHER;
    }

    public static String canonicalCode(String raw) {
        ClinicalSpecies matched = fromInput(raw);
        if (matched == CANINE || matched == FELINE) {
            return matched.code;
        }
        return raw == null ? OTHER.code : raw.trim().toUpperCase(Locale.ROOT);
    }

    public static String displayLabel(String raw) {
        ClinicalSpecies matched = fromInput(raw);
        if (matched == CANINE || matched == FELINE) {
            return matched.label;
        }
        if (raw == null || raw.isBlank()) {
            return OTHER.label;
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(clean.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isWhitespace(c) || c == '-' || c == '/') {
                out.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static List<String> matchKeys(String raw) {
        ClinicalSpecies matched = fromInput(raw);
        return switch (matched) {
            case CANINE -> List.of("CANINE", "CANINO", "PERRO", "DOG");
            case FELINE -> List.of("FELINE", "FELINO", "GATO", "CAT");
            case OTHER -> raw == null ? List.of("OTHER") : List.of(raw.trim().toUpperCase(Locale.ROOT));
        };
    }
}
