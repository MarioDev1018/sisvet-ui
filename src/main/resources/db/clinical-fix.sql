-- Campos adicionales para el cierre de vacunación.

IF COL_LENGTH('dbo.ClinicalAttention', 'vaccination_completed') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalAttention
    ADD vaccination_completed BIT NOT NULL
        CONSTRAINT DF_ClinicalAttention_VaccinationCompleted
        DEFAULT (0);
END;
GO

-- Control de concurrencia optimista.

IF COL_LENGTH('dbo.ClinicalAttention', 'version') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalAttention
    ADD [version] BIGINT NOT NULL
        CONSTRAINT DF_ClinicalAttention_Version
        DEFAULT (0);
END;
GO

-- Asociación entre vacunación y atención.

IF COL_LENGTH('dbo.ClinicalVaccination', 'attention_id') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalVaccination
    ADD attention_id BIGINT NULL;
END;
GO

-- Integridad referencial.

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_ClinicalVaccination_Attention'
      AND parent_object_id = OBJECT_ID('dbo.ClinicalVaccination')
)
BEGIN
    ALTER TABLE dbo.ClinicalVaccination WITH CHECK
    ADD CONSTRAINT FK_ClinicalVaccination_Attention
    FOREIGN KEY (attention_id)
    REFERENCES dbo.ClinicalAttention(id);
END;
GO

-- Índice de consulta por atención.

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_ClinicalVaccination_Attention'
      AND object_id = OBJECT_ID('dbo.ClinicalVaccination')
)
BEGIN
    CREATE INDEX IX_ClinicalVaccination_Attention
    ON dbo.ClinicalVaccination(attention_id);
END;
GO
