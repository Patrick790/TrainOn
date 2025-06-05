package licenta.model;

public enum PaymentStatus {
    PENDING("În Așteptare"),
    PROCESSING("Se Procesează"),
    SUCCEEDED("Reușită"),
    FAILED("Eșuată"),
    CANCELLED("Anulată"),
    REFUNDED("Rambursată");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}