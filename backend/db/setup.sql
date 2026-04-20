-- ============================================================
-- Database: Ride-Hailing App (PostgreSQL)
-- ============================================================

-- ============================================================
-- Table: Account
-- ============================================================
DROP TABLE IF EXISTS Account CASCADE;
CREATE TABLE Account (
    ID             SERIAL PRIMARY KEY,
    Email          VARCHAR(255) NOT NULL UNIQUE,
    PhoneNumber    VARCHAR(20)  NOT NULL UNIQUE,
    HashedPassword VARCHAR(255) NOT NULL,
    Role           VARCHAR(50)  NOT NULL CHECK (Role IN ('User', 'Driver', 'Admin')),
    FullName       VARCHAR(255),
    DateOfBirth    DATE,
    Gender         VARCHAR(10)  CHECK (Gender IN ('Male', 'Female', 'Other')),
    Avatar         VARCHAR(500),   -- URL or file path to avatar image
    IsActive       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Driver-only fields (NULL if Role != 'Driver')
    IDCard              VARCHAR(50),
    DriverLicense       VARCHAR(50),
    VehicleRegistration VARCHAR(100),
    DriverStatus        VARCHAR(50) CHECK (DriverStatus IN ('Pending', 'Approved', 'Rejected')),
    RatePoint           NUMERIC(3, 2) DEFAULT 0,   -- average rating score
    CreatedAt      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UpdatedAt      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    DeletedAt      TIMESTAMPTZ
);

-- ============================================================
-- Table: Promotion (defined before Trip for FK reference)
-- ============================================================
DROP TABLE IF EXISTS Promotion CASCADE;
CREATE TABLE Promotion (
    ID            SERIAL PRIMARY KEY,
    Name          VARCHAR(255)   NOT NULL,
    Type          VARCHAR(100)   NOT NULL, -- % or amount
    DiscountValue NUMERIC(10, 2) NOT NULL,
    AdminID       INT NOT NULL REFERENCES Account(ID) ON DELETE RESTRICT,
    CreatedAt     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UpdatedAt     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    DeletedAt     TIMESTAMPTZ
);

-- ============================================================
-- Table: Trip
-- ============================================================
DROP TABLE IF EXISTS Trip CASCADE;
CREATE TABLE Trip (
    ID                  SERIAL PRIMARY KEY,
    Distance            NUMERIC(10, 2) NOT NULL,
    TotalAmount         NUMERIC(12, 2) NOT NULL,
    PickUpLocation      VARCHAR(500)   NOT NULL,
    DestinationLocation VARCHAR(500)   NOT NULL,
    Duration            NUMERIC(10, 2),                    -- in minutes
    TripType            VARCHAR(50)    NOT NULL DEFAULT 'Motor'
                            CHECK (TripType IN ('Motor', 'Taxi')),
    Status              VARCHAR(50)    NOT NULL DEFAULT 'Pending'
                            CHECK (Status IN ('Pending', 'Accepted', 'InProgress', 'Completed', 'Cancelled')),
    PickUpTime          TIMESTAMPTZ,
    CompletedTime       TIMESTAMPTZ,
    CancelReason        TEXT,
    PaymentMethod       VARCHAR(50)    NOT NULL DEFAULT 'Cash'
                            CHECK (PaymentMethod IN ('Cash', 'BankTransfer')),
    PaymentStatus       VARCHAR(50)    NOT NULL DEFAULT 'Unpaid'
                            CHECK (PaymentStatus IN ('Unpaid', 'Paid')),
    UserID              INT NOT NULL REFERENCES Account(ID) ON DELETE RESTRICT,
    DriverID            INT          REFERENCES Account(ID) ON DELETE SET NULL,
    PromotionID         INT          REFERENCES Promotion(ID) ON DELETE SET NULL,
    CreatedAt           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UpdatedAt           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    DeletedAt           TIMESTAMPTZ
);

-- ============================================================
-- Table: Rating
-- Đánh giá sau mỗi chuyến đi (User đánh giá Driver)
-- ============================================================
DROP TABLE IF EXISTS Rating CASCADE;
CREATE TABLE Rating (
    ID        SERIAL PRIMARY KEY,
    TripID    INT NOT NULL REFERENCES Trip(ID) ON DELETE CASCADE,
    RaterID   INT NOT NULL REFERENCES Account(ID) ON DELETE CASCADE,   -- người đánh giá (User)
    RateeID   INT NOT NULL REFERENCES Account(ID) ON DELETE CASCADE,   -- người được đánh giá (Driver)
    Score     NUMERIC(2, 1) NOT NULL CHECK (Score >= 1 AND Score <= 5),
    Comment   TEXT,
    CreatedAt TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UpdatedAt TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    DeletedAt TIMESTAMPTZ,
    UNIQUE (TripID, RaterID)   -- mỗi người chỉ đánh giá 1 lần / chuyến
);

