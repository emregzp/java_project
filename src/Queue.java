// Bağlı liste kullanan sabit kapasiteli kuyruk — FIFO prensibi (dizi kullanılmıyor).
// Oyunda rezerv kuyruk, tamamlayıcı kuyruk ve skor tablosu bu yapı ile yönetilir.
public class Queue<T> {

    private Node<T> front;       // Kuyruğun önü (çıkış noktası)
    private Node<T> rear;        // Kuyruğun arkası (giriş noktası)
    private int size;            // Anlık eleman sayısı
    private final int capacity;  // Maksimum kapasite

    public Queue(int capacity) {
        this.capacity = capacity;
        this.front    = null;
        this.rear     = null;
        this.size     = 0;
    }

    // Kuyruğun arkasına yeni eleman ekler
    public void enqueue(T item) {
        if (isFull()) throw new RuntimeException("Queue is full");
        Node<T> newNode = new Node<>(item);
        if (rear != null) rear.next = newNode;
        rear = newNode;
        if (front == null) front = newNode;
        size++;
    }

    // Kuyruğun önündeki elemanı çıkarır ve döner
    public T dequeue() {
        if (isEmpty()) throw new RuntimeException("Queue is empty");
        T data = front.data;
        front  = front.next;
        if (front == null) rear = null;
        size--;
        return data;
    }

    // Kuyruğun önündeki elemanı çıkarmadan gösterir
    public T peek() {
        if (isEmpty()) throw new RuntimeException("Queue is empty");
        return front.data;
    }

    // Kuyrukta hiç eleman yoksa true döner
    public boolean isEmpty() {
        return size == 0;
    }

    // Kuyruk maksimum kapasiteye ulaştıysa true döner
    public boolean isFull() {
        return size == capacity;
    }

    // Anlık eleman sayısını döner
    public int size() {
        return size;
    }
}
