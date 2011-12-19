package vm;

import vm.mm.CodePointer;
import vm.mm.MM;
import vm.mm.Pointer;

import java.util.Arrays;

public class KnapsackManual {

    private ClausVM vm;
    private MM mm;
    private Pointer knapsackClass;

    public static void main(String[] args) {
        MM mm = new MM(4096, 4096, 4096);
        ClausVM vm = new ClausVM(mm);

        new KnapsackManual(vm, mm);
    }

    public KnapsackManual(ClausVM vm, MM mm) {
        this.vm = vm;
        this.mm = mm;

        go();
    }

    private void go() {
        defineKnapsackClass();

        vm.run(entryPoint(), 1);
    }

    private CodePointer entryPoint() {
        String[] entryPoint = new String[]{
                "new " + knapsackClass.address,
                "push-local 0",

                "pop-local 0",
                "call " + mm.addConstant("load"),

                "pop-local 0",
                "call " + mm.addConstant("init"),

                "return"
        };

        return mm.storeCode(Util.translateBytecode(entryPoint));
    }

    private void defineKnapsackClass() {
        knapsackClass = vm.newClazz("Knapsack", 6);

        String[] init = new String[]{
                // split input
                "pop-arg 0",
                "push-int 5",
                "get-field",
                "call " + mm.addConstant("split"),
                "push-local 0",

                // set n
                "pop-arg 0",
                "push-int 1",
                "pop-local 0",
                "push-int 0",
                "get-field",
                "cast-str-int",
                "set-field",

                // set M
                "pop-arg 0",
                "push-int 2",
                "pop-local 0",
                "push-int 1",
                "get-field",
                "cast-str-int",
                "set-field",

//                "pop-arg 0",
//                "push-int 2",
//                "get-field",
//                "syscall " + Syscalls.calls2ints.get("print-int"),

                "pop-arg 0",
                "push-int 3",
                "new-arr " + mm.addConstant(4),
                "set-field",

                "pop-arg 0",
                "push-int 4",
                "new-arr " + mm.constantIndex(4),
                "set-field",

                "return"
        };

        String[] load = new String[]{
                "pop-arg 0",
                "push-int 0",
                "new-str " + mm.addConstant("input.dat"),
                "set-field",

                "pop-arg 0",
                "push-int 0",
                "get-field",
                "syscall " + Syscalls.calls2ints.get("open-file-r"),
                "push-local 0",

                "pop-arg 0",
                "push-int 5",
                "pop-local 0",
                "syscall " + Syscalls.calls2ints.get("read-line"),
                "set-field",

                "pop-local 0",
                "syscall " + Syscalls.calls2ints.get("close-file-r"),
                "return"
        };

        Pointer dictionary = vm.newMethodDictionary(Arrays.asList(new Integer[]{
                vm.newMethod("init", mm.storeCode(Util.translateBytecode(init)), 1),
                vm.newMethod("load", mm.storeCode(Util.translateBytecode(load)), 1)
        }));

        knapsackClass.$c().methods(dictionary);
    }
}
