package vm.mm;

import vm.Bytecode;
import vm.Util;

import java.io.PrintWriter;
import java.util.*;

public class MM {

    public static final int INSTR_SIZE = 1;

    public static final int WORD_SIZE = 4;

    public static final int REF_SIZE = WORD_SIZE;

    // MARKER + OBJ_KIND + GC_STATE + SIZE + CLASS
    public static final int HEADER_SIZE = 1 + 1 + 1 + WORD_SIZE + REF_SIZE;

    public static final byte MARKER = (byte) 0xF0;

    public static final byte FREE_MARKER = (byte) -1;

    public final Pointer NULL = new Pointer(0xFFFFFFFF, this);

    private byte[] code;
    private int firstFreeCodeByte = 0;

    private byte[] heap;
    private int firstFreeHeapByte;

    boolean firstSpace;
    private int[] space1;
    private int[] space2;

    private byte[] stack;
    private int stackPointer = 0;
    private int basePointer = NULL.address;

    private List<Method> methods;

    private List<Object> constantPool;

    private CodePointer programCounter = new CodePointer(0, this);

    private List<Pointer> classes;
    private List<Pointer> methodDictionaries;

    public MM(int codeSize, int heapSize, int stackSize) {
        code = new byte[codeSize];
        heap = new byte[heapSize];
        stack = new byte[stackSize];
        methods = new ArrayList<Method>();
        constantPool = new ArrayList<Object>();
        this.classes = new ArrayList<Pointer>();
        this.methodDictionaries = new ArrayList<Pointer>();

        space1 = new int[]{0, heapSize / 2};
        space2 = new int[]{heapSize / 2, heapSize};
        firstSpace = true;
        firstFreeHeapByte = space1[0];
    }

    public Pointer alloc(int size) {
//        System.out.println("Trying to allocate " + size + " bytes at " + firstFreeHeapByte);
        Pointer p;
        int max = firstSpace ? space1[1] : space2[1];
        if (firstFreeHeapByte + size <= max) {
            p = new Pointer(firstFreeHeapByte, this);
            clear(heap, firstFreeHeapByte, size);
            firstFreeHeapByte += size;
        } else {
            // garbage collection
            int freeBefore = firstSpace ? (space1[1] - firstFreeHeapByte) : (space2[1] - firstFreeHeapByte);
            int baker = baker();
            flip();
            int freeAfter = firstSpace ? (space1[1] - baker) : (space2[1] - baker);

            System.out.println("Before GC: " + freeBefore + " B");
            System.out.println("After GC: " + freeAfter + " B");

            firstFreeHeapByte = baker;

//            System.out.println("After Baker: " + firstFreeHeapByte);

            max = firstSpace ? space1[1] : space2[1];
            if (firstFreeHeapByte + size <= max) {
                p = new Pointer(firstFreeHeapByte, this);
                clear(heap, firstFreeHeapByte, size);
                firstFreeHeapByte += size;
            } else {
                throw new RuntimeException("Not enough memory!");
            }
        }

//        System.out.println("Allocated " + size + " bytes at " + p.address);
        return p;
    }

