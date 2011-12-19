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

    private List<BufferedReader> inputHandles;
    private List<BufferedWriter> outputHandles;

    public static void main(String... args) {
        new ClausVM(new MM(1024, 1024, 1024));
    }

    public ClausVM(MM mm) {
        this.mm = mm;

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
        inputHandles = new ArrayList<BufferedReader>();
        outputHandles = new ArrayList<BufferedWriter>();
        LibraryClasses library = new LibraryClasses(this, mm);

        // Metaclass is the class of classes, needs to be created first.
        metaclass = newClazz("Metaclass");

        // Object is the superclass of all classes, needs to be created second.
        // Object does not have a superclass. Needs to be specified directly as mm.NULL!
        classOfObject = newClazz("Object", 0, mm.NULL);

        // Now that we have Metaclass class and Object class, we can set them to the Metclass.
        metaclass.$c().metaclass(metaclass);
        metaclass.$c().superclass(classOfObject);

        // We need Array class now because it is used as a method dictionary.
        // It is created in two steps, first class is created, second methods are added to it (for the methods to be added,
        // we need the class so that method dictionary can be created).
        classOfArray = library.createArrayClass();
        library.finishArrayClass(classOfArray);

        // Now that we are able to create method dictionaries we can finish the Object class and put some methods in it.
        byte[] methodBytecode = new byte[]{Bytecode.strings2bytecodes.get("return").code};
        CodePointer methodPointer = mm.storeCode(methodBytecode);
        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Integer[]{newMethod("getObjectID", methodPointer, 0)}));
        classOfObject.$c().methods(methodDictionaryOfObject);

        // Everything is bootstrapped now, we can start creating library classes.
        classOfInteger = library.createIntegerClass();
        classOfString = library.createStringClass();

        // Finishing the library.
        library.createTextFileReaderClass();
        library.createTextFileWriterClass();
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = mm.alloc(mm.pointerIndexedObjectSize(size));

        debug("CREATING OBJECT AT " + newObject.address + " OF " + clazz.$c().name());

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().marker(MM.MARKER);
        newObject.$().gcState(GCState.NORMAL);
        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        for (int i = 0; i < size; i++) {
            newObject.$p().field(i, mm.NULL);
        }

        return newObject;
    }

    public Pointer newClazz(String name) {
        return newClazz(name, 0, null);
    }

    public Pointer newClazz(String name, int numOfFields) {
        return newClazz(name, numOfFields, null);
    }

    public Pointer newClazz(String name, int numOfFields, Pointer superclass) {
        int CLASS_SIZE = 5;

        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(CLASS_SIZE));

        debug("CREATING CLASS '" + name + "' AT " + newClass.address);

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().marker(MM.MARKER);
        newClass.$().gcState(GCState.NORMAL);
        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(CLASS_SIZE);

        if (metaclass != null) {
            newClass.$().clazz(metaclass);
        }

        if (superclass == null) {
            superclass = classOfObject;
        }
        newClass.$c().superclass(superclass);

        newClass.$c().objectSize(numOfFields);

        newClass.$c().name(name);
        newClass.$c().methods(mm.NULL);

        mm.addClass(newClass);

        return newClass;
    }

    public Pointer getClazz(String name) {
        for (Pointer clazz : mm.getClasses()) {
            if (clazz.$c().name().equals(name)) {
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
        Pointer methodDictionary = newObject(classOfArray, methods.size());

        debug("CREATING DICTIONARY AT " + methodDictionary.address);

        for (int i = 0; i < methods.size(); i++) {
            methodDictionary.$p().fieldInt(i, methods.get(i));
        }

        mm.addMethodDictionary(methodDictionary);

        return methodDictionary;
    }

    public Pointer newArray(int size) {
        return newObject(classOfArray, size);
    }

    public Pointer newString(byte[] str) {
        Pointer newString = mm.alloc(mm.byteIndexedObjectSize(str.length));

        debug("CREATING STRING '" + new String(str) + "' AT " + newString.address);

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().marker(MM.MARKER);
        newString.$().gcState(GCState.NORMAL);
        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(classOfString);
        newString.$b().bytes(str);

        return newString;
    }

    public Pointer newInteger(byte[] integer) {
        Pointer newInteger = mm.alloc(mm.byteIndexedObjectSize(MM.WORD_SIZE));

        debug("CREATING INT '" + bytes2int(integer) + "' AT " + newInteger.address);

        if (newInteger == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newInteger.$().marker(MM.MARKER);
        newInteger.$().gcState(GCState.NORMAL);
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
            throw new RuntimeException("Method '" + selector + "' not found in class.");
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
                    int methodIndex = methodDictionary.fieldInt(i);

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
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(filename));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File '" + filename + "' does not exist.");
                }

                inputHandles.add(br);
                int index = inputHandles.indexOf(br);
                mm.pushPointer(newInteger(int2bytes(index)));
            }
        });

        Syscalls.ints2calls.put(3, new Syscall("open-file-w") {
            @Override
            public void call() {
                String filename = bytes2str(mm.popPointer().$b().bytes());
                BufferedWriter bw = null;
                try {
                    bw = new BufferedWriter(new FileWriter(filename));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File '" + filename + "' does not exist.");
                } catch (IOException e) {
                    throw new RuntimeException("File '" + filename + "' could not be opened for writing.");
                }

                outputHandles.add(bw);
                int index = outputHandles.indexOf(bw);
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
                BufferedReader br = inputHandles.get(handle);
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
                BufferedWriter bw = outputHandles.get(handle);
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

        Syscalls.ints2calls.put(12, new Syscall("str-split") {
            @Override
            public void call() {
                String str = bytes2str(mm.popPointer().$b().bytes());
                String[] splitted = str.split(" ");
                Pointer arr = newArray(splitted.length);
                for (int i = 0; i < splitted.length; i++) {
                    arr.$p().field(i, newString(str2bytes(splitted[i])));
                }
                mm.pushPointer(arr);
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
