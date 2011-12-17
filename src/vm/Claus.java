package vm;

import vm.mm.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static vm.Util.*;

public class Claus {

    private MM mm;

    private Pointer metaclass;

    private Pointer classOfObject;

    private Pointer classOfArray;

    private Pointer classOfString;

    private Pointer classOfInteger;

    private List<FileInputStream> inputHandles;
    private List<FileOutputStream> outputHandles;

    public static void main(String... args) {
        new Claus(new MM(1024, 1024, 1024));
    }

    public Claus(MM mm) {
        this.mm = mm;

        syscalls();

        bootstrap();
    }

    public void run(CodePointer entryPoint) {
        mm.newFrame(0);
        mm.setPC(entryPoint);
        interpret();
    }

    private final void bootstrap() {
        inputHandles = new ArrayList<FileInputStream>();
        outputHandles = new ArrayList<FileOutputStream>();

        metaclass = newClazz(str2bytes("Metaclass"));
        // Object does not have a superclass! Needs to be specified directly here!
        classOfObject = newClazz(str2bytes("Object"), 0, mm.NULL);
        metaclass.$c().metaclass(metaclass);
        metaclass.$c().superclass(classOfObject);

        byte[] methodBytecode = new byte[]{Bytecode.strings2bytecodes.get("return").code};
        CodePointer methodPointer = mm.storeCode(methodBytecode);
        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Integer[]{newMethod("getObjectID", methodPointer, 0)}));
        classOfObject.$c().methods(methodDictionaryOfObject);

        // TODO prepare internal classes such as Array and String
        classOfInteger = createIntegerClass();
        classOfString = createStringClass();
        classOfArray = newClazz(str2bytes("Array"));
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = mm.alloc(mm.pointerIndexedObjectSize(size));

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        return newObject;
    }

    public Pointer newClazz(byte[] name) {
        return newClazz(name, 0, null);
    }

    public Pointer newClazz(byte[] name, int numOfFields) {
        return newClazz(name, numOfFields, null);
    }

    public Pointer newClazz(byte[] name, int numOfFields, Pointer superclass) {
        int CLASS_SIZE = 5;

        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(CLASS_SIZE));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(CLASS_SIZE);

        if (metaclass != null) {
            newClass.$().clazz(metaclass);
        }

        if (superclass == null) {
            superclass = classOfObject;
        }
        newClass.$c().superclass(superclass);

        newClass.$c().objectSize(newInteger(int2bytes(numOfFields)));

        newClass.$c().name(newString(name));
        newClass.$c().methods(mm.NULL);

        return newClass;
    }

    /**
     * @return method index
     */
    public int newMethod(String selector, CodePointer bytecode, int numOfLocals) {
        Method m = new Method(selector, bytecode, numOfLocals);
        return mm.addMethod(m);
    }

    public Pointer newMethodDictionary(List<Integer> methods) {
        Pointer methodDictionary = newObject(classOfObject, methods.size());

        for (int i = 0; i < methods.size(); i++) {
            methodDictionary.$p().field(i, newInteger(int2bytes(methods.get(i))));
        }

        return methodDictionary;
    }

    public Pointer newArray(int size) {
        return newObject(classOfArray, size);
    }

    public Pointer newString(byte[] str) {
        Pointer newString = mm.alloc(mm.byteIndexedObjectSize(str.length));

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(classOfString);
        newString.$b().bytes(str);

        return newString;
    }

    public Pointer newInteger(byte[] integer) {
        Pointer newInteger = mm.alloc(mm.byteIndexedObjectSize(MM.WORD_SIZE));

        if (newInteger == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newInteger.$().kind(ObjectKind.BYTE_INDEXED);
        newInteger.$().size(MM.WORD_SIZE);
        newInteger.$().clazz(classOfInteger);
        newInteger.$b().bytes(integer);

        return newInteger;
    }

    private void interpret() {
        boolean interpret = true;
        while (interpret) {
            byte instruction = mm.getByteFromBC();
            switch (instruction) {
                // syscall syscall-number
                case 0x01:
                    int syscall = mm.getIntFromBC();
                    Syscalls.ints2calls.get(syscall).call();
                    break;
                // call selector-index
                case 0x02:
                    String methodSelector = (String) mm.constant(mm.getIntFromBC());
                    callMethod(mm.popPointer(), methodSelector);
                    break;
                // return
                case 0x03:
                    CodePointer jumpAddress = mm.discardFrame();
                    if (jumpAddress == null) {
                        interpret = false;
                    } else {
                        jump(jumpAddress);
                    }
                    break;
                // return-top
                case 0x04:
                    Pointer returnValue = mm.popPointer();
                    jumpAddress = mm.discardFrame();
                    mm.pushPointer(returnValue);
                    if (jumpAddress == null) {
                        interpret = false;
                    } else {
                        jump(jumpAddress);
                    }
                    break;
                // new clazz-pointer
                case 0x05:
                    Pointer clazz = mm.getPointerFromBC();
                    Pointer obj = newObject(clazz, bytes2int(clazz.$c().objectSize().$b().bytes()));
                    mm.pushPointer(obj);
                    break;
                // get-field index
                case 0x06:
                    int index = mm.getIntFromBC();
                    obj = mm.popPointer();
                    mm.pushPointer(obj.$p().field(index));
                    break;
                // set-field index pointer
                case 0x07:
                    index = mm.getIntFromBC();
                    Pointer setValue = mm.getPointerFromBC();
                    obj = mm.popPointer();
                    obj.$p().field(index, setValue);
                    break;
                // push-ref pointer
                case 0x08:
                    mm.pushPointer(mm.getPointerFromBC());
                    break;
                // pop-ref
                case 0x09:
                    mm.popPointer();
                    break;
                // push-int number
                case 0x0A:
                    mm.pushInt(mm.getIntFromBC());
                    break;
                // pop-int
                case 0xB:
                    mm.popInt();
                    break;
                // push-local index val-pointer
                case 0x0C:
                    index = mm.getIntFromBC();
                    obj = mm.getPointerFromBC();
                    mm.local(index, obj);
                    break;
                // pop-local index
                case 0x0D:
                    index = mm.getIntFromBC();
                    mm.pushPointer(mm.local(index));
                    break;
                // add-int
                case 0x0E:
                    Pointer operand2 = mm.popPointer();
                    Pointer operand1 = mm.popPointer();
                    int sum = bytes2int(operand1.$b().bytes()) + bytes2int(operand2.$b().bytes());
                    mm.pushPointer(newInteger(int2bytes(sum)));
                    break;
                // sub-int
                case 0x0F:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int diff = bytes2int(operand1.$b().bytes()) - bytes2int(operand2.$b().bytes());
                    mm.pushPointer(newInteger(int2bytes(diff)));
                    break;
                // mul-int
                case 0x10:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int product = bytes2int(operand1.$b().bytes()) * bytes2int(operand2.$b().bytes());
                    mm.pushPointer(newInteger(int2bytes(product)));
                    break;
                // div-int
                case 0x11:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int division = bytes2int(operand1.$b().bytes()) / bytes2int(operand2.$b().bytes());
                    mm.pushPointer(newInteger(int2bytes(division)));
                    break;
                // mod-int
                case 0x12:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int modulo = bytes2int(operand1.$b().bytes()) % bytes2int(operand2.$b().bytes());
                    mm.pushPointer(newInteger(int2bytes(modulo)));
                    break;
                // push-arg index val-pointer
                case 0x13:
                    index = mm.getIntFromBC();
                    obj = mm.getPointerFromBC();
                    mm.arg(index, obj);
                    break;
                // pop-arg index
                case 0x14:
                    index = mm.getIntFromBC();
                    mm.pushPointer(mm.arg(index));
                    break;
                // set-bytes number
                case 0x15:
                    int number = mm.getIntFromBC();
                    obj = mm.popPointer();
                    obj.$b().bytes(int2bytes(number));
                    break;
                // new-int
                case 0x16:
                    obj = newInteger(int2bytes(mm.getIntFromBC()));
                    mm.pushPointer(obj);
                    break;
                // new-str
                case 0x17:
                    obj = newString(str2bytes((String) mm.constant((int) mm.getIntFromBC())));
                    mm.pushPointer(obj);
                    break;
                // new-arr
                case 0x18:
                    // TODO implement new-arr
                    break;
                // NOP = no operation
                case 0x00:
                    interpret = false;
                    break;
                default:
                    throw new RuntimeException("Unknown instruction.");
            }
        }
    }

    protected void callMethod(Pointer obj, String selector) {
        Pointer objectClass = obj.$().clazz();
        Method method = lookupMethod(objectClass, selector);

        if (method != null) {
            mm.pushPointer(obj);
            mm.newFrame(method.numOfLocals());
            jump(method.bytecodePointer());
        } else {
            throw new RuntimeException("Method '" + selector + "' not found in class '" + bytes2str(objectClass.$c().name().$b().bytes()) + "'");
        }
    }

    private Method lookupMethod(Pointer clazz, String selector) {
        Pointer current = clazz;
        while (!current.isNull()) {
            MM.Clazz c = current.$c();
            Pointer classMethods = c.methods();

            if (!classMethods.isNull()) {
                MM.PointerIndexedObj methodDictionary = classMethods.$p();
                int size = methodDictionary.size();

                for (int i = 0; i < size; i++) {
                    Pointer methodPointer = methodDictionary.field(i);
                    int methodIndex = bytes2int(methodPointer.$b().bytes());

                    Method m = mm.method(methodIndex);

                    if (m.selector().equals(selector)) {
                        return m;
                    }
                }
            }

            current = c.superclass();
        }

        return null;
    }

    private void jump(CodePointer where) {
        mm.setPC(where);
    }

    private Pointer createIntegerClass() {
        Pointer clazz = newClazz(str2bytes("Integer"), MM.WORD_SIZE);

        String[] plusBC = new String[]{
                "pop-arg 1",
                "pop-arg 0",
                "add-int",
                "return-top"
        };
        CodePointer plusMethod = mm.storeCode(Util.translateBytecode(plusBC));

        String[] minusBC = new String[]{
                "pop-arg 1",
                "pop-arg 0",
                "sub-int",
                "return-top"
        };
        CodePointer minusMethod = mm.storeCode(Util.translateBytecode(minusBC));

        String[] multiBC = new String[]{
                "pop-arg 1",
                "pop-arg 0",
                "mul-int",
                "return-top"
        };
        CodePointer multiMethod = mm.storeCode(Util.translateBytecode(multiBC));

        String[] divBC = new String[]{
                "pop-arg 1",
                "pop-arg 0",
                "div-int",
                "return-top"
        };
        CodePointer divMethod = mm.storeCode(Util.translateBytecode(divBC));

        String[] modBC = new String[]{
                "pop-arg 1",
                "pop-arg 0",
                "mod-int",
                "return-top"
        };
        CodePointer modMethod = mm.storeCode(Util.translateBytecode(modBC));

        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Integer[]{
                newMethod("add", plusMethod, 0),
                newMethod("subtract", minusMethod, 0),
                newMethod("multiply", multiMethod, 0),
                newMethod("divide", divMethod, 0),
                newMethod("modulo", modMethod, 0),
        }));
        clazz.$c().methods(methodDictionaryOfObject);

        return clazz;
    }

    private Pointer createStringClass() {
        Pointer clazz = newClazz(str2bytes("String"));

        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Integer[]{
//                        newMethod("add", plusMethod, 0),
        }));
        clazz.$c().methods(methodDictionaryOfObject);

        return clazz;
    }

    private void syscalls() {
        Syscalls.ints2calls.put(1, new Syscall("print") {
            @Override
            public void call() {
                System.out.println(bytes2str(mm.popPointer().$b().bytes()));
            }
        });

        Syscalls.ints2calls.put(2, new Syscall("open-file-r") {
            @Override
            public void call() {
                String filename = bytes2str(mm.popPointer().$b().bytes());
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(filename);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File '" + filename + "' does not exist.");
                }

                inputHandles.add(fis);
                int index = inputHandles.indexOf(fis);
                mm.pushPointer(newInteger(int2bytes(index)));
            }
        });

        Syscalls.ints2calls.put(3, new Syscall("open-file-w") {
            @Override
            public void call() {
                String filename = bytes2str(mm.popPointer().$b().bytes());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File '" + filename + "' does not exist.");
                }

                outputHandles.add(fos);
                int index = outputHandles.indexOf(fos);
                mm.pushPointer(newInteger(int2bytes(index)));
            }
        });

        Syscalls.ints2calls.put(4, new Syscall("close-file-r") {
            @Override
            public void call() {
                int handle = bytes2int(mm.popPointer().$b().bytes());
                Closeable closeable = inputHandles.get(handle);
                try {
                    closeable.close();
                } catch (IOException e) {
                    throw new RuntimeException("File could not be closed.");
                }
            }
        });

        Syscalls.ints2calls.put(5, new Syscall("close-file-w") {
            @Override
            public void call() {
                int handle = bytes2int(mm.popPointer().$b().bytes());
                Closeable closeable = outputHandles.get(handle);
                try {
                    closeable.close();
                } catch (IOException e) {
                    throw new RuntimeException("File could not be closed.");
                }
            }
        });

        Syscalls.ints2calls.put(6, new Syscall("read-line") {
            @Override
            public void call() {
                int handle = bytes2int(mm.popPointer().$b().bytes());
                FileInputStream fis = inputHandles.get(handle);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                try {
                    Pointer str = newString(str2bytes(br.readLine()));
                    mm.pushPointer(str);
                } catch (IOException e) {
                    throw new RuntimeException("IO error while reading file.");
                }
            }
        });

        Syscalls.ints2calls.put(7, new Syscall("write-line") {
            @Override
            public void call() {
                String str = bytes2str(mm.popPointer().$b().bytes());
                int handle = bytes2int(mm.popPointer().$b().bytes());
                FileOutputStream fos = outputHandles.get(handle);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                try {
                    bw.write(str);
                    bw.flush();
                } catch (IOException e) {
                    throw new RuntimeException("IO error while writing file.");
                }
            }
        });

        Syscalls.ints2calls.put(8, new Syscall("print-int") {
            @Override
            public void call() {
                System.out.println(bytes2int(mm.popPointer().$b().bytes()));
            }
        });

        // needs to be called
        Syscalls.generateReversedTable();
    }

    public abstract class Syscall {

        public String name;

        public Syscall(String name) {
            this.name = name;
        }

        abstract public void call();

    }

}