    public int baker() {
        System.out.println("Starting Baker...");

        Set<Pointer> rootSet = new HashSet<Pointer>(classes);
        rootSet.addAll(methodDictionaries);
        rootSet.addAll(scanStack());

        int nextAllocationPointer = firstSpace ? space2[0] : space1[0];
        int scanPointer = nextAllocationPointer;

        int objectSizeInBytes = 0;

        Map<Integer, Integer> stackReplaceTable = new HashMap<Integer, Integer>();

        for (Pointer root : rootSet) {
//            System.out.println("Baker: copying root " + root.address + " at " + nextAllocationPointer);
            objectSizeInBytes = copyObject(root, nextAllocationPointer);

//            System.out.println("putting for replacement " + root.address);
            stackReplaceTable.put(root.address, nextAllocationPointer);

            // setting GC state
            root.$().gcState(GCState.COPIED);

            // setting forward pointer, misusing class pointer because we don't need it anymore
            root.$().clazz(new Pointer(nextAllocationPointer, this));

            root.address = nextAllocationPointer;

            nextAllocationPointer += objectSizeInBytes;

//            System.out.println("NEXT: " + nextAllocationPointer);
        }

        while (scanPointer <= nextAllocationPointer) {
//            System.out.println("SCAN POINTER: " + scanPointer);

            Pointer obj = new Pointer(scanPointer, this);

            int objectSize;

            if (obj.$().kind() == ObjectKind.POINTER_INDEXED) {
                objectSize = pointerIndexedObjectSize(obj.$().size());

                int size = obj.$().size();

                for (int i = 0; i < size; i++) {
                    Pointer field = obj.$p().field(i);

                    if (field.address >= 0) {
                        if (field.$().gcState() == GCState.NORMAL) {
                            if (field.$().marker() == MARKER) {
//                                System.out.println("Baker: copying field " + field.address + " at " + nextAllocationPointer);
                                objectSizeInBytes = copyObject(field, nextAllocationPointer);

                                obj.$p().field(i, new Pointer(nextAllocationPointer, this));

                                field.$().gcState(GCState.COPIED);
                                field.$().clazz(new Pointer(nextAllocationPointer, this));

                                nextAllocationPointer += objectSizeInBytes;
                            } else {
                                //  it is not a normal pointer-based object
                                // it is a method dictionary containing indexes of methods
                                // doesn't need to be copied, already there
                            }
                        } else {
//                            System.out.println("Baker: using FP " + field.address + " at " + field.$().clazz().address);

                            // using the forward pointer
                            obj.$p().field(i, field.$().clazz());
                        }
                    }
                }
            } else {
                objectSize = byteIndexedObjectSize(obj.$().size());
            }

            scanPointer += objectSize;
        }

        replaceOnStack(stackReplaceTable);

        return nextAllocationPointer;
    }

    private int copyObject(Pointer obj, int to) {
        int objectSize = 0;
        switch (obj.$().kind()) {
            case POINTER_INDEXED:
                objectSize = pointerIndexedObjectSize(obj.$().size());
                break;
            case BYTE_INDEXED:
                objectSize = byteIndexedObjectSize(obj.$().size());
                break;
        }

//        System.out.println("Copying object " + obj.address + " to " + to + " size " + objectSize);
        System.arraycopy(heap, obj.address, heap, to, objectSize);

        return objectSize;
    }

    public Set<Pointer> scanStack() {
        Set<Pointer> active = new HashSet<Pointer>();

        int gcStackPointer = 0;
        while (gcStackPointer <= stackPointer) {
            Pointer p = retrievePointer(stack, gcStackPointer);
            if (p.address >= 0) {
                // is object
                if (p.$unsafe().marker() == MARKER) {
//                    System.out.println("@@@ " + p.address + " # " + gcStackPointer);
                    active.add(retrievePointer(stack, gcStackPointer));
                } else {
                    // skipping not objects - integers such as return address and caller frame address
                }
            }

            gcStackPointer += REF_SIZE;
        }

//        System.out.println("Found " + active.size() + " on stack...");

        return active;
    }

    public void replaceOnStack(Map<Integer, Integer> table) {
        int gcStackPointer = 0;
        while (gcStackPointer <= stackPointer) {
            Pointer p = retrievePointer(stack, gcStackPointer);
            if (p.address >= 0) {
                // is object
                if (p.$unsafe().marker() == MARKER) {
                    if (table.containsKey(p.address)) {
//                        System.out.println("replacing on stack: " + p.address + " => " + (int) table.get(p.address));
                        storeInt(stack, gcStackPointer, table.get(p.address));
                    }
                } else {
                    // skipping not objects - integers such as return address and caller frame address
                }
            }

            gcStackPointer += WORD_SIZE;
        }
    }

