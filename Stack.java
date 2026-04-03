/**
 * Generic Stack data structure implementation using an internal array.
 * Allowed methods only: push, pop, peek, isFull, isEmpty, size.
 *
 * @param <T> the type of elements stored in this stack
 */
public class Stack<T> {

    private Object[] data;
    private int topIndex;
    private int capacity;

    /**
     * Constructs a Stack with the given maximum capacity.
     *
     * @param capacity maximum number of elements the stack can hold
     */
    public Stack(int capacity) {
        this.capacity = capacity;
        this.data = new Object[capacity];
        this.topIndex = -1;
    }

    /**
     * Pushes an item onto the top of the stack.
     *
     * @param item the item to push
     * @throws RuntimeException if the stack is full
     */
    public void push(T item) {
        if (isFull()) {
            throw new RuntimeException("Stack overflow: stack is full.");
        }
        data[++topIndex] = item;
    }

    /**
     * Removes and returns the item at the top of the stack.
     *
     * @return the top item
     * @throws RuntimeException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new RuntimeException("Stack underflow: stack is empty.");
        }
        T item = (T) data[topIndex];
        data[topIndex--] = null; // help GC
        return item;
    }

    /**
     * Returns (without removing) the item at the top of the stack.
     *
     * @return the top item
     * @throws RuntimeException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty.");
        }
        return (T) data[topIndex];
    }

    /**
     * Returns true if the stack has reached its maximum capacity.
     *
     * @return true if full
     */
    public boolean isFull() {
        return topIndex == capacity - 1;
    }

    /**
     * Returns true if the stack contains no elements.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return topIndex == -1;
    }

    /**
     * Returns the number of elements currently in the stack.
     *
     * @return current size
     */
    public int size() {
        return topIndex + 1;
    }
}
