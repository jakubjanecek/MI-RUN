package vm.mm;

import java.io.PrintWriter;
import java.util.Arrays;

public class MM {

    private byte[] memory;

    // private Method[] methods;
    private int firstFree = 0;

    private static final byte MARKER = (byte) 0xF0;
    private static final int WORD_SIZE = 4;
    private static final int REF_SIZE = WORD_SIZE;
    // MARKER + OBJ_KIND + SIZE + CLASS
    private static final int HEADER_SIZE = 1 + 1 + WORD_SIZE + REF_SIZE;

    public MM(int memorySize) {
        memory = new byte[memorySize];
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = alloc(pointerIndexedObjectSize(size));

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().marker(MARKER);
        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        return newObject;
    }

    public Pointer newClazz(Pointer metaclass, Pointer superclass, byte[] name) {
        Pointer newClass = alloc(pointerIndexedObjectSize(3));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().marker(MARKER);
        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(3);
        newClass.$().clazz(metaclass);
        newClass.$c().name(newString(name));
        newClass.$c().superclazz(superclass);
        newClass.$c().methods(null);

        return newClass;
    }

    public Pointer newString(byte[] str) {
        Pointer newString = alloc(byteIndexedObjectSize(str.length));

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().marker(MARKER);
        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(null);
        newString.$b().bytes(str);

        return newString;
    }

    private Pointer alloc(int size) {
        Pointer p = null;
        if (firstFree + size <= memory.length) {
            p = new Pointer(firstFree, this);
            clear(firstFree, size);
            firstFree += size;
        } else {
            // garbage collection
        }

        return p;
    }

    private void deleteObject(Pointer obj) {
        int objectSize = 0;
        switch (obj.$().kind()) {
            case POINTER_INDEXED:
                objectSize = pointerIndexedObjectSize(obj.$().size());
            case BYTE_INDEXED:
                objectSize = byteIndexedObjectSize(obj.$().size());
        }
        clear(obj.address, objectSize);
    }

    private int pointerIndexedObjectSize(int size) {
        return HEADER_SIZE + (size * REF_SIZE);
    }

    private int byteIndexedObjectSize(int size) {
        return HEADER_SIZE + size;
    }

    private void clear(int from, int size) {
        for (int i = from; i < size; i++) {
            memory[i] = 0x0;
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
        }

        public void marker(byte marker) {
            memory[pointer.address] = marker;
        }

        public ObjectKind kind() {
            return ObjectKind.fromValue(memory[pointer.address + KIND_OFFSET]);
        }

        public void kind(ObjectKind kind) {
            memory[pointer.address + KIND_OFFSET] = kind.value;
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
            return Arrays.copyOfRange(memory, pointer.address + DATA_OFFSET, pointer.address + DATA_OFFSET + size());
        }

        public void bytes(byte[] bytes) {
            System.arraycopy(bytes, 0, memory, pointer.address + DATA_OFFSET, size());
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

        public void superclazz(Pointer superclazz) {
            field(1, superclazz);
        }

        public Pointer superclazz() {
            return field(1);
        }

        public void methods(Pointer methods) {
            field(2, methods);
        }

        public Pointer methods() {
            return field(2);
        }
    }

    public static enum ObjectKind {

        POINTER_INDEXED((byte) 0x01), BYTE_INDEXED((byte) 0x02);
        public byte value;

        ObjectKind(byte value) {
            this.value = value;
        }

        public static ObjectKind fromValue(byte value) {
            for (ObjectKind kind : values()) {
                if (kind.value == value) {
                    return kind;
                }
            }

            return POINTER_INDEXED;
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
        byte[] bytes = int2bytes(value);
        System.arraycopy(bytes, 0, memory, address, 4);
    }

    private int retrieveInt(int address) {
        return bytes2int(Arrays.copyOfRange(memory, address, address + 4));
    }

    private byte[] int2bytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private int bytes2int(byte[] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }

    public void dump(PrintWriter out) {
        out.println("\n# MEMORY DUMP\naddr: hex   dec");
        int emptyCount = 0;
        for (int i = 0; i < memory.length; i++) {
            out.println(String.format("%04d: %02X    %d", i, new Byte(memory[i]), new Byte(memory[i])));

            if (memory[i] == 0x0) {
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
