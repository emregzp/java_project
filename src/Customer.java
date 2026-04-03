import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bankadaki bir müşteriyi temsil eder.
 * Her müşterinin kendine ait birden fazla hesabı olabilir.
 */
public class Customer {

    private final String customerId;   // Kimlik numarası gibi benzersiz bir ID
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Bu müşteriye ait hesapların listesi
    private final List<Account> accounts;

    public Customer(String customerId, String firstName, String lastName,
                    String email, String phone) {
        this.customerId = customerId;
        this.firstName  = firstName;
        this.lastName   = lastName;
        this.email      = email;
        this.phone      = phone;
        this.accounts   = new ArrayList<>();
    }

    // --- Hesap yönetimi ---

    /**
     * Müşteriye yeni bir hesap bağlar.
     * Aynı hesap iki kez eklenmez.
     */
    public void addAccount(Account account) {
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    /**
     * Müşteriden bir hesabı kaldırır.
     */
    public boolean removeAccount(Account account) {
        return accounts.remove(account);
    }

    /**
     * Müşterinin tüm hesaplarındaki toplam bakiyeyi döner.
     */
    public double getTotalBalance() {
        return accounts.stream().mapToDouble(Account::getBalance).sum();
    }

    // --- Getter / Setter ---

    public String getCustomerId() { return customerId; }
    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getFullName()   { return firstName + " " + lastName; }
    public String getEmail()      { return email; }
    public String getPhone()      { return phone; }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts); // Dışarıdan doğrudan değiştirmeyi önler
    }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName)   { this.lastName  = lastName; }
    public void setEmail(String email)         { this.email     = email; }
    public void setPhone(String phone)         { this.phone     = phone; }

    /**
     * Müşteri bilgilerini özetleyen kısa bir metin.
     */
    @Override
    public String toString() {
        return String.format("Müşteri [%s] %s | E-posta: %s | Tel: %s | Hesap Sayısı: %d",
            customerId, getFullName(), email, phone, accounts.size());
    }
}
