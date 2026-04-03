import java.util.Random;
import java.util.Scanner;
import java.io.*;

/**
 * Tile Matching Game — Stack ve Queue veri yapıları kullanılarak yazılmış harf eşleştirme oyunu.
 *
 * Eclipse'de çalıştırmak için:
 *   TileGame.java dosyasına sağ tıkla → Run As → Java Application
 *
 * Not: HighScoreTable.txt dosyası projenin kök klasöründe (src/ üstünde) okunup yazılır.
 *
 * Kurallar:
 *  - Yalnızca Stack ve Queue veri yapıları kullanılır (dizi, ArrayList, vs. YASAK).
 *  - Stack sınıfı: sadece push/pop/peek/isFull/isEmpty/size metotları.
 *  - Tüm oyun metotları bu (main'in bulunduğu) sınıfta yer alır.
 */
public class TileGame {

    // =========================================================================
    // SABİTLER
    // =========================================================================

    static final int    NUM_SETS         = 5;
    static final int    MAX_CAPACITY     = 10;   // Her setin maksimum kart sayısı
    static final int    MATCH_POINTS     = 5;    // Başarılı eşleşme puanı
    static final int    ADDSET_PENALTY   = -2;   // Kaydırma hakkı yokken AddSet cezası
    static final int    HIGH_SCORE_SIZE  = 10;   // Skor tablosundaki maksimum oyuncu sayısı
    static final String HIGH_SCORE_FILE  = "HighScoreTable.txt";
    static final String SEPARATOR        = "----------------------------------------------------------------------";

    // =========================================================================
    // OYUN DURUMU — Yalnızca Stack ve Queue kullanılır
    // =========================================================================

    // 5 kart seti
    static Stack<Character> set1;
    static Stack<Character> set2;
    static Stack<Character> set3;
    static Stack<Character> set4;
    static Stack<Character> set5;

    // Rezerv kuyruk (karıştırılmış 26 harf)
    static Queue<Character> reserveQueue;
    // Tamamlayıcı kuyruk (karıştırılmış 26 harf)
    static Queue<Character> supplementaryQueue;

    // Skor tablosu — en fazla HIGH_SCORE_SIZE oyuncu (Queue ile tutulur)
    static Queue<HighScoreEntry> highScores;

    // Oyun istatistikleri
    static int    score;
    static int    remainingShifts;  // Kalan kaydırma hakkı
    static int    currentStep;      // Tamamlanan adım sayısı
    static int    maxSteps;         // Adım limiti: başlangıç kartı × 1.2
    static String playerName;

    static Random rand = new Random();

    // =========================================================================
    // ANA METOT
    // =========================================================================

    public static void main(String[] args) {
        Scanner scanner  = new Scanner(System.in);
        boolean playAgain = true;

        while (playAgain) {
            // Her yeni oyun için durumu sıfırla
            resetGameState();
            loadHighScores();

            // Oyuncu adını al
            System.out.print("Enter player name: ");
            playerName = scanner.nextLine().trim();

            // Oyunu hazırla ve başlat
            initGame();
            System.out.println("THE GAME STARTS NOW!...");

            // Başlangıç durumunu göster (adım 0)
            displayGameState();

            // ---- Ana oyun döngüsü ----
            boolean gameOver = false;
            while (!gameOver) {
                System.out.print(">> ");
                String input = scanner.nextLine().trim();

                // Oyuncu 'F' ile oyunu bitiriyor
                if (input.equals("F")) {
                    break;
                }

                boolean validCommand = false;

                if (input.startsWith("Match(") && input.endsWith(")")) {
                    // Match(i,j) — String.split yerine indexOf ile ayrıştırılır
                    String inner    = input.substring(6, input.length() - 1);
                    int    commaIdx = inner.indexOf(',');
                    if (commaIdx > 0) {
                        try {
                            int i = Integer.parseInt(inner.substring(0, commaIdx).trim());
                            int j = Integer.parseInt(inner.substring(commaIdx + 1).trim());
                            validCommand = handleMatch(i, j);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid command!");
                        }
                    } else {
                        System.out.println("Invalid command!");
                    }

                } else if (input.startsWith("AddSet(") && input.endsWith(")")) {
                    // AddSet(i)
                    String inner = input.substring(7, input.length() - 1).trim();
                    try {
                        int i = Integer.parseInt(inner);
                        validCommand = handleAddSet(i);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid command!");
                    }

                } else if (input.equals("ShiftQueue")) {
                    validCommand = handleShiftQueue();

                } else {
                    System.out.println("Invalid command!");
                }

                // Geçerli komut: adım sayacını artır, otomatik kart ekle, durumu göster
                if (validCommand) {
                    currentStep++;
                    if (currentStep % 3 == 0) {
                        autoAddTile();
                    }
                    displayGameState();
                    // Bitiş kontrolü: adım limitine ulaşıldı veya tüm setler boşaldı
                    if (currentStep >= maxSteps || allSetsEmpty()) {
                        gameOver = true;
                    }
                }
            }

            // ---- Oyun bitti ----
            System.out.println("END OF THE GAME!...");
            updateHighScores(playerName, score);
            displayHighScoreTable();
            saveHighScores();

            System.out.println("Play again?");
            String answer = scanner.nextLine().trim();
            playAgain = answer.equalsIgnoreCase("Y");
        }

        scanner.close();
    }

