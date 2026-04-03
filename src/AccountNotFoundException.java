/**
 * Aranan hesap sistemde bulunamadığında fırlatılan istisna.
 * Hem hesap numarası hem de müşteri ID'sine göre arama yapılabilir.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Hesap bulunamadı: " + accountNumber);
    }

    public AccountNotFoundException(String message, boolean isCustomMessage) {
        super(message);
    }
}
