/**
 * Vadesiz (çek) hesabı – belirli bir limite kadar eksi bakiyeye izin verir.
 *
 * Kurallar:
 *  - overdraftLimit kadar borçlanılabilir (örneğin -1000 TL).
 *  - Limit aşılırsa InsufficientFundsException fırlatılır.
 */
public class CheckingAccount extends Account {

    // Maksimum borçlanma limiti (pozitif değer olarak saklanır)
    private double overdraftLimit;

    public CheckingAccount(String accountNumber, String ownerCustomerId,
                           double initialBalance, double overdraftLimit) {
        super(accountNumber, ownerCustomerId, initialBalance);
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("Overdraft limiti negatif olamaz.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Para çeker. Bakiye + overdraft limiti yeterliyse işlem yapılır.
     * Örnek: Bakiye 200 TL, limit 500 TL → maksimum 700 TL çekilebilir.
     */
    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Çekilecek tutar 0'dan büyük olmalıdır.");
        }
        double availableFunds = balance + overdraftLimit; // Çekilebilecek maksimum tutar
        if (amount > availableFunds) {
            throw new InsufficientFundsException(amount, availableFunds);
        }
        balance -= amount;
        recordTransaction(TransactionType.WITHDRAWAL, amount, "Para çekme işlemi");
    }

    // --- Getter / Setter ---

    public double getOverdraftLimit() { return overdraftLimit; }

    public void setOverdraftLimit(double overdraftLimit) {
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("Overdraft limiti negatif olamaz.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Hesabın şu an eksi bakiyede (overdraft) olup olmadığını söyler.
     */
    public boolean isOverdrawn() {
        return balance < 0;
    }

    @Override
    public String getAccountType() {
        return String.format("Vadesiz Hesap (Overdraft Limiti: %.2f TL)", overdraftLimit);
    }
}
