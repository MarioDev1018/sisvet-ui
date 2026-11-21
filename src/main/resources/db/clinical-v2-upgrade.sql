-- Actualizacion estructural v2 para Vacunas en inventario, Cargos de vacunacion,
-- Sesiones de pago electronico clinico (Yape / PayPal) y Log de notificaciones.

-- 1. Categoria Vacunas
IF NOT EXISTS (SELECT 1 FROM dbo.Category WHERE name = 'Vacunas')
BEGIN
    INSERT INTO dbo.Category (name, record_status)
    VALUES ('Vacunas', 'Active');
END;
GO

-- 2. Cargos clinicos opcionalmente ligados a atencion (para vacunaciones independientes)
IF COL_LENGTH('dbo.ClinicalCharge', 'attention_id') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalCharge
    ADD attention_id BIGINT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_ClinicalCharge_Attention'
      AND parent_object_id = OBJECT_ID('dbo.ClinicalCharge')
)
BEGIN
    ALTER TABLE dbo.ClinicalCharge WITH CHECK
    ADD CONSTRAINT FK_ClinicalCharge_Attention
    FOREIGN KEY (attention_id) REFERENCES dbo.ClinicalAttention(id);
END;
GO

IF EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'ClinicalCharge' AND COLUMN_NAME = 'consultation_id' AND IS_NULLABLE = 'NO'
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_ClinicalCharge_Consultation'
          AND object_id = OBJECT_ID('dbo.ClinicalCharge')
    )
    BEGIN
        DROP INDEX IX_ClinicalCharge_Consultation ON dbo.ClinicalCharge;
    END;

    ALTER TABLE dbo.ClinicalCharge
    ALTER COLUMN consultation_id BIGINT NULL;

    CREATE INDEX IX_ClinicalCharge_Consultation
    ON dbo.ClinicalCharge(consultation_id, is_charged);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ClinicalCharge_Attention'
      AND object_id = OBJECT_ID('dbo.ClinicalCharge')
)
BEGIN
    CREATE INDEX IX_ClinicalCharge_Attention
    ON dbo.ClinicalCharge(attention_id, is_charged);
END;
GO

-- 3. Vinculo de producto y cantidad descontada en vacunaciones
IF COL_LENGTH('dbo.ClinicalVaccination', 'product_id') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalVaccination
    ADD product_id BIGINT NULL;
END;
GO

IF COL_LENGTH('dbo.ClinicalVaccination', 'quantity_used') IS NULL
BEGIN
    ALTER TABLE dbo.ClinicalVaccination
    ADD quantity_used INT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_ClinicalVaccination_Product'
      AND parent_object_id = OBJECT_ID('dbo.ClinicalVaccination')
)
BEGIN
    ALTER TABLE dbo.ClinicalVaccination WITH CHECK
    ADD CONSTRAINT FK_ClinicalVaccination_Product
    FOREIGN KEY (product_id) REFERENCES dbo.Product(id);
END;
GO

-- 4. Sesiones de pago electronico para cobros clinicos (Yape y PayPal)
IF OBJECT_ID('dbo.ClinicalPaymentSession', 'U') IS NULL
CREATE TABLE dbo.ClinicalPaymentSession (
    operation_key VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    consultation_id BIGINT NULL REFERENCES dbo.MedicalConsultation(id),
    attention_id BIGINT NULL REFERENCES dbo.ClinicalAttention(id),
    customer_id BIGINT NOT NULL REFERENCES dbo.Customer(id),
    payment_method_id BIGINT NOT NULL REFERENCES dbo.PaymentMethod(id),
    provider_code VARCHAR(30) NOT NULL,
    charge_ids NVARCHAR(500) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL CHECK (total_amount > 0),
    request_fingerprint CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_id VARCHAR(64) NULL,
    sale_id BIGINT NULL REFERENCES dbo.Sale(id),
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2),
    updated_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2)
);
GO

-- 5. Bitacora persistente de notificaciones enviadas al cliente
IF OBJECT_ID('dbo.NotificationLog', 'U') IS NULL
CREATE TABLE dbo.NotificationLog (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    recipient_email NVARCHAR(150) NULL,
    channel VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(120) NULL,
    subject NVARCHAR(200) NOT NULL,
    body NVARCHAR(2000) NOT NULL,
    sent_ok BIT NOT NULL DEFAULT 1,
    detail NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2)
);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_NotificationLog_IdempotencyKey'
      AND object_id = OBJECT_ID('dbo.NotificationLog')
)
CREATE UNIQUE INDEX UX_NotificationLog_IdempotencyKey
ON dbo.NotificationLog(idempotency_key)
WHERE idempotency_key IS NOT NULL;
GO
