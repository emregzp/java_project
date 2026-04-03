// Bağlı liste kullanan sabit kapasiteli yığın (dizi kullanılmıyor).
// Kural: Bu sınıfta YALNIZCa şu 6 metot bulunur:
//   push, pop, peek, isFull, isEmpty, size
public class Stack<T> {

    private Node<T> top;         // Yığının tepesi
    private int size;            // Anlık eleman sayısı
    private final int capacity;  // Maksimum kapasite

    public Stack(int capacity) {
        this.capacity = capacity;
        this.top      = null;
        this.size     = 0;
    }

    // Yığının tepesine yeni eleman ekler
    public void push(T item) {
        if (isFull()) throw new RuntimeException("Stack is full");
        Node<T> newNode = new Node<>(item);
        newNode.next = top;
        top  = newNode;
        size++;
    }

    // Tepedeki elemanı yığından çıkarır ve döner
    public T pop() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        T data = top.data;
        top    = top.next;
        size--;
        return data;
    }

    // Tepedeki elemanı çıkarmadan gösterir
    public T peek() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        return top.data;
    }

    // Yığın maksimum kapasiteye ulaştıysa true döner
    public boolean isFull() {
        return size == capacity;
    }

    // Yığında hiç eleman yoksa true döner
    public boolean isEmpty() {
        return size == 0;
    }

    // Anlık eleman sayısını döner
    public int size() {
        return size;
    }
}
