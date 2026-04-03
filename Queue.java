/**
 * Generic circular Queue data structure implementation using an internal array.
 * Allowed methods only: enqueue, dequeue, peek, isFull, isEmpty, size.
 *
 * @param <T> the type of elements stored in this queue
 */
public class Queue<T> {

    private Object[] data;
    private int front;
    private int rear;
    private int count;
    private int capacity;

    /**
     * Constructs a Queue with the given maximum capacity.
     *
     * @param capacity maximum number of elements the queue can hold
     */
    public Queue(int capacity) {
        this.capacity = capacity;
        this.data = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.count = 0;
    }

    /**
     * Adds an item to the rear of the queue.
     *
     * @param item the item to enqueue
     * @throws RuntimeException if the queue is full
     */
    public void enqueue(T item) {
        if (isFull()) {
            throw new RuntimeException("Queue overflow: queue is full.");
        }
        rear = (rear + 1) % capacity;
        data[rear] = item;
        count++;
    }

    /**
     * Removes and returns the item at the front of the queue.
     *
     * @return the front item
     * @throws RuntimeException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new RuntimeException("Queue underflow: queue is empty.");
        }
        T item = (T) data[front];
        data[front] = null; // help GC
        front = (front + 1) % capacity;
        count--;
        return item;
    }

    /**
     * Returns (without removing) the item at the front of the queue.
     *
     * @return the front item
     * @throws RuntimeException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new RuntimeException("Queue is empty.");
        }
        return (T) data[front];
    }

    /**
     * Returns true if the queue has reached its maximum capacity.
     *
     * @return true if full
     */
    public boolean isFull() {
        return count == capacity;
    }

    /**
     * Returns true if the queue contains no elements.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns the number of elements currently in the queue.
     *
     * @return current size
     */
    public int size() {
        return count;
    }
}