    // =========================================================================
    // BAŞLATMA
    // =========================================================================

    // Tüm oyun değişkenlerini sıfırlar — yeni oyun başlamadan önce çağrılır
    static void resetGameState() {
        set1               = new Stack<>(MAX_CAPACITY);
        set2               = new Stack<>(MAX_CAPACITY);
        set3               = new Stack<>(MAX_CAPACITY);
        set4               = new Stack<>(MAX_CAPACITY);
        set5               = new Stack<>(MAX_CAPACITY);
        reserveQueue       = new Queue<>(30);
        supplementaryQueue = new Queue<>(30);
        score              = 0;
        currentStep        = 0;
        remainingShifts    = 0;
        maxSteps           = 0;
    }

    // Oyunu kurar: setleri rastgele harflerle doldurur, kuyrukları hazırlar
    static void initGame() {
        // Her set için 1-10 arası rastgele benzersiz harf
        for (int i = 1; i <= NUM_SETS; i++) {
            int size = rand.nextInt(MAX_CAPACITY) + 1;
            Queue<Character> shuffled = createShuffledAlphabetQueue();
            Stack<Character> set      = getSet(i);
            for (int j = 0; j < size; j++) {
                set.push(shuffled.dequeue());
            }
        }

        // Rezerv ve tamamlayıcı kuyruklar (her biri ayrı karıştırılmış 26 harf)
        reserveQueue       = createShuffledAlphabetQueue();
        supplementaryQueue = createShuffledAlphabetQueue();

        // Rastgele kaydırma hakkı (1-5)
        remainingShifts = rand.nextInt(5) + 1;

        // Adım limiti: toplam başlangıç kartı × 1.2 (tam sayı kısmı alınır)
        int total = set1.size() + set2.size() + set3.size() + set4.size() + set5.size();
        maxSteps = (int)(total * 1.2);
    }

    // İngiliz alfabesini (A-Z) rastgele sıraya dizerek 26 elemanlı bir kuyruk döner.
    // Fisher-Yates benzeri yaklaşım — Queue döndürme ile dizi kullanmadan karıştırma
    static Queue<Character> createShuffledAlphabetQueue() {
        // Kaynak kuyruğa A-Z sırasıyla ekle
        Queue<Character> source = new Queue<>(26);
        for (char c = 'A'; c <= 'Z'; c++) {
            source.enqueue(c);
        }

        // Her adımda kalan elemanlar arasından rastgele birini seç
        Queue<Character> result    = new Queue<>(26);
        int              remaining = 26;
        while (!source.isEmpty()) {
            int r = rand.nextInt(remaining);
            // Seçmek istediğimiz elemanı öne getirmek için r kez döndür
            for (int i = 0; i < r; i++) {
                source.enqueue(source.dequeue());
            }
            result.enqueue(source.dequeue());
            remaining--;
        }
        return result;
    }

    // =========================================================================
    // OYUN KOMUTLARI
    // =========================================================================

