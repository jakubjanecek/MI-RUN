package vm.mm;

import java.util.Arrays;

public class MemoryManager {

    private byte[] memory;
    // private Method[] methods;
    private int firstFree = 0;
    private static final byte MARKER = (byte) 0xF0;
    private static final int WORD_SIZE = 4;
    private static final int REF_SIZE = WORD_SIZE;
    // MARKER + OBJ_KIND + SIZE + CLASS
    private static final int HEADER_SIZE = 1 + 1 + WORD_SIZE + REF_SIZE;
    private static final int CLASS_HEADER_SIZE = 3 * REF_SIZE;

    public MemoryManager(int memorySize) {
        memory = new byte[memorySize];
    }

    public Obj newObject(Obj clazz, int size) {
        int objectSize = HEADER_SIZE + (size * REF_SIZE);

        Pointer p = alloc(objectSize);

        if (p == null) {
            throw new RuntimeException("Not enough memory!");
        }

        PointerIndexedObj obj = new PointerIndexedObj(p);

        obj.marker(MARKER);
        obj.kind(ObjectKind.POINTER_INDEXED);
        obj.size(size);
        obj.clazz(clazz.pointer);

        return obj;
    }

    public Obj newClazz(Obj metaclass, Obj superclass, byte[] name) {
        int objectSize = HEADER_SIZE + CLASS_HEADER_SIZE;

        Pointer p = alloc(objectSize);

        if (p == null) {
            throw new RuntimeException("Not enough memory!");
        }

        Clazz obj = new Clazz(p);
        obj.name(name);

        return obj;
    }

    public Obj newString(byte[] str) {
        int objectSize = HEADER_SIZE + str.length;

        Pointer p = alloc(objectSize);

        if (p == null) {
            throw new RuntimeException("Not enough memory!");
        }

        ByteIndexedObj obj = new ByteIndexedObj(p);

        obj.marker(MARKER);
        obj.kind(ObjectKind.BYTE_INDEXED);
        obj.size(str.length);
        obj.clazz(null);
        obj.bytes(str);

        return obj;
    }

    private Pointer alloc(int size) {
        Pointer p = null;
        if (firstFree + size <= memory.length) {
            p = new Pointer(firstFree);
            clear(firstFree, size);
            firstFree += size;
        } else {
            // garbage collection
        }

        return p;
    }

    private void deleteObject(Obj o) {
        clear(cast2Pointer(o).address, HEADER_SIZE + (o.size() * REF_SIZE));
    }

    private Obj cast2Obj(Pointer p) {
        return new Obj(p);
    }

    private Pointer cast2Pointer(Obj o) {
        return o.pointer;
    }

    private void clear(int from, int size) {
        for (int i = from; i < size; i++) {
            memory[i] = 0x0;
        }
    }

    public static class Pointer {

        public int address;

        public Pointer(int address) {
            this.address = address;
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

        public Clazz clazz() {
            return new Clazz(retrievePointer(pointer.address + CLASS_OFFSET));
        }

        public void clazz(Pointer p) {
            if (p != null) {
                storePointer(pointer.address + CLASS_OFFSET, p);
            } else {
                storePointer(pointer.address + CLASS_OFFSET, new Pointer((byte) -1));
            }
        }
    }

    public class PointerIndexedObj extends Obj {

        public PointerIndexedObj(Pointer startAddress) {
            super(startAddress);
        }

        public Obj field(int index) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            return cast2Obj(new Pointer(address));
        }

        public void field(int index, Obj value) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            storeInt(address, value.pointer.address);
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

    //    typedef struct class {
//    obj_header;
//    obj *name;
//    struct class *superclass;
//    obj    *methods;
//} class;
    public class Clazz extends PointerIndexedObj {

        private final int CLASS_INFO_OFFSET = DATA_OFFSET + (REF_SIZE * size());

        public Clazz(Pointer startAddress) {
            super(startAddress);
        }

        public void name(byte[] name) {
            storePointer(CLASS_INFO_OFFSET, newString(name).pointer);
        }

        public Obj name() {
            return new ByteIndexedObj(retrievePointer(CLASS_INFO_OFFSET));
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
        storeInt(address, p.address);
    }

    private Pointer retrievePointer(int address) {
        return new Pointer(retrieveInt(address));
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
}
