/**
 * Hesapta yeterli para olmadığında fırlatılan özel istisna sınıfı.
 * RuntimeException'dan türetilmiştir, dolayısıyla checked exception değildir.
 */
public class InsufficientFundsException extends RuntimeException {

    // Yetersiz bakiye miktarını saklıyoruz (kullanıcıya daha iyi hata mesajı verebilmek için)
    private final double requestedAmount;
    private final double availableBalance;

    public InsufficientFundsException(double requestedAmount, double availableBalance) {
        super(String.format(
            "Yetersiz bakiye! İstenen tutar: %.2f TL, Mevcut bakiye: %.2f TL",
            requestedAmount, availableBalance
        ));
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }
}