    // Match(i,j): Set_i ve Set_j'nin tepelerini karşılaştırır.
    // Eşleşirse her iki kart da kaldırılır ve +5 puan verilir.
    // Eşleşmezse uyarı verilir, kartlar yerinde kalır; komut yine de geçerli sayılır.
    static boolean handleMatch(int i, int j) {
        if (i < 1 || i > NUM_SETS || j < 1 || j > NUM_SETS) {
            System.out.println("Invalid set index! Use 1-5.");
            return false;
        }
        if (i == j) {
            System.out.println("Cannot match a set with itself!");
            return false;
        }
        Stack<Character> setI = getSet(i);
        Stack<Character> setJ = getSet(j);
        if (setI.isEmpty() || setJ.isEmpty()) {
            System.out.println("One of the sets is empty. Cannot match.");
            return false;
        }

        char tileI = setI.peek();
        char tileJ = setJ.peek();

        if (tileI == tileJ) {
            // Eşleşme başarılı — her iki kartı da kaldır
            setI.pop();
            setJ.pop();
            score += MATCH_POINTS;
            System.out.println("Match successful! +5 points.");
        } else {
            // Kartlar eşleşmedi — uyarı ver, kart kaldırma
            System.out.println("No match! Set" + i + " has '" + tileI
                             + "', Set" + j + " has '" + tileJ + "'. Try again.");
        }
        return true; // Sözdizimsel olarak geçerli komut → adım sayılır
    }

    // AddSet(i): Rezerv kuyruğunun önündeki kartı Set_i'nin tepesine ekler.
    // Kaydırma hakkı yoksa -2 ceza uygulanır.
    // Set doluysa kart rezerv kuyruğunun arkasına geri döner.
    static boolean handleAddSet(int i) {
        if (i < 1 || i > NUM_SETS) {
            System.out.println("Invalid set index! Use 1-5.");
            return false;
        }
        if (reserveQueue.isEmpty()) {
            System.out.println("Reserve queue is empty!");
            return false;
        }

        // Kaydırma hakkı yoksa cezayı hemen uygula
        if (remainingShifts == 0) {
            score += ADDSET_PENALTY;
            System.out.println("No shift rights left. AddSet penalty: -2");
            System.out.println("points.");
        }

        Stack<Character> set = getSet(i);

        // Set doluysa kartı rezerv kuyruğunun arkasına geri koy
        if (set.isFull()) {
            System.out.println("Set" + i + " is full! Tile returned to Reserve Queue.");
            char letter = reserveQueue.dequeue();
            reserveQueue.enqueue(letter);
            return true;
        }

        // Normal ekleme: rezerv kuyruğunun önündeki kartı setin tepesine koy
        char letter = reserveQueue.dequeue();
        set.push(letter);
        return true;
    }

    // ShiftQueue: Rezerv kuyruğunun önündeki kartı kuyruğun arkasına taşır.
    // Kalan kaydırma hakkı yoksa işlem yapılmaz ve adım sayılmaz.
    static boolean handleShiftQueue() {
        if (remainingShifts <= 0) {
            System.out.println("No shift rights remaining!");
            return false;
        }
        char front = reserveQueue.dequeue();
        reserveQueue.enqueue(front);
        remainingShifts--;
        System.out.println("Reserve queue shifted.");
        return true;
    }

    // Her 3 geçerli adımda bir tamamlayıcı kuyruğun önündeki kartı
    // en az kart içeren sete (eşitlikte en düşük indeksli) ekler.
    // Set doluysa kart kuyruğun arkasına geri döner.
    static void autoAddTile() {
        if (supplementaryQueue.isEmpty()) return;

        int              minIdx = findMinSizeSetIndex();
        Stack<Character> minSet = getSet(minIdx);
        char             tile   = supplementaryQueue.dequeue();

        if (minSet.isFull()) {
            // Set dolu — kartı tamamlayıcı kuyruğun arkasına geri koy
            supplementaryQueue.enqueue(tile);
        } else {
            minSet.push(tile);
            System.out.println("[Auto] Tile added to Set" + minIdx + " from");
            System.out.println("Supplementary Queue.");
        }
    }

    // =========================================================================
    // GÖRÜNTÜLEME
    // =========================================================================

    // Tam oyun durumunu (ayraç, tüm setler, kuyruklar, skor) ekrana yazar
    static void displayGameState() {
        System.out.println(SEPARATOR);
        displaySet1WithQueues();
        System.out.println(SEPARATOR);
        displaySet2WithScore();
        displaySet(3, set3);
        displaySet(4, set4);
        displaySet(5, set5);
    }

