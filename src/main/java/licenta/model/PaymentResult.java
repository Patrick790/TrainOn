package licenta.model;

/**
 * Clasă comună pentru rezultatele plăților
 * Folosită atât în AutoPaymentService cât și în BookingPrioritizationRestController
 */
public class PaymentResult {
    private boolean successful;
    private Payment payment;
    private String paymentMethod;
    private String message;
    private String errorMessage;

    public PaymentResult() {
        this.successful = false;
    }

    public PaymentResult(boolean successful) {
        this.successful = successful;
    }

    // Metode helper pentru crearea rapidă a rezultatelor
    public static PaymentResult success(Payment payment, String paymentMethod, String message) {
        PaymentResult result = new PaymentResult(true);
        result.setPayment(payment);
        result.setPaymentMethod(paymentMethod);
        result.setMessage(message);
        return result;
    }

    public static PaymentResult failure(String errorMessage) {
        PaymentResult result = new PaymentResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    // Getters și setters
    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "PaymentResult{" +
                "successful=" + successful +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", message='" + message + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", hasPayment=" + (payment != null) +
                '}';
    }
}