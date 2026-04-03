import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bir hesap üzerinde gerçekleşen tek bir işlemi temsil eder.
 * İşlem oluşturulduktan sonra değiştirilemez (immutable tasarım).
 */
public class Transaction {

    private static int idCounter = 1; // Her işleme benzersiz bir ID vermek için

    private final int id;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter; // İşlem sonrası hesap bakiyesi
    private final LocalDateTime date;
    private final String description;

    public Transaction(TransactionType type, double amount, double balanceAfter, String description) {
        this.id = idCounter++;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.date = LocalDateTime.now();
        this.description = description;
    }

    // --- Getter'lar ---

    public int getId() { return id; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getDate() { return date; }
    public String getDescription() { return description; }

    /**
     * İşlemi ekrana bastırmak için okunabilir bir format döner.
     */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return String.format(
            "[#%d] %s | %s | Tutar: %.2f TL | Sonraki Bakiye: %.2f TL | %s",
            id,
            date.format(formatter),
            type.getDescription(),
            amount,
            balanceAfter,
            description
        );
    }
}