    // Set1'i rezerv ve tamamlayıcı kuyruk bilgileriyle sağ sütunda gösterir.
    //
    // Sütun düzeni (sol sütun 24 karakter genişliğinde, %-24s ile hizalanır):
    //   Satır 0        : "Set1: Top -> X"      | "Reserve Queue:"
    //   Satır 1        : "      Y"             | "Front -> ... <- Rear"
    //   Satır n-2      : "      P"             | "Supplementary Queue:"  (n >= 4 ise)
    //   Satır n-1(son) : "      V <- Bottom"   | "Front -> ... <- Rear"  (n >= 4 ise)
    static void displaySet1WithQueues() {
        int size1 = set1.size();

        // Set1 boşsa kuyrukları alt satırlarda göster
        if (size1 == 0) {
            printLeftColumn("Set1: (Empty)");
            System.out.println("Reserve Queue:");
            printLeftColumn("");
            System.out.print("Front -> ");
            printQueueInline(reserveQueue);
            System.out.println("<- Rear");
            printLeftColumn("");
            System.out.println("Supplementary Queue:");
            printLeftColumn("");
            System.out.print("Front -> ");
            printQueueInline(supplementaryQueue);
            System.out.println("<- Rear");
            return;
        }

        // 4 adımlı yığın döngüsü: Set1'i yukarıdan aşağıya dolaşır ve geri yükler
        //   Adım 1: set1   → tempA  (ters sıra)
        //   Adım 2: tempA  → tempB  (tekrar doğru sıra: tepe üstte)
        //   Adım 3: tempB  → yazdır → tempA  (ters sıra)
        //   Adım 4: tempA  → set1   (geri yükle)
        Stack<Character> tempA = new Stack<>(MAX_CAPACITY + 5);
        Stack<Character> tempB = new Stack<>(MAX_CAPACITY + 5);
        while (!set1.isEmpty()) tempA.push(set1.pop());
        while (!tempA.isEmpty()) tempB.push(tempA.pop());

        int row = 0;
        while (!tempB.isEmpty()) {
            char    tile     = tempB.pop();
            boolean isTop    = (row == 0);
            boolean isBottom = (row == size1 - 1);

            // Sol sütun içeriği (24 karaktere hizalanır)
            String leftPart;
            if (isTop && isBottom)  leftPart = "Set1: Top -> " + tile + " <- Bottom";
            else if (isTop)         leftPart = "Set1: Top -> " + tile;
            else if (isBottom)      leftPart = "      " + tile + " <- Bottom";
            else                    leftPart = "      " + tile;

            printLeftColumn(leftPart);

            // Sağ sütun: hangi satırda hangi kuyruk bilgisi gösterileceği
            if (row == 0) {
                System.out.print("Reserve Queue:");
            } else if (row == 1) {
                System.out.print("Front -> ");
                printQueueInline(reserveQueue);
                System.out.print("<- Rear");
            } else if (size1 >= 4 && row == size1 - 2) {
                // Tamamlayıcı kuyruk etiketi (yalnızca 4+ kartlı setlerde satır sığar)
                System.out.print("Supplementary Queue:");
            } else if (isBottom && size1 >= 4) {
                // Tamamlayıcı kuyruk içeriği son satırda
                System.out.print("Front -> ");
                printQueueInline(supplementaryQueue);
                System.out.print("<- Rear");
            }

            System.out.println();
            tempA.push(tile);
            row++;
        }

        // Set1'i eski haline geri yükle
        while (!tempA.isEmpty()) set1.push(tempA.pop());

        // Set1'de 4'ten az kart varsa tamamlayıcı kuyruk ana döngüde gösterilemedi;
        // ekstra satır olarak döngü sonrasına eklenir
        if (size1 < 4) {
            if (size1 == 1) {
                // Rezerv kuyruk içeriği de gösterilemedi (row 1 hiç oluşmadı)
                printLeftColumn("");
                System.out.print("Front -> ");
                printQueueInline(reserveQueue);
                System.out.println("<- Rear");
            }
            // Tamamlayıcı kuyruk: etiket + içerik
            printLeftColumn("");
            System.out.println("Supplementary Queue:");
            printLeftColumn("");
            System.out.print("Front -> ");
            printQueueInline(supplementaryQueue);
            System.out.println("<- Rear");
        }
    }

