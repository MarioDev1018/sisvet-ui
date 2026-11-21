-- Ejecutar en la base SQL Server de SISVET-CLINIC.
-- Las tablas comerciales deben existir previamente.

IF OBJECT_ID('dbo.ClinicalPet', 'U') IS NULL
CREATE TABLE dbo.ClinicalPet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES dbo.Customer(id),
    name NVARCHAR(100) NOT NULL,
    species NVARCHAR(60) NOT NULL,
    breed NVARCHAR(60) NULL,
    sex VARCHAR(20) NULL,
    birth_date DATE NULL,
    estimated_age_months INT NULL
        CHECK (estimated_age_months >= 0)
);
GO

IF OBJECT_ID('dbo.ClinicalAttention', 'U') IS NULL
CREATE TABLE dbo.ClinicalAttention (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES dbo.ClinicalPet(id),
    type VARCHAR(30) NOT NULL DEFAULT 'MEDICAL',
    priority INT NULL CHECK (priority BETWEEN 1 AND 5),
    urgency_description NVARCHAR(500) NULL,
    priority_assigned_by INT NULL REFERENCES dbo.[User](id),
    state VARCHAR(30) NOT NULL DEFAULT 'EN_ESPERA'
        CHECK (
            state IN (
                'EN_ESPERA',
                'ATENDIENDOSE',
                'EN_BANO',
                'EN_PELUQUERIA',
                'LISTO_RECOJO',
                'ATENDIDO'
            )
        ),
    arrived_at DATETIME2 NULL,
    requires_grooming BIT NOT NULL DEFAULT 0,
    clinical_closed BIT NOT NULL DEFAULT 0,
    consultation_id BIGINT NULL,
    vaccination_completed BIT NOT NULL DEFAULT 0,
    [version] BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2)
);
GO

IF OBJECT_ID('dbo.MedicalConsultation', 'U') IS NULL
CREATE TABLE dbo.MedicalConsultation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    attention_id BIGINT NOT NULL UNIQUE
        REFERENCES dbo.ClinicalAttention(id),
    pet_id BIGINT NOT NULL REFERENCES dbo.ClinicalPet(id),
    veterinarian_id INT NOT NULL REFERENCES dbo.[User](id),
    reason NVARCHAR(300) NOT NULL,
    weight DECIMAL(10,3) NOT NULL CHECK (weight > 0),
    symptoms NVARCHAR(MAX) NULL,
    observations NVARCHAR(MAX) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'COMPLETED')),
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    last_modified_at DATETIME2 NULL,
    last_modified_by INT NULL REFERENCES dbo.[User](id)
);
GO

IF OBJECT_ID('dbo.ClinicalDiagnosis', 'U') IS NULL
CREATE TABLE dbo.ClinicalDiagnosis (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    consultation_id BIGINT NOT NULL
        REFERENCES dbo.MedicalConsultation(id),
    description NVARCHAR(1000) NOT NULL,
    indications NVARCHAR(2000) NOT NULL,
    author_id INT NOT NULL REFERENCES dbo.[User](id),
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    CONSTRAINT CK_ClinicalDiagnosis_Text
        CHECK (
            LEN(LTRIM(RTRIM(description))) > 0
            AND LEN(LTRIM(RTRIM(indications))) > 0
        )
);
GO

IF OBJECT_ID('dbo.ClinicalTreatment', 'U') IS NULL
CREATE TABLE dbo.ClinicalTreatment (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    consultation_id BIGINT NOT NULL
        REFERENCES dbo.MedicalConsultation(id),
    description NVARCHAR(1000) NOT NULL,
    instructions NVARCHAR(2000) NOT NULL,
    product_id BIGINT NULL REFERENCES dbo.Product(id),
    prescribed_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    author_id INT NOT NULL REFERENCES dbo.[User](id)
);
GO

