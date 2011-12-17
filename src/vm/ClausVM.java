package vm;

import vm.mm.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static vm.Util.*;

public class ClausVM {

    private MM mm;

    private BytecodeInterpreter interpreter;

    private Pointer metaclass;

    private Pointer classOfObject;

    private Pointer classOfArray;

    private Pointer classOfString;

    private Pointer classOfInteger;

    private List<Pointer> classes;

    private List<FileInputStream> inputHandles;
    private List<FileOutputStream> outputHandles;

    public static void main(String... args) {
        new ClausVM(new MM(1024, 1024, 1024));
    }

    public ClausVM(MM mm) {
        this.mm = mm;

        this.classes = new ArrayList<Pointer>();

        this.interpreter = new BytecodeInterpreter(this, mm);

        syscalls();

        bootstrap();
    }

    public void run(CodePointer entryPoint) {
        run(entryPoint, 0);
    }

    public void run(CodePointer entryPoint, int numOfLocals) {
        mm.newFrame(numOfLocals);
        mm.setPC(entryPoint);
        interpreter.interpret();
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

        LibraryClasses library = new LibraryClasses(this, mm);

        classOfInteger = library.createIntegerClass();
        classOfString = library.createStringClass();
        classOfArray = library.createArrayClass();

        classes.addAll(asList(new Pointer[]{
                metaclass, classOfObject, classOfInteger, classOfString, classOfArray
        }));

        classes.add(library.createTextFileReaderClass());
        classes.add(library.createTextFileWriterClass());
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = mm.alloc(mm.pointerIndexedObjectSize(size));

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        for (int i = 0; i < size; i++) {
            newObject.$p().field(i, mm.NULL);
        }

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

    public Pointer getClazz(String name) {
        for (Pointer clazz : classes) {
            if (bytes2str(clazz.$c().name().$b().bytes()).equals(name)) {
                return clazz;
            }
        }

        return null;
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


    public void callMethod(Pointer obj, String selector) {
        Pointer objectClass = obj.$().clazz();
        Method method = lookupMethod(objectClass, selector);

        if (method != null) {
            mm.pushPointer(obj);
            mm.newFrame(method.numOfLocals());
            interpreter.jump(method.bytecodePointer());
        } else {
            throw new RuntimeException("Method '" + selector + "' not found in class '" + bytes2str(objectClass.$c().name().$b().bytes()) + "'");
        }
    }

    public Method lookupMethod(Pointer clazz, String selector) {
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

        Syscalls.ints2calls.put(9, new Syscall("str-length") {
            @Override
            public void call() {
                mm.pushPointer(newInteger(int2bytes(bytes2str(mm.popPointer().$b().bytes()).length())));
            }
        });

        Syscalls.ints2calls.put(10, new Syscall("str-append") {
            @Override
            public void call() {
                String str = bytes2str(mm.popPointer().$b().bytes());
                str += bytes2str(mm.popPointer().$b().bytes());
                mm.pushPointer(newString(str.getBytes()));
            }
        });

        Syscalls.ints2calls.put(11, new Syscall("arr-length") {
            @Override
            public void call() {
                mm.pushPointer(newInteger(int2bytes(mm.popPointer().$().size())));
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
