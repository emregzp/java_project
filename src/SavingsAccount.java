/**
 * Tasarruf hesabı – faiz işleyen, para çekiminde sınır olan hesap türü.
 *
 * Kurallar:
 *  - Bakiye hiçbir zaman 0'ın altına düşemez.
 *  - Belirlenen faiz oranıyla periyodik faiz uygulanabilir.
 */
public class SavingsAccount extends Account {

    // Varsayılan yıllık faiz oranı (örneğin %5 için 0.05)
    private double annualInterestRate;

    public SavingsAccount(String accountNumber, String ownerCustomerId,
                          double initialBalance, double annualInterestRate) {
        super(accountNumber, ownerCustomerId, initialBalance);
        if (annualInterestRate < 0) {
            throw new IllegalArgumentException("Faiz oranı negatif olamaz.");
        }
        this.annualInterestRate = annualInterestRate;
    }

    /**
     * Para çeker. Bakiye yetersizse InsufficientFundsException fırlatır.
     * Tasarruf hesabında eksi bakiyeye izin verilmez.
     */
    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Çekilecek tutar 0'dan büyük olmalıdır.");
        }
        if (amount > balance) {
            throw new InsufficientFundsException(amount, balance);
        }
        balance -= amount;
        recordTransaction(TransactionType.WITHDRAWAL, amount, "Para çekme işlemi");
    }

    /**
     * Aylık faizi hesaplayıp hesaba ekler.
     * Örnek: Yıllık %12 faiz → aylık %1 faiz.
     */
    public void applyMonthlyInterest() {
        double monthlyRate = annualInterestRate / 12;
        double interest = balance * monthlyRate;
        if (interest > 0) {
            balance += interest;
            recordTransaction(TransactionType.INTEREST, interest,
                String.format("Aylık faiz (yıllık %%%.1f)", annualInterestRate * 100));
        }
    }

    // --- Getter / Setter ---

    public double getAnnualInterestRate() { return annualInterestRate; }

    public void setAnnualInterestRate(double annualInterestRate) {
        if (annualInterestRate < 0) {
            throw new IllegalArgumentException("Faiz oranı negatif olamaz.");
        }
        this.annualInterestRate = annualInterestRate;
    }

    @Override
    public String getAccountType() {
        return String.format("Tasarruf Hesabı (Yıllık Faiz: %%%.1f)", annualInterestRate * 100);
    }
}