IF OBJECT_ID('dbo.ClinicalProcedure', 'U') IS NULL
CREATE TABLE dbo.ClinicalProcedure (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    consultation_id BIGINT NOT NULL
        REFERENCES dbo.MedicalConsultation(id),
    name NVARCHAR(200) NOT NULL,
    observations NVARCHAR(2000) NULL,
    performed_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    author_id INT NOT NULL REFERENCES dbo.[User](id)
);
GO

IF OBJECT_ID('dbo.ClinicalVaccination', 'U') IS NULL
CREATE TABLE dbo.ClinicalVaccination (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES dbo.ClinicalPet(id),
    consultation_id BIGINT NULL
        REFERENCES dbo.MedicalConsultation(id),
    attention_id BIGINT NULL
        REFERENCES dbo.ClinicalAttention(id),
    vaccine NVARCHAR(200) NOT NULL,
    dose NVARCHAR(100) NOT NULL,
    applied_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    next_dose_date DATE NULL,
    veterinarian_id INT NOT NULL REFERENCES dbo.[User](id)
);
GO

IF OBJECT_ID('dbo.ClinicalReferral', 'U') IS NULL
CREATE TABLE dbo.ClinicalReferral (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    consultation_id BIGINT NOT NULL
        REFERENCES dbo.MedicalConsultation(id),
    reason NVARCHAR(1000) NOT NULL,
    destination NVARCHAR(250) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    author_id INT NOT NULL REFERENCES dbo.[User](id)
);
GO

IF OBJECT_ID('dbo.ClinicalProfessionalSpecialty', 'U') IS NULL
CREATE TABLE dbo.ClinicalProfessionalSpecialty (
    professional_id INT NOT NULL REFERENCES dbo.[User](id),
    specialty NVARCHAR(150) NOT NULL,
    CONSTRAINT PK_ClinicalProfessionalSpecialty
        PRIMARY KEY (professional_id, specialty)
);
GO

IF OBJECT_ID('dbo.ClinicalAppointment', 'U') IS NULL
CREATE TABLE dbo.ClinicalAppointment (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES dbo.ClinicalPet(id),
    professional_id INT NOT NULL REFERENCES dbo.[User](id),
    reason NVARCHAR(500) NOT NULL,
    starts_at DATETIME2 NOT NULL,
    ends_at DATETIME2 NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    specialty NVARCHAR(150) NULL,
    consultation_id BIGINT NULL
        REFERENCES dbo.MedicalConsultation(id),
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    reminder_sent BIT NOT NULL DEFAULT 0,
    CONSTRAINT CK_ClinicalAppointment_Period
        CHECK (ends_at > starts_at)
);
GO

IF OBJECT_ID('dbo.ClinicalFollowUp', 'U') IS NULL
CREATE TABLE dbo.ClinicalFollowUp (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES dbo.ClinicalPet(id),
    consultation_id BIGINT NULL
        REFERENCES dbo.MedicalConsultation(id),
    reason NVARCHAR(500) NOT NULL,
    due_at DATETIME2 NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'REVIEWED', 'COMPLETED')),
    reviewed_at DATETIME2 NULL,
    appointment_id BIGINT NULL
        REFERENCES dbo.ClinicalAppointment(id)
);
GO

IF OBJECT_ID('dbo.ClinicalWeightPolicy', 'U') IS NULL
CREATE TABLE dbo.ClinicalWeightPolicy (
    species NVARCHAR(60) PRIMARY KEY,
    min_kg DECIMAL(10,3) NOT NULL CHECK (min_kg > 0),
    max_kg DECIMAL(10,3) NOT NULL CHECK (max_kg > 0),
    CONSTRAINT CK_ClinicalWeightPolicy_Range
        CHECK (max_kg >= min_kg)
);
GO

IF OBJECT_ID('dbo.ClinicalMedicationDetail', 'U') IS NULL
CREATE TABLE dbo.ClinicalMedicationDetail (
    product_id BIGINT PRIMARY KEY REFERENCES dbo.Product(id),
    active_ingredient NVARCHAR(150) NOT NULL,
    concentration NVARCHAR(100) NOT NULL
);
GO

