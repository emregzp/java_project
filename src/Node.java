// Stack ve Queue sınıflarının iç yapısı için kullanılan bağlı liste düğümü.
// Hem veriyi hem de bir sonraki düğümün referansını tutar.
public class Node<T> {

    T data;        // Düğümün taşıdığı veri
    Node<T> next;  // Bir sonraki düğüme işaret eden referans

    public Node(T data) {
        this.data = data;
        this.next = null;
    }
}