    // Set2'yi puan/adım bilgisiyle birlikte gösterir.
    // Skor bilgisi: 3. satırda (index 2) ya da kart sayısı 3'ten azsa son satırda.
    static void displaySet2WithScore() {
        int size2 = set2.size();

        if (size2 == 0) {
            printLeftColumn("Set2: (Empty)");
            System.out.printf("Score: %d | Remaining Shifts: %d | Step: %d/%d%n",
                    score, remainingShifts, currentStep, maxSteps);
            return;
        }

        // Skor satır indeksi: min(2, son satır)
        int scoreRow = Math.min(2, size2 - 1);

        Stack<Character> tempA = new Stack<>(MAX_CAPACITY + 5);
        Stack<Character> tempB = new Stack<>(MAX_CAPACITY + 5);
        while (!set2.isEmpty()) tempA.push(set2.pop());
        while (!tempA.isEmpty()) tempB.push(tempA.pop());

        int row = 0;
        while (!tempB.isEmpty()) {
            char    tile     = tempB.pop();
            boolean isTop    = (row == 0);
            boolean isBottom = (row == size2 - 1);

            String leftPart;
            if (isTop && isBottom)  leftPart = "Set2: Top -> " + tile + " <- Bottom";
            else if (isTop)         leftPart = "Set2: Top -> " + tile;
            else if (isBottom)      leftPart = "      " + tile + " <- Bottom";
            else                    leftPart = "      " + tile;

            printLeftColumn(leftPart);

            // Puan bilgisi belirlenen satırda gösterilir
            if (row == scoreRow) {
                System.out.printf("Score: %d | Remaining Shifts: %d | Step: %d/%d",
                        score, remainingShifts, currentStep, maxSteps);
            }

            System.out.println();
            tempA.push(tile);
            row++;
        }

        while (!tempA.isEmpty()) set2.push(tempA.pop());
    }

    // Set3, Set4 ve Set5 için standart yukarıdan aşağıya görüntüleme
    static void displaySet(int setNum, Stack<Character> set) {
        int size = set.size();

        if (size == 0) {
            System.out.println("Set" + setNum + ": (Empty)");
            return;
        }

        Stack<Character> tempA = new Stack<>(MAX_CAPACITY + 5);
        Stack<Character> tempB = new Stack<>(MAX_CAPACITY + 5);
        while (!set.isEmpty()) tempA.push(set.pop());
        while (!tempA.isEmpty()) tempB.push(tempA.pop());

        int row = 0;
        while (!tempB.isEmpty()) {
            char    tile     = tempB.pop();
            boolean isTop    = (row == 0);
            boolean isBottom = (row == size - 1);

            if (isTop && isBottom)
                System.out.println("Set" + setNum + ": Top -> " + tile + " <- Bottom");
            else if (isTop)
                System.out.println("Set" + setNum + ": Top -> " + tile);
            else if (isBottom)
                System.out.println("      " + tile + " <- Bottom");
            else
                System.out.println("      " + tile);

            tempA.push(tile);
            row++;
        }

        while (!tempA.isEmpty()) set.push(tempA.pop());
    }

    // Sol sütunu 24 karaktere hizalar; sol kısım tam 24 char ise en az 1 boşluk bırakır.
    // Bu sayede tek kartlı setlerde "Bottom" ile sağ sütun arasında boşluk kalmaması önlenir.
    static void printLeftColumn(String leftPart) {
        int width = Math.max(leftPart.length() + 1, 24);
        System.out.printf("%-" + width + "s", leftPart);
    }

    // Bir kuyruğun tüm içeriğini aynı satırda (Front'tan Rear'a) yazdırır.
    // Kuyruk içeriği değişmez: elemanlar geçici kuyruğa taşınıp geri iade edilir.
    static void printQueueInline(Queue<Character> q) {
        Queue<Character> temp = new Queue<>(30);
        while (!q.isEmpty()) {
            char c = q.dequeue();
            System.out.print(c + " ");
            temp.enqueue(c);
        }
        while (!temp.isEmpty()) {
            q.enqueue(temp.dequeue());
        }
    }

    // =========================================================================
    // SKOR TABLOSU
    // =========================================================================

