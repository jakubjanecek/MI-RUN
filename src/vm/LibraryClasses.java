package vm;

import vm.mm.CodePointer;
import vm.mm.MM;
import vm.mm.Pointer;

import static java.util.Arrays.asList;
import static vm.Util.str2bytes;

public class LibraryClasses {

    private Claus vm;

    private MM mm;

    public LibraryClasses(Claus vm, MM mm) {
        this.vm = vm;
        this.mm = mm;
    }

    public Pointer createIntegerClass() {
        Pointer clazz = vm.newClazz(str2bytes("Integer"), MM.WORD_SIZE);

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

        Pointer methodDictionaryOfObject = vm.newMethodDictionary(asList(new Integer[]{
                vm.newMethod("add", plusMethod, 0),
                vm.newMethod("subtract", minusMethod, 0),
                vm.newMethod("multiply", multiMethod, 0),
                vm.newMethod("divide", divMethod, 0),
                vm.newMethod("modulo", modMethod, 0),
        }));
        clazz.$c().methods(methodDictionaryOfObject);

        return clazz;
    }

    public Pointer createStringClass() {
        Pointer clazz = vm.newClazz(str2bytes("String"));

        String[] lengthBC = new String[]{
                "pop-arg 0",
                "syscall " + Syscalls.calls2ints.get("str-length"),
                "return-top"
        };
        CodePointer lengthMethod = mm.storeCode(Util.translateBytecode(lengthBC));

        String[] appendBC = new String[]{
                "pop-arg 0",
                "pop-arg 1",
                "syscall " + Syscalls.calls2ints.get("str-append"),
                "return-top"
        };
        CodePointer appendMethod = mm.storeCode(Util.translateBytecode(appendBC));

        Pointer methodDictionaryOfObject = vm.newMethodDictionary(asList(new Integer[]{
                vm.newMethod("length", lengthMethod, 0),
                vm.newMethod("append", appendMethod, 0),
        }));
        clazz.$c().methods(methodDictionaryOfObject);

        return clazz;
    }

    public Pointer createArrayClass() {
        Pointer clazz = vm.newClazz(str2bytes("Array"));

        String[] lengthBC = new String[]{
                "pop-arg 0",
                "syscall " + Syscalls.calls2ints.get("arr-length"),
                "return-top"
        };
        CodePointer lengthMethod = mm.storeCode(Util.translateBytecode(lengthBC));

        String[] getBC = new String[]{
                "pop-arg 0",
                "pop-arg 1",
                "get-field",
                "return-top"
        };
        CodePointer getMethod = mm.storeCode(Util.translateBytecode(getBC));

        String[] setBC = new String[]{
                "pop-arg 0",
                "pop-arg 1",
                "pop-arg 2",
                "set-field",
                "return"
        };
        CodePointer setMethod = mm.storeCode(Util.translateBytecode(setBC));

        Pointer methodDictionaryOfObject = vm.newMethodDictionary(asList(new Integer[]{
                vm.newMethod("length", lengthMethod, 0),
                vm.newMethod("get", getMethod, 0),
                vm.newMethod("set", setMethod, 0),
        }));
        clazz.$c().methods(methodDictionaryOfObject);

        return clazz;
    }

}