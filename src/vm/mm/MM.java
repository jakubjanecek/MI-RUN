package vm.mm;

import vm.Util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO
// operace pro stack, viz. kody od Clause
// pak to bude tak, ze vezme se prvni instrukce a zacne se zpracovavat, stack funguje pouze jako odkladiste promennych (objektu), atd. => zadny kod tam neni!!!

public class MM {

    public static final int WORD_SIZE = 4;

    public static final int REF_SIZE = WORD_SIZE;

    // MARKER + OBJ_KIND + SIZE + CLASS
    public static final int HEADER_SIZE = 1 + 1 + WORD_SIZE + REF_SIZE;

    private static final byte MARKER = (byte) 0xF0;

    public static final byte FREE_MARKER = (byte) 0x0;

    public final Pointer NULL = new Pointer(0xFFFFFFFF, this);

    public byte[] code;
    private int firstFreeCodeByte = 0;

    public byte[] heap;
    private int firstFreeHeapByte = 0;

    public byte[] stack;
    private int firstFreeStackByte = 0;

    private List<Method> methods;

    public MM(int codeSize, int heapSize, int stackSize) {
        code = new byte[codeSize];
        heap = new byte[heapSize];
        stack = new byte[stackSize];
        methods = new ArrayList<Method>();
    }

    public Pointer alloc(int size) {
        Pointer p = null;
        if (firstFreeHeapByte + size <= heap.length) {
            p = new Pointer(firstFreeHeapByte, this);
            clear(heap, firstFreeHeapByte, size);
            firstFreeHeapByte += size;
        } else {
            // garbage collection
        }

        return p;
    }

    public void free(Pointer obj) {
        int objectSize = 0;
        switch (obj.$().kind()) {
            case POINTER_INDEXED:
                objectSize = pointerIndexedObjectSize(obj.$().size());
            case BYTE_INDEXED:
                objectSize = byteIndexedObjectSize(obj.$().size());
        }
        clear(heap, obj.address, objectSize);
    }

    // TODO
    public void newFrame() {

    }

    public void push(Pointer object) {
        if (firstFreeStackByte + WORD_SIZE <= stack.length) {
            storePointer(stack, firstFreeStackByte, object);
            firstFreeStackByte += WORD_SIZE;
        } else {
            throw new RuntimeException("Stack overflow!");
        }
    }

    public Pointer pop() {
        if (firstFreeStackByte - WORD_SIZE >= 0) {
            firstFreeStackByte -= WORD_SIZE;
            Pointer p = retrievePointer(stack, firstFreeStackByte);
            clear(stack, firstFreeStackByte, WORD_SIZE);
            return p;
        } else {
            throw new RuntimeException("Nothing to pop from stack!");
        }
    }

    public CodePointer storeMethod(byte[] bytecode) {
        CodePointer p = null;
        if (firstFreeCodeByte + bytecode.length <= code.length) {
            p = new CodePointer(firstFreeCodeByte, this);
            System.arraycopy(bytecode, 0, code, firstFreeCodeByte, bytecode.length);
            firstFreeCodeByte += bytecode.length;
        } else {
            throw new RuntimeException("Not enough memory for code!");
        }

        return p;
    }

    public int addMethod(Method m) {
        methods.add(m);
        return methods.indexOf(m);
    }

    public Method method(int index) {
        return methods.get(index);
    }

    public int pointerIndexedObjectSize(int size) {
        return HEADER_SIZE + (size * REF_SIZE);
    }

    public int byteIndexedObjectSize(int size) {
        return HEADER_SIZE + size;
    }

    private void clear(byte[] what, int from, int size) {
        for (int i = from; i < size; i++) {
            what[i] = FREE_MARKER;
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
            return retrieveInt(heap, pointer.address + SIZE_OFFSET);
        }

        public void size(int size) {
            storeInt(heap, pointer.address + SIZE_OFFSET, size);
        }

        public Pointer clazz() {
            return retrievePointer(heap, pointer.address + CLASS_OFFSET);
        }

        public void clazz(Pointer p) {
            storePointer(heap, pointer.address + CLASS_OFFSET, p);
        }
    }

    public class PointerIndexedObj extends Obj {

        public PointerIndexedObj(Pointer startAddress) {
            super(startAddress);
        }

        public Pointer field(int index) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            return retrievePointer(heap, address);
        }

        public void field(int index, Pointer obj) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            storePointer(heap, address, obj);
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

    public void storePointer(byte[] where, int address, Pointer p) {
        if (p != null) {
            storeInt(where, address, p.address);
        }
    }

    public Pointer retrievePointer(byte[] from, int address) {
        return new Pointer(retrieveInt(from, address), this);
    }

    public void storeInt(byte[] where, int address, int value) {
        byte[] bytes = Util.int2bytes(value);
        System.arraycopy(bytes, 0, where, address, 4);
    }

    public int retrieveInt(byte[] from, int address) {
        return Util.bytes2int(Arrays.copyOfRange(from, address, address + 4));
    }

    public void dump(PrintWriter out) {
        int numOfBytesOnRow = 8;

        out.println();
        out.println("# MEMORY DUMP");
        out.println("HEAP");
        dumpByteArray(heap, numOfBytesOnRow, out);

        out.println();
        out.println();
        out.println("STACK");
        dumpByteArray(stack, numOfBytesOnRow, out);

        out.println();
        out.println();
        out.println("CODE");
        dumpByteArray(code, numOfBytesOnRow, out);

        out.println();
    }

    private void dumpByteArray(byte[] arr, int numOfBytesOnRow, PrintWriter out) {
        int emptyCount = 0;

        for (int i = 0; i < arr.length; i += numOfBytesOnRow) {
            out.print(String.format("%04d: ", i));
            for (int j = i; j < i + numOfBytesOnRow; j++) {
                if (j < arr.length) {
                    out.print(String.format("%02X ", new Byte(arr[j])));

                    if (arr[j] == FREE_MARKER) {
                        emptyCount++;
                    } else {
                        emptyCount = 0;
                    }
                }
            }

            out.print("          ");

            for (int j = i; j < i + numOfBytesOnRow; j++) {
                if (j < arr.length) {
                    out.print(String.format("%4d   ", new Byte(arr[j])));
                }
            }

            if (emptyCount > 20) {
                break;
            }

            out.println();
        }
    }

}