-- ============================================================
-- Table: Notification
-- ============================================================
DROP TABLE IF EXISTS Notification CASCADE;
CREATE TABLE Notification (
    ID        SERIAL PRIMARY KEY,
    AccountID INT NOT NULL REFERENCES Account(ID) ON DELETE CASCADE,
    Title     VARCHAR(255) NOT NULL,
    Body      TEXT         NOT NULL,
    IsRead    BOOLEAN      NOT NULL DEFAULT FALSE,
    CreatedAt TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UpdatedAt TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    DeletedAt TIMESTAMPTZ
);

-- ============================================================
-- Table: ToDriverRequest
-- Yêu cầu nâng cấp tài khoản lên Driver, duyệt bởi Admin
-- ============================================================
DROP TABLE IF EXISTS ToDriverRequest CASCADE;
CREATE TABLE ToDriverRequest (
    ID                  SERIAL PRIMARY KEY,
    IDCard              VARCHAR(50)  NOT NULL,
    DriverLicense       VARCHAR(50)  NOT NULL,
    VehicleRegistration VARCHAR(100) NOT NULL,
    Status              VARCHAR(50)  NOT NULL DEFAULT 'Pending'
                            CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    DriverID            INT NOT NULL REFERENCES Account(ID) ON DELETE CASCADE,
    AdminID             INT          REFERENCES Account(ID) ON DELETE SET NULL,
    CreatedAt           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UpdatedAt           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    DeletedAt           TIMESTAMPTZ
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX idx_account_email           ON Account(Email);
CREATE INDEX idx_account_phone           ON Account(PhoneNumber);
CREATE INDEX idx_account_role            ON Account(Role);
CREATE INDEX idx_account_active          ON Account(IsActive);
CREATE INDEX idx_account_deleted         ON Account(DeletedAt) WHERE DeletedAt IS NULL;

CREATE INDEX idx_trip_user               ON Trip(UserID);
CREATE INDEX idx_trip_driver             ON Trip(DriverID);
CREATE INDEX idx_trip_promotion          ON Trip(PromotionID);
CREATE INDEX idx_trip_type               ON Trip(TripType);
CREATE INDEX idx_trip_status             ON Trip(Status);
CREATE INDEX idx_trip_payment_status     ON Trip(PaymentStatus);
CREATE INDEX idx_trip_deleted            ON Trip(DeletedAt) WHERE DeletedAt IS NULL;

CREATE INDEX idx_rating_trip             ON Rating(TripID);
CREATE INDEX idx_rating_ratee            ON Rating(RateeID);
CREATE INDEX idx_rating_deleted          ON Rating(DeletedAt) WHERE DeletedAt IS NULL;

CREATE INDEX idx_notification_account    ON Notification(AccountID);
CREATE INDEX idx_notification_read       ON Notification(IsRead);
CREATE INDEX idx_notification_deleted    ON Notification(DeletedAt) WHERE DeletedAt IS NULL;

CREATE INDEX idx_todriverrequest_driver  ON ToDriverRequest(DriverID);
CREATE INDEX idx_todriverrequest_admin   ON ToDriverRequest(AdminID);
CREATE INDEX idx_todriverrequest_status  ON ToDriverRequest(Status);
CREATE INDEX idx_todriverrequest_deleted ON ToDriverRequest(DeletedAt) WHERE DeletedAt IS NULL;

CREATE INDEX idx_promotion_admin         ON Promotion(AdminID);
CREATE INDEX idx_promotion_deleted       ON Promotion(DeletedAt) WHERE DeletedAt IS NULL;

-- ============================================================
-- Function: tự động cập nhật UpdatedAt
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.UpdatedAt = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_updated_at_account ON Account CASCADE;
CREATE TRIGGER set_updated_at_account
    BEFORE UPDATE ON Account
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_trip ON Trip CASCADE;
CREATE TRIGGER set_updated_at_trip
    BEFORE UPDATE ON Trip
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_rating ON Rating CASCADE;
CREATE TRIGGER set_updated_at_rating
    BEFORE UPDATE ON Rating
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_notification ON Notification CASCADE;
CREATE TRIGGER set_updated_at_notification
    BEFORE UPDATE ON Notification
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_promotion ON Promotion CASCADE;
CREATE TRIGGER set_updated_at_promotion
    BEFORE UPDATE ON Promotion
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_todriverrequest ON ToDriverRequest CASCADE;
CREATE TRIGGER set_updated_at_todriverrequest
    BEFORE UPDATE ON ToDriverRequest
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
