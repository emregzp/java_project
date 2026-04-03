/**
 * Hesap üzerinde yapılabilecek işlem türlerini tanımlar.
 * Enum kullanmak, tip güvenliğini artırır ve hata ihtimalini azaltır.
 */
public enum TransactionType {
    DEPOSIT("Para Yatırma"),
    WITHDRAWAL("Para Çekme"),
    TRANSFER_IN("Transfer Alındı"),
    TRANSFER_OUT("Transfer Gönderildi"),
    INTEREST("Faiz Ödemesi");

    // Her işlem türünün Türkçe açıklaması
    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
