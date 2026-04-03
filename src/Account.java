import java.util.ArrayList;
import java.util.List;

/**
 * Tüm hesap türlerinin temel sınıfı.
 * Soyut (abstract) olduğu için doğrudan nesnesi oluşturulamaz;
 * SavingsAccount veya CheckingAccount gibi alt sınıflar üzerinden kullanılır.
 */
public abstract class Account {

    private final String accountNumber;   // Hesap numarası değişmez
    private final String ownerCustomerId; // Hesabın sahibi olan müşteri ID'si
    protected double balance;             // Alt sınıflar da erişebilir
    private final List<Transaction> transactions; // İşlem geçmişi

    public Account(String accountNumber, String ownerCustomerId, double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Başlangıç bakiyesi negatif olamaz.");
        }
        this.accountNumber = accountNumber;
        this.ownerCustomerId = ownerCustomerId;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
    }

    // --- Para yatırma ---

    /**
     * Hesaba para yatırır. Yatırılacak tutar sıfırdan büyük olmalıdır.
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Yatırılacak tutar 0'dan büyük olmalıdır.");
        }
        balance += amount;
        recordTransaction(TransactionType.DEPOSIT, amount, "Para yatırma işlemi");
    }

    // --- Para çekme (soyut – alt sınıflar kendi kurallarını uygular) ---

    /**
     * Her hesap türü para çekmeyi farklı biçimde ele alabilir.
     * Örneğin vadesiz hesapta overdraft olabilirken, tasarruf hesabında olmaz.
     */
    public abstract void withdraw(double amount);

    // --- Transfer ---

    /**
     * Bu hesaptan başka bir hesaba para gönderir.
     * Çekme işlemi bu hesabın kendi kurallarına göre yapılır.
     */
    public void transferTo(Account targetAccount, double amount) {
        this.withdraw(amount); // Kendi hesabımızdan çek (kural kontrolü burada)
        targetAccount.receiveTransfer(amount, this.accountNumber);
        // Çekme kaydı withdraw() içinde yapıldığından sadece gönderme notunu güncelliyoruz
        // En son eklenen işlemi "Transfer Gönderildi" olarak işaretle
        replaceLastTransactionType(TransactionType.TRANSFER_OUT,
            String.format("%s hesabına transfer", targetAccount.getAccountNumber()));
    }

    /**
     * Başka bir hesaptan gelen transferi kabul eder.
     * Bu metodu dışarıdan çağırmak yerine transferTo üzerinden kullanın.
     */
    void receiveTransfer(double amount, String fromAccountNumber) {
        balance += amount;
        recordTransaction(TransactionType.TRANSFER_IN, amount,
            String.format("%s hesabından transfer alındı", fromAccountNumber));
    }

    // --- Yardımcı metodlar ---

    /**
     * Yeni bir işlem kaydı oluşturur ve geçmişe ekler.
     */
    protected void recordTransaction(TransactionType type, double amount, String description) {
        transactions.add(new Transaction(type, amount, balance, description));
    }

    /**
     * Son işlemin türünü ve açıklamasını günceller (transfer durumunda kullanılır).
     */
    private void replaceLastTransactionType(TransactionType newType, String newDescription) {
        if (!transactions.isEmpty()) {
            Transaction last = transactions.get(transactions.size() - 1);
            transactions.set(transactions.size() - 1,
                new Transaction(newType, last.getAmount(), last.getBalanceAfter(), newDescription));
        }
    }

    // --- Hesap bilgisi ---

    /**
     * Tüm işlem geçmişini yazdırır.
     */
    public void printStatement() {
        System.out.println("=== Hesap Ekstresi: " + accountNumber + " ===");
        if (transactions.isEmpty()) {
            System.out.println("  Henüz işlem yapılmamış.");
        } else {
            for (Transaction t : transactions) {
                System.out.println("  " + t);
            }
        }
        System.out.printf("  Güncel Bakiye: %.2f TL%n", balance);
    }

    // --- Getter'lar ---

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerCustomerId() { return ownerCustomerId; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }

    /**
     * Her alt sınıf hesap türünü açıklayan bir metin döndürmelidir.
     */
    public abstract String getAccountType();

    @Override
    public String toString() {
        return String.format("%s [%s] | Bakiye: %.2f TL | Tür: %s",
            accountNumber, ownerCustomerId, balance, getAccountType());
    }
}
