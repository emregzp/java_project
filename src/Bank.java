import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Bankanın merkezi sınıfı – müşteri ve hesap işlemlerini yönetir.
 *
 * Sorumlulukları:
 *  - Müşteri kayıt / silme
 *  - Hesap açma / kapatma
 *  - Para yatır / çek / transfer
 *  - Raporlama
 */
public class Bank {

    private final String bankName;

    // Hızlı erişim için Map yapıları kullanıyoruz
    private final Map<String, Customer> customers;   // customerId → Customer
    private final Map<String, Account>  accounts;    // accountNumber → Account

    // Hesap numarası üreteci (iş parçacığı güvenli)
    private final AtomicInteger accountCounter = new AtomicInteger(1000);

    public Bank(String bankName) {
        this.bankName  = bankName;
        this.customers = new HashMap<>();
        this.accounts  = new HashMap<>();
    }

    // =========================================================
    //  MÜŞTERİ İŞLEMLERİ
    // =========================================================

    /**
     * Yeni bir müşteri kaydeder.
     * Aynı ID'ye sahip müşteri zaten varsa hata fırlatır.
     */
    public Customer registerCustomer(String customerId, String firstName, String lastName,
                                     String email, String phone) {
        if (customers.containsKey(customerId)) {
            throw new IllegalArgumentException("Bu ID'ye sahip müşteri zaten mevcut: " + customerId);
        }
        Customer customer = new Customer(customerId, firstName, lastName, email, phone);
        customers.put(customerId, customer);
        System.out.printf("✓ Müşteri kaydedildi: %s%n", customer.getFullName());
        return customer;
    }

    /**
     * Sistemdeki müşteriyi getirir. Bulunamazsa boş Optional döner.
     */
    public Optional<Customer> findCustomer(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    /**
     * Tüm müşterileri listeler.
     */
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }

    // =========================================================
    //  HESAP İŞLEMLERİ
    // =========================================================

    /**
     * Müşteri için yeni bir tasarruf hesabı açar.
     * Hesap numarasını otomatik üretir.
     */
    public SavingsAccount openSavingsAccount(String customerId,
                                             double initialDeposit,
                                             double annualInterestRate) {
        Customer customer = getCustomerOrThrow(customerId);
        String accNumber = generateAccountNumber("SAV");
        SavingsAccount account = new SavingsAccount(accNumber, customerId,
                                                    initialDeposit, annualInterestRate);
        accounts.put(accNumber, account);
        customer.addAccount(account);
        System.out.printf("✓ Tasarruf hesabı açıldı: %s (Başlangıç: %.2f TL)%n",
                          accNumber, initialDeposit);
        return account;
    }

    /**
     * Müşteri için yeni bir vadesiz hesap açar.
     */
    public CheckingAccount openCheckingAccount(String customerId,
                                               double initialDeposit,
                                               double overdraftLimit) {
        Customer customer = getCustomerOrThrow(customerId);
        String accNumber = generateAccountNumber("CHK");
        CheckingAccount account = new CheckingAccount(accNumber, customerId,
                                                      initialDeposit, overdraftLimit);
        accounts.put(accNumber, account);
        customer.addAccount(account);
        System.out.printf("✓ Vadesiz hesap açıldı: %s (Başlangıç: %.2f TL)%n",
                          accNumber, initialDeposit);
        return account;
    }

