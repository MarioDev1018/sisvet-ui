package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalSpecies;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.SpeciesOption;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightPolicyView;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.WeightPolicyPort;

@Repository
public class SqlWeightPolicyAdapter implements WeightPolicyPort {

    private final JdbcTemplate jdbc;

    public SqlWeightPolicyAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String warning(String species, BigDecimal weight) {
        WeightPolicyView policy = findForSpecies(species);
        if (policy == null) {
            return "";
        }

        if (weight.compareTo(policy.minKg()) < 0 ||
            weight.compareTo(policy.maxKg()) > 0) {
            return "Advertencia: peso fuera del rango orientativo configurado ("
                + policy.minKg().stripTrailingZeros().toPlainString() + " - "
                + policy.maxKg().stripTrailingZeros().toPlainString()
                + " kg); requiere criterio veterinario.";
        }

        return "";
    }

    public WeightPolicyView findForSpecies(String species) {
        if (species == null || species.isBlank()) {
            return null;
        }
        List<String> keys = ClinicalSpecies.matchKeys(species);
        List<WeightPolicyView> all = findAll();
        for (String key : keys) {
            for (WeightPolicyView view : all) {
                if (view.species().equalsIgnoreCase(key)
                        || ClinicalSpecies.canonicalCode(view.species()).equalsIgnoreCase(key)) {
                    return view;
                }
            }
        }
        return null;
    }

    public List<WeightPolicyView> findAll() {
        return jdbc.query(
            "SELECT species, min_kg, max_kg FROM dbo.ClinicalWeightPolicy ORDER BY species",
            (r, n) -> {
                String rawSpecies = r.getString("species");
                return new WeightPolicyView(
                    ClinicalSpecies.canonicalCode(rawSpecies),
                    ClinicalSpecies.displayLabel(rawSpecies),
                    r.getBigDecimal("min_kg"),
                    r.getBigDecimal("max_kg")
                );
            }
        );
    }

    @Override
    public List<SpeciesOption> findRegisteredSpecies() {
        Map<String, String> byCode = new LinkedHashMap<>();
        byCode.put(ClinicalSpecies.CANINE.code(), "Perro (Canino)");
        byCode.put(ClinicalSpecies.FELINE.code(), "Gato (Felino)");

        List<String> registered = jdbc.queryForList(
            """
            SELECT DISTINCT species FROM (
                SELECT species FROM dbo.ClinicalPet
                UNION
                SELECT species FROM dbo.ClinicalWeightPolicy
            ) s
            WHERE species IS NOT NULL AND LTRIM(RTRIM(species)) <> ''
            """,
            String.class
        );

        List<SpeciesOption> custom = new ArrayList<>();
        for (String raw : registered) {
            String code = ClinicalSpecies.canonicalCode(raw);
            if (byCode.containsKey(code) || ClinicalSpecies.OTHER.code().equals(code)) {
                continue;
            }
            String label = ClinicalSpecies.displayLabel(raw);
            byCode.put(code, label);
            custom.add(new SpeciesOption(code, label));
        }

        custom.sort(Comparator.comparing(SpeciesOption::label, String.CASE_INSENSITIVE_ORDER));

        List<SpeciesOption> result = new ArrayList<>();
        result.add(new SpeciesOption(ClinicalSpecies.CANINE.code(), byCode.get(ClinicalSpecies.CANINE.code())));
        result.add(new SpeciesOption(ClinicalSpecies.FELINE.code(), byCode.get(ClinicalSpecies.FELINE.code())));
        result.addAll(custom);
        return result;
    }

    public void configure(
        String species,
        BigDecimal minimum,
        BigDecimal maximum
    ) {
        String canonical = ClinicalSpecies.canonicalCode(species);
        List<String> aliases = ClinicalSpecies.matchKeys(canonical);

        // Actualiza la entrada canonica si existe, o cualquiera equivalente previa.
        int updated = jdbc.update(
            "UPDATE dbo.ClinicalWeightPolicy " +
            "SET min_kg=?,max_kg=? WHERE UPPER(species)=?",
            minimum, maximum, canonical
        );

        if (updated == 0) {
            for (String alias : aliases) {
                int aliasUpdated = jdbc.update(
                    "UPDATE dbo.ClinicalWeightPolicy " +
                    "SET species=?, min_kg=?, max_kg=? WHERE UPPER(species)=?",
                    canonical, minimum, maximum, alias
                );
                if (aliasUpdated > 0) {
                    updated += aliasUpdated;
                    break;
                }
            }
        }

        if (updated == 0) {
            jdbc.update(
                "INSERT INTO dbo.ClinicalWeightPolicy " +
                "(species,min_kg,max_kg) VALUES(?,?,?)",
                canonical, minimum, maximum
            );
        }
    }
}