    // HighScoreTable.txt dosyasından skor tablosunu okur.
    // Dosya yoksa boş tablo ile devam edilir.
    static void loadHighScores() {
        highScores = new Queue<>(HIGH_SCORE_SIZE + 1);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE));
            reader.readLine(); // "High Score Table" başlığını atla
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int lastSpace = line.lastIndexOf(' ');
                if (lastSpace > 0 && !highScores.isFull()) {
                    String name  = line.substring(0, lastSpace);
                    int    sc    = Integer.parseInt(line.substring(lastSpace + 1));
                    highScores.enqueue(new HighScoreEntry(name, sc));
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            // Dosya henüz mevcut değil — boş tablo ile başla
        } catch (IOException e) {
            System.out.println("Could not read high score file.");
        }
    }

    // Yeni oyuncuyu puana göre sıralı skor tablosuna ekler.
    // Aynı puana sahip oyuncuların üstüne yerleştirilir (yeni oyuncu daha üstte).
    // Tablo en fazla HIGH_SCORE_SIZE oyuncu tutar.
    static void updateHighScores(String name, int newScore) {
        // Geçici kuyrukta mevcut liste + yeni oyuncu birleştirilir
        Queue<HighScoreEntry> newQueue = new Queue<>(HIGH_SCORE_SIZE + 2);
        boolean inserted = false;

        while (!highScores.isEmpty()) {
            HighScoreEntry entry = highScores.dequeue();
            // Yeni oyuncu aynı veya daha yüksek puanlı ilk girişin önüne eklenir
            if (!inserted && newScore >= entry.score) {
                newQueue.enqueue(new HighScoreEntry(name, newScore));
                inserted = true;
            }
            newQueue.enqueue(entry);
        }
        if (!inserted) {
            newQueue.enqueue(new HighScoreEntry(name, newScore));
        }

        // En fazla HIGH_SCORE_SIZE kaydı tut
        highScores = new Queue<>(HIGH_SCORE_SIZE + 1);
        int count = 0;
        while (!newQueue.isEmpty() && count < HIGH_SCORE_SIZE) {
            highScores.enqueue(newQueue.dequeue());
            count++;
        }
    }

    // Skor tablosunu ekrana yazar, kuyruk yapısı korunur
    static void displayHighScoreTable() {
        System.out.println("High Score Table");
        Queue<HighScoreEntry> temp = new Queue<>(HIGH_SCORE_SIZE + 1);
        while (!highScores.isEmpty()) {
            HighScoreEntry entry = highScores.dequeue();
            System.out.println(entry.name + " " + entry.score);
            temp.enqueue(entry);
        }
        while (!temp.isEmpty()) {
            highScores.enqueue(temp.dequeue());
        }
    }

    // Güncel skor tablosunu HighScoreTable.txt'ye yazar
    static void saveHighScores() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(HIGH_SCORE_FILE));
            writer.println("High Score Table");
            Queue<HighScoreEntry> temp = new Queue<>(HIGH_SCORE_SIZE + 1);
            while (!highScores.isEmpty()) {
                HighScoreEntry entry = highScores.dequeue();
                writer.println(entry.name + " " + entry.score);
                temp.enqueue(entry);
            }
            while (!temp.isEmpty()) {
                highScores.enqueue(temp.dequeue());
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not save high score file.");
        }
    }

    // =========================================================================
    // YARDIMCI METOTLAR
    // =========================================================================

    // İndekse göre ilgili Stack'i döner — dizi yerine switch kullanılır
    static Stack<Character> getSet(int i) {
        switch (i) {
            case 1:  return set1;
            case 2:  return set2;
            case 3:  return set3;
            case 4:  return set4;
            case 5:  return set5;
            default: return null;
        }
    }

    // En az kart içeren setin indeksini döner.
    // Eşitlik durumunda en küçük indeks (en düşük numaralı set) kazanır.
    static int findMinSizeSetIndex() {
        int minSize  = Integer.MAX_VALUE;
        int minIndex = 1;
        for (int i = 1; i <= NUM_SETS; i++) {
            int sz = getSet(i).size();
            if (sz < minSize) {
                minSize  = sz;
                minIndex = i;
            }
        }
        return minIndex;
    }

    // Tüm setler boşsa true döner — oyun bitiş koşullarından biri
    static boolean allSetsEmpty() {
        for (int i = 1; i <= NUM_SETS; i++) {
            if (!getSet(i).isEmpty()) return false;
        }
        return true;
    }
}