IF OBJECT_ID('dbo.ClinicalPriorityAudit', 'U') IS NULL
CREATE TABLE dbo.ClinicalPriorityAudit (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    attention_id BIGINT NOT NULL
        REFERENCES dbo.ClinicalAttention(id),
    veterinarian_id INT NOT NULL REFERENCES dbo.[User](id),
    priority INT NOT NULL CHECK (priority BETWEEN 1 AND 5),
    changed_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2)
);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_ClinicalAttention_OpenPet'
      AND object_id = OBJECT_ID('dbo.ClinicalAttention')
)
CREATE UNIQUE INDEX UX_ClinicalAttention_OpenPet
ON dbo.ClinicalAttention(pet_id)
WHERE state <> 'ATENDIDO';
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_ClinicalAppointment_ProfessionalStart'
      AND object_id = OBJECT_ID('dbo.ClinicalAppointment')
)
CREATE INDEX IX_ClinicalAppointment_ProfessionalStart
ON dbo.ClinicalAppointment(professional_id, starts_at)
INCLUDE (ends_at, status);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_ClinicalFollowUp_StatusDue'
      AND object_id = OBJECT_ID('dbo.ClinicalFollowUp')
)
CREATE INDEX IX_ClinicalFollowUp_StatusDue
ON dbo.ClinicalFollowUp(status, due_at);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_ClinicalPet_Owner'
      AND object_id = OBJECT_ID('dbo.ClinicalPet')
)
CREATE INDEX IX_ClinicalPet_Owner
ON dbo.ClinicalPet(owner_id, name);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_Consultation_Pet'
      AND object_id = OBJECT_ID('dbo.MedicalConsultation')
)
CREATE INDEX IX_Consultation_Pet
ON dbo.MedicalConsultation(pet_id, created_at DESC);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_ClinicalAttention_Consultation'
)
BEGIN
    ALTER TABLE dbo.ClinicalAttention WITH CHECK
    ADD CONSTRAINT FK_ClinicalAttention_Consultation
    FOREIGN KEY (consultation_id)
    REFERENCES dbo.MedicalConsultation(id);
END;
GO

IF OBJECT_ID('dbo.ClinicalCharge', 'U') IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM dbo.ClinicalCharge c
    LEFT JOIN dbo.MedicalConsultation m
        ON m.id = c.consultation_id
    WHERE c.consultation_id IS NOT NULL
      AND m.id IS NULL
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_ClinicalCharge_MedicalConsultation'
)
BEGIN
    ALTER TABLE dbo.ClinicalCharge WITH CHECK
    ADD CONSTRAINT FK_ClinicalCharge_MedicalConsultation
    FOREIGN KEY (consultation_id)
    REFERENCES dbo.MedicalConsultation(id);
END;
GO

IF OBJECT_ID('dbo.Sale', 'U') IS NOT NULL
AND COL_LENGTH('dbo.Sale', 'consultation_id') IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM dbo.Sale s
    LEFT JOIN dbo.MedicalConsultation m
        ON m.id = s.consultation_id
    WHERE s.consultation_id IS NOT NULL
      AND m.id IS NULL
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Sale_MedicalConsultation'
)
BEGIN
    ALTER TABLE dbo.Sale WITH CHECK
    ADD CONSTRAINT FK_Sale_MedicalConsultation
    FOREIGN KEY (consultation_id)
    REFERENCES dbo.MedicalConsultation(id);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Role WHERE name = 'RECEPCIONISTA'
)
BEGIN
    INSERT INTO dbo.Role (name)
    VALUES ('RECEPCIONISTA');
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.Role WHERE name = 'VETERINARIO'
)
BEGIN
    INSERT INTO dbo.Role (name)
    VALUES ('VETERINARIO');
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.Role WHERE name = 'ESPECIALISTA'
)
BEGIN
    INSERT INTO dbo.Role (name)
    VALUES ('ESPECIALISTA');
END;
GO
