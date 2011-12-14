package vm.mm;

import vm.Util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MM {

    public static final int WORD_SIZE = 4;

    public static final int REF_SIZE = WORD_SIZE;

    // MARKER + OBJ_KIND + SIZE + CLASS
    public static final int HEADER_SIZE = 1 + 1 + WORD_SIZE + REF_SIZE;

    private static final byte MARKER = (byte) 0xF0;

    private byte[] heap;

    private byte[] stack;

    private List<Method> methods;

    private int firstFree = 0;

    public MM(int heapSize, int stackSize) {
        heap = new byte[heapSize];
        stack = new byte[stackSize];
        methods = new ArrayList<Method>();
    }

    public Pointer alloc(int size) {
        Pointer p = null;
        if (firstFree + size <= heap.length) {
            p = new Pointer(firstFree, this);
            clear(firstFree, size);
            firstFree += size;
        } else {
            // garbage collection
        }

        return p;
    }

    public void deleteObject(Pointer obj) {
        int objectSize = 0;
        switch (obj.$().kind()) {
            case POINTER_INDEXED:
                objectSize = pointerIndexedObjectSize(obj.$().size());
            case BYTE_INDEXED:
                objectSize = byteIndexedObjectSize(obj.$().size());
        }
        clear(obj.address, objectSize);
    }

    public int pointerIndexedObjectSize(int size) {
        return HEADER_SIZE + (size * REF_SIZE);
    }

    public int byteIndexedObjectSize(int size) {
        return HEADER_SIZE + size;
    }

    private void clear(int from, int size) {
        for (int i = from; i < size; i++) {
            heap[i] = 0x0;
        }
    }

    public class Obj {

        protected Pointer pointer;
        protected static final int KIND_OFFSET = 1;
        protected static final int SIZE_OFFSET = KIND_OFFSET + 1;
        protected static final int CLASS_OFFSET = SIZE_OFFSET + WORD_SIZE;
        protected static final int DATA_OFFSET = CLASS_OFFSET + REF_SIZE;

        public Obj(Pointer startAddress) {
            this.pointer = startAddress;

            marker(MARKER);
        }

        public void marker(byte marker) {
            heap[pointer.address] = marker;
        }

        public ObjectKind kind() {
            return ObjectKind.fromValue(heap[pointer.address + KIND_OFFSET]);
        }

        public void kind(ObjectKind kind) {
            heap[pointer.address + KIND_OFFSET] = kind.value;
        }

        public int size() {
            return retrieveInt(pointer.address + SIZE_OFFSET);
        }

        public void size(int size) {
            storeInt(pointer.address + SIZE_OFFSET, size);
        }

        public Pointer clazz() {
            return retrievePointer(pointer.address + CLASS_OFFSET);
        }

        public void clazz(Pointer p) {
            storePointer(pointer.address + CLASS_OFFSET, p);
        }
    }

    public class PointerIndexedObj extends Obj {

        public PointerIndexedObj(Pointer startAddress) {
            super(startAddress);
        }

        public Pointer field(int index) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            return retrievePointer(address);
        }

        public void field(int index, Pointer obj) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            storePointer(address, obj);
        }
    }

    public class ByteIndexedObj extends Obj {

        public ByteIndexedObj(Pointer startAddress) {
            super(startAddress);
        }

        public byte[] bytes() {
            return Arrays.copyOfRange(heap, pointer.address + DATA_OFFSET, pointer.address + DATA_OFFSET + size());
        }

        public void bytes(byte[] bytes) {
            System.arraycopy(bytes, 0, heap, pointer.address + DATA_OFFSET, size());
        }
    }

    public class Clazz extends PointerIndexedObj {

        public Clazz(Pointer startAddress) {
            super(startAddress);
        }

        public void name(Pointer name) {
            field(0, name);
        }

        public Pointer name() {
            return field(0);
        }

        public void superclass(Pointer superclass) {
            field(1, superclass);
        }

        public Pointer superclass() {
            return field(1);
        }

        public void metaclass(Pointer metaclass) {
            field(2, metaclass);
        }

        public Pointer metaclass() {
            return field(2);
        }

        public void methods(Pointer methods) {
            field(3, methods);
        }

        public Pointer methods() {
            return field(3);
        }
    }

    private void storePointer(int address, Pointer p) {
        if (p != null) {
            storeInt(address, p.address);
        }
    }

    private Pointer retrievePointer(int address) {
        return new Pointer(retrieveInt(address), this);
    }

    private void storeInt(int address, int value) {
        byte[] bytes = Util.int2bytes(value);
        System.arraycopy(bytes, 0, heap, address, 4);
    }

    private int retrieveInt(int address) {
        return Util.bytes2int(Arrays.copyOfRange(heap, address, address + 4));
    }

    public void dump(PrintWriter out) {
        out.println("\n# MEMORY DUMP\naddr: hex   dec");
        int emptyCount = 0;
        for (int i = 0; i < heap.length; i++) {
            out.println(String.format("%04d: %02X    %d", i, new Byte(heap[i]), new Byte(heap[i])));

            if (heap[i] == 0x0) {
                emptyCount++;
            } else {
                emptyCount = 0;
            }

            if (emptyCount > 20) {
                break;
            }
        }
        out.println();
    }

}
