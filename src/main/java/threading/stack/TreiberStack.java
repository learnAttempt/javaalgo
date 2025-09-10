package threading.stack;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free stack (Treiber stack) using AtomicReference for the head pointer.
 * Note: vulnerable to ABA problem in extreme scenarios.
 */
public class TreiberStack<E> {
    private static final class Node<E> {
        final E value;
        Node<E> next;
        Node(E value, Node<E> next) { this.value = value; this.next = next; }
    }

    private final AtomicReference<Node<E>> head = new AtomicReference<>(null);

    /** Push value onto stack. */
    public void push(E value) {
        final Node<E> newNode = new Node<>(value, null);
        while (true) {
            Node<E> currentHead = head.get();
            newNode.next = currentHead;           // set next to current head
            if (head.compareAndSet(currentHead, newNode)) {
                return; // success
            }
            // else retry (CAS failed)
        }
    }

    /** Pop and return top value; returns null if empty. */
    public E pop() {
        while (true) {
            Node<E> currentHead = head.get();
            if (currentHead == null) return null; // empty
            Node<E> next = currentHead.next;
            if (head.compareAndSet(currentHead, next)) {
                return currentHead.value; // success
            }
            // else retry
        }
    }

    /** Return top value without removing; may be stale; returns null if empty. */
    public E peek() {
        Node<E> h = head.get();
        return (h == null) ? null : h.value;
    }

    public boolean isEmpty() {
        return head.get() == null;
    }
}
