// Skor tablosundaki her giriş için oyuncu adını ve puanı bir arada tutan basit veri nesnesi.
// Queue<HighScoreEntry> yapısında saklanır.
public class HighScoreEntry {

    String name;  // Oyuncu adı
    int    score; // Oyuncu puanı

    public HighScoreEntry(String name, int score) {
        this.name  = name;
        this.score = score;
    }
}