    /**
     * Hesabı kapatır ve sahibinin listesinden çıkarır.
     * Kapatılacak hesabın bakiyesi sıfır olmalıdır.
     */
    public void closeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getBalance() != 0) {
            throw new IllegalStateException(
                String.format("Hesap kapatılamaz; bakiye sıfır değil: %.2f TL", account.getBalance())
            );
        }
        Customer owner = customers.get(account.getOwnerCustomerId());
        if (owner != null) {
            owner.removeAccount(account);
        }
        accounts.remove(accountNumber);
        System.out.println("✓ Hesap kapatıldı: " + accountNumber);
    }

    /**
     * Belirtilen hesaba para yatırır.
     */
    public void deposit(String accountNumber, double amount) {
        getAccountOrThrow(accountNumber).deposit(amount);
        System.out.printf("✓ Para yatırıldı: %.2f TL → %s%n", amount, accountNumber);
    }

    /**
     * Belirtilen hesaptan para çeker.
     */
    public void withdraw(String accountNumber, double amount) {
        getAccountOrThrow(accountNumber).withdraw(amount);
        System.out.printf("✓ Para çekildi: %.2f TL ← %s%n", amount, accountNumber);
    }

    /**
     * Bir hesaptan diğerine para aktarır.
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        Account from = getAccountOrThrow(fromAccountNumber);
        Account to   = getAccountOrThrow(toAccountNumber);
        from.transferTo(to, amount);
        System.out.printf("✓ Transfer: %.2f TL | %s → %s%n",
                          amount, fromAccountNumber, toAccountNumber);
    }

    /**
     * Sistemdeki tüm tasarruf hesaplarına aylık faiz uygular.
     */
    public void applyMonthlyInterestToAll() {
        accounts.values().stream()
            .filter(a -> a instanceof SavingsAccount)
            .map(a -> (SavingsAccount) a)
            .forEach(sa -> {
                sa.applyMonthlyInterest();
                System.out.printf("  Faiz uygulandı → %s%n", sa.getAccountNumber());
            });
        System.out.println("✓ Aylık faizler uygulandı.");
    }

    // =========================================================
    //  RAPORLAMA
    // =========================================================

    /**
     * Bankadaki tüm hesapları özetleyen raporu yazdırır.
     */
    public void printBankReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + bankName + " – Hesap Raporu");
        System.out.println("=".repeat(60));

        double totalAssets = 0;
        for (Account acc : accounts.values()) {
            System.out.println("  " + acc);
            totalAssets += acc.getBalance();
        }

        System.out.println("-".repeat(60));
        System.out.printf("  Toplam Müşteri: %d | Toplam Hesap: %d | Toplam Varlık: %.2f TL%n",
                          customers.size(), accounts.size(), totalAssets);
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Belirtilen müşteriye ait tüm hesapların ekstresini yazdırır.
     */
    public void printCustomerStatement(String customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        System.out.println("\n>>> Müşteri Ekstresi: " + customer.getFullName());
        if (customer.getAccounts().isEmpty()) {
            System.out.println("  Hesap bulunamadı.");
            return;
        }
        for (Account acc : customer.getAccounts()) {
            System.out.println();
            acc.printStatement();
        }
        System.out.printf("%nToplam Bakiye: %.2f TL%n", customer.getTotalBalance());
    }

    // =========================================================
    //  YARDIMCI METODLAR
    // =========================================================

    /** Müşteriyi getirir; yoksa istisna fırlatır. */
    private Customer getCustomerOrThrow(String customerId) {
        return findCustomer(customerId)
            .orElseThrow(() -> new AccountNotFoundException(
                "Müşteri bulunamadı: " + customerId, true));
    }

    /** Hesabı getirir; yoksa istisna fırlatır. */
    private Account getAccountOrThrow(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /**
     * Benzersiz ve anlamlı bir hesap numarası üretir.
     * Örnek: SAV-1001, CHK-1002
     */
    private String generateAccountNumber(String prefix) {
        return prefix + "-" + accountCounter.getAndIncrement();
    }

    // --- Getter ---

    public String getBankName() { return bankName; }

    /**
     * Hesap numarasına göre hesabı getirir (dışarıdan erişim için).
     */
    public Optional<Account> findAccount(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    /**
     * Müşteriye ait hesapları döner.
     */
    public List<Account> getAccountsByCustomer(String customerId) {
        return accounts.values().stream()
            .filter(a -> a.getOwnerCustomerId().equals(customerId))
            .collect(Collectors.toList());
    }
}
