package vm;

import vm.mm.*;

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

    private void bootstrap() {
        metaclass = newClazz(str2bytes("Metaclass"));
        classOfObject = newClazz(str2bytes("Object"), mm.NULL);
        metaclass.$c().metaclass(metaclass);
        metaclass.$c().superclass(classOfObject);

        byte[] methodBytecode = new byte[]{Bytecode.strings2bytecodes.get("return").code};
        CodePointer methodPointer = mm.storeCode(methodBytecode);
        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Integer[]{newMethod("getObjectID", methodPointer, 0)}));
        classOfObject.$c().methods(methodDictionaryOfObject);

        classOfArray = newClazz(str2bytes("Array"));
        classOfString = newClazz(str2bytes("String"));
        classOfInteger = newClazz(str2bytes("Integer"));

        // TODO
        // prepare classes such as Integer, String, Array and so on
        // necessary to add methdos to them
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
        return newClazz(name, null);
    }

    public Pointer newClazz(byte[] name, Pointer superclass) {
        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(4));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(4);

        if (metaclass != null) {
            newClass.$().clazz(metaclass);
        }

        if (superclass == null) {
            superclass = classOfObject;
        }
        newClass.$c().superclass(superclass);

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
                // syscall
                case 0x01:
                    int syscall = mm.getIntFromBC();
                    Syscalls.ints2calls.get(syscall).call();
                    break;
                // call
                case 0x02:
                    String methodSelector = bytes2str(mm.getPointerFromBC().$b().bytes());
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
                // push-ref
                case 0x04:
                    mm.pushPointer(mm.getPointerFromBC());
                    break;
                // pop-ref
                case 0x05:
                    mm.popPointer();
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

    // OTHER INSTRUCTIONS WE WILL POSSIBLY NEED
    // open-file
    // read-char
    // read-line
    // write-char
    // write-line
    // close-file
    private void syscalls() {
        Syscalls.ints2calls.put(1, new Syscall("print") {
            @Override
            public void call() {
                System.out.println(bytes2str(mm.popPointer().$b().bytes()));
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
