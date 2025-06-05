package licenta.model;

public enum PaymentMethod {
    CARD("Card de Credit/Debit"),
    CASH("Plată la Fața Locului"),
    BANK_TRANSFER("Transfer Bancar"),
    PAYPAL("PayPal");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}