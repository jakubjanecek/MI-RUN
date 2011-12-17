package vm.mm;

import vm.Bytecode;
import vm.Util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static vm.Util.bytes2int;

public class MM {

    public static final int INSTR_SIZE = 1;

    public static final int WORD_SIZE = 4;

    public static final int REF_SIZE = WORD_SIZE;

    // MARKER + OBJ_KIND + SIZE + CLASS
    public static final int HEADER_SIZE = 1 + 1 + WORD_SIZE + REF_SIZE;

    private static final byte MARKER = (byte) 0xF0;

    public static final byte FREE_MARKER = (byte) 0x0;

    public final Pointer NULL = new Pointer(0xFFFFFFFF, this);

    private byte[] code;
    private int firstFreeCodeByte = 0;

    private byte[] heap;
    private int firstFreeHeapByte = 0;

    private byte[] stack;
    private int stackPointer = 0;
    private int basePointer = NULL.address;

    private List<Method> methods;

    private List<Object> constantPool;

    private CodePointer programCounter = new CodePointer(0, this);

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

    public void newFrame(int numOfLocals) {
        int caller = basePointer;
        basePointer = stackPointer;
        pushInt(caller);
        pushInt(programCounter.address);
        for (int i = 0; i < numOfLocals; i++) {
            pushPointer(NULL);
        }
    }

    public CodePointer discardFrame() {
        int currentBasePointer = basePointer;
        int caller = bytes2int(Arrays.copyOfRange(stack, basePointer, basePointer + WORD_SIZE));
        int returnAddress = bytes2int(Arrays.copyOfRange(stack, basePointer + WORD_SIZE, basePointer + (2 * WORD_SIZE)));

        basePointer = caller;
        stackPointer = currentBasePointer;

        if (caller == NULL.address) {
            // end of the program
            return null;
        }

        return new CodePointer(returnAddress, this);
    }

    public void pushInt(int num) {
        if (this.stackPointer + WORD_SIZE <= stack.length) {
            storeInt(stack, stackPointer, num);
            this.stackPointer += WORD_SIZE;
        } else {
            throw new RuntimeException("Stack overflow!");
        }
    }

    public void pushPointer(Pointer object) {
        if (this.stackPointer + WORD_SIZE <= stack.length) {
            storePointer(stack, stackPointer, object);
            this.stackPointer += WORD_SIZE;
        } else {
            throw new RuntimeException("Stack overflow!");
        }
    }

    public int popInt() {
        if (this.stackPointer - WORD_SIZE >= 0) {
            this.stackPointer -= WORD_SIZE;
            int i = retrieveInt(stack, this.stackPointer);
            clear(stack, this.stackPointer, WORD_SIZE);
            return i;
        } else {
            throw new RuntimeException("Nothing to pop from stack!");
        }
    }

    public Pointer popPointer() {
        if (this.stackPointer - WORD_SIZE >= 0) {
            this.stackPointer -= WORD_SIZE;
            Pointer p = retrievePointer(stack, this.stackPointer);
            clear(stack, this.stackPointer, WORD_SIZE);
            return p;
        } else {
            throw new RuntimeException("Nothing to pop from stack!");
        }
    }

    public void arg(int index, Pointer val) {
        int address = basePointer - WORD_SIZE - (index * WORD_SIZE);
        storePointer(stack, address, val);
    }

    public Pointer arg(int index) {
        int address = basePointer - WORD_SIZE - (index * WORD_SIZE);
        Pointer p = retrievePointer(stack, address);
        return p;
    }

    public void local(int index, Pointer val) {
        // caller + return address + local at position
        int address = basePointer + WORD_SIZE + WORD_SIZE + (index * WORD_SIZE);
        storePointer(stack, address, val);
    }

    public Pointer local(int index) {
        // caller + return address + local at position 
        int address = basePointer + WORD_SIZE + WORD_SIZE + (index * WORD_SIZE);
        Pointer p = retrievePointer(stack, address);
        return p;
    }

    public CodePointer storeCode(byte[] bytecode) {
        CodePointer p;
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

    public void setPC(CodePointer pc) {
        programCounter = pc;
    }

    public byte getByteFromBC() {
        byte b = code[programCounter.address];
        pcInstr();
        return b;
    }

    public int getIntFromBC() {
        int i = retrieveInt(code, programCounter.address);
        pcWord();
        return i;
    }

    public Pointer getPointerFromBC() {
        Pointer p = retrievePointer(code, programCounter.address);
        pcWord();
        return p;
    }

    public int addConstant(Object constant) {
        constantPool.add(constant);
        return constantPool.indexOf(constant);
    }

    public Object constant(int index) {
        return constantPool.get(index);
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

    private void storePointer(byte[] where, int address, Pointer p) {
        if (p != null) {
            storeInt(where, address, p.address);
        }
    }

    private Pointer retrievePointer(byte[] from, int address) {
        return new Pointer(retrieveInt(from, address), this);
    }

    private void storeInt(byte[] where, int address, int value) {
        byte[] bytes = Util.int2bytes(value);
        System.arraycopy(bytes, 0, where, address, 4);
    }

    private int retrieveInt(byte[] from, int address) {
        return Util.bytes2int(Arrays.copyOfRange(from, address, address + 4));
    }

    private void pc(int number) {
        programCounter = programCounter.arith(number);
    }

    private void pcInstr() {
        pc(INSTR_SIZE);
    }

    private void pcWord() {
        pc(WORD_SIZE);
    }

    private int frameSize(int args, int locals) {
        // caller + args + locals
        return WORD_SIZE + (WORD_SIZE * args) + (WORD_SIZE * locals);
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

    private void dumpBytecode(byte[] arr, PrintWriter out) {
        int emptyCount = 0;

        int i = 0;
        while (i < arr.length && emptyCount <= 20) {
            out.print(String.format("%04d: ", i));

            for (int j = i; j < arr.length; j++) {
                out.print(String.format("%02X ", new Byte(arr[j])));
                String instr = Bytecode.bytes2strings.get(new Byte(arr[j]));
                out.println(instr);

                if (arr[j] == FREE_MARKER) {
                    emptyCount++;
                } else {
                    emptyCount = 0;
                }
//                int numOfArgs = Bytecode.strings2bytecodes.get(instr).numOfArguments;
//                for (int k = 0; k < numOfArgs; k += WORD_SIZE) {
//                    out.print(String.format("", bytes2int()));
//                }
            }
        }

        out.println();
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

        public void objectSize(Pointer objectSize) {
            field(3, objectSize);
        }

        public Pointer objectSize() {
            return field(3);
        }

        public void methods(Pointer methods) {
            field(4, methods);
        }

        public Pointer methods() {
            return field(4);
        }
    }

}