    private void flip() {
        if (firstSpace) {
            firstSpace = false;
            firstFreeHeapByte = space2[0];
            clear(heap, space1[0], space1[1]);
        } else {
            firstSpace = true;
            firstFreeHeapByte = space1[0];
            clear(heap, space2[0], space2[1]);
        }
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
        int currentStackPointer = stackPointer;

        int caller = retrieveInt(stack, basePointer);
        int returnAddress = retrieveInt(stack, basePointer + WORD_SIZE);

        basePointer = caller;
        stackPointer = currentBasePointer;

        clear(stack, currentBasePointer, currentStackPointer);

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

    public void addClass(Pointer root) {
        classes.add(root);
    }

    public List<Pointer> getClasses() {
        return classes;
    }

    public void addMethodDictionary(Pointer root) {
        methodDictionaries.add(root);
    }

    public List<Pointer> getMethodDictionaries() {
        return methodDictionaries;
    }

    public void setPC(CodePointer pc) {
        programCounter = pc;
    }

    public CodePointer getPC() {
        return programCounter;
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
        int i = constantPool.indexOf(constant);
        return i;
    }

    public Object constant(int index) {
        return constantPool.get(index);
    }

    public int constantIndex(Object o) {
        return constantPool.indexOf(o);
    }

    public int pointerIndexedObjectSize(int size) {
        return HEADER_SIZE + (size * REF_SIZE);
    }

    public int byteIndexedObjectSize(int size) {
        return HEADER_SIZE + size;
    }

    private void clear(byte[] what, int from, int size) {
        for (int i = from; i < from + size && from + size < what.length; i++) {
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

    public void heapObjectsDump() {
        Pointer p = new Pointer(0, this);
        while (p.address <= firstFreeHeapByte) {
//            System.out.println("@" + p.address + ": " + p.$().kind() + " " + p.$().clazz().$c().name() + " #" + p.$().size());
            int size = 0;
            switch (p.$().kind()) {
                case POINTER_INDEXED:
                    size = pointerIndexedObjectSize(p.$().size());
                    break;
                case BYTE_INDEXED:
                    size = byteIndexedObjectSize(p.$().size());
                    break;
            }
            p = new Pointer(p.address + size, this);
        }
    }

    public class Obj {

        protected Pointer pointer;
        protected static final int KIND_OFFSET = 1;
        protected static final int GC_STATE_OFFSET = KIND_OFFSET + 1;
        protected static final int SIZE_OFFSET = GC_STATE_OFFSET + 1;
        protected static final int CLASS_OFFSET = SIZE_OFFSET + WORD_SIZE;
        protected static final int DATA_OFFSET = CLASS_OFFSET + REF_SIZE;

        public Obj(Pointer startAddress) {
            this.pointer = startAddress;
        }

        public byte marker() {
            return heap[pointer.address];
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

        public GCState gcState() {
            return GCState.fromValue(heap[pointer.address + GC_STATE_OFFSET]);
        }

        public void gcState(GCState state) {
            heap[pointer.address + GC_STATE_OFFSET] = state.value;
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

        public int fieldInt(int index) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            int i = retrieveInt(heap, address);
            return i;
        }

        public void field(int index, Pointer obj) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            storePointer(heap, address, obj);
        }

        public void fieldInt(int index, int i) {
            int address = pointer.address + DATA_OFFSET + (index * REF_SIZE);
            storeInt(heap, address, i);
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

        public void name(String name) {
            int index = addConstant(name);
            fieldInt(0, index);
        }

        public String name() {
            return (String) constant(fieldInt(0));
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

        public void objectSize(int objectSize) {
            fieldInt(3, objectSize);
        }

        public int objectSize() {
            return fieldInt(3);
        }

        public void methods(Pointer methods) {
            field(4, methods);
        }

        public Pointer methods() {
            return field(4);
        }
    }

}
