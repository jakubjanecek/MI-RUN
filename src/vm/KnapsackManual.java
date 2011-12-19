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
        int memSize = 256 * 12;
        MM mm = new MM(memSize, memSize, memSize);
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
                // println("Knapsack input:")
                "new-str " + mm.addConstant("Knapsack input:"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // println(this.line)
                "pop-arg 0",
                "push-int 5",
                "get-field",
                "syscall " + Syscalls.calls2ints.get("print"),

                // var splitted
                // splitted = this.line.split
                "pop-arg 0",
                "push-int 5",
                "get-field",
                "call " + mm.addConstant("split"),
                "push-local 0",

                // this.n = splitted[0]
                "pop-arg 0",
                "push-int 1",
                "pop-local 0",
                "push-int 0",
                "get-field",
                "cast-str-int",
                "set-field",

                // this.M = splitted[1]
                "pop-arg 0",
                "push-int 2",
                "pop-local 0",
                "push-int 1",
                "get-field",
                "cast-str-int",
                "set-field",

                // weights
                "pop-arg 0",
                "push-int 3",
                "new-arr " + mm.addConstant(4),
                "set-field",

                // prices
                "pop-arg 0",
                "push-int 4",
                "new-arr " + mm.constantIndex(4),
                "set-field",

                // int i = 2
                // int j = 0
                // int k = splitted.length
                // while (i < k) {
                //   i = i + 2
                //   j = j + 1
                //   weights[j] = splitted[i]
                // }

                // i = 2
                "new-int 2",
                "push-local 1",

                // j = 0
                "new-int 0",
                "push-local 2",

                // k = splitted.length
                "pop-local 0",
                "call " + mm.addConstant("length"),
                "push-local 3",

                "pop-local 1",
                "pop-local 3",
                "jmp-ge-int " + (18 * MM.INSTR_SIZE + 14 * MM.WORD_SIZE),

                // weights[j] = splitted[i]
                // weights
                "pop-arg 0",
                "push-int 3",
                "get-field",

                // j
                "pop-local 2",

                // splitted[i]
                "pop-local 0",
                "pop-local 1",
                "get-field-dyn",
                "cast-str-int",
                "set-field-dyn",

                // i = i + 2
                "pop-local 1",
                "new-int 2",
                "call " + mm.addConstant("add"),
                "push-local 1",

                // j = j + 1
                "pop-local 2",
                "new-int 1",
                "call " + mm.addConstant("add"),
                "push-local 2",

                "jmp -" + (21 * MM.INSTR_SIZE + 17 * MM.WORD_SIZE),


                // i = 3
                // j = 0
                // while (i < k) {
                //   i = i + 2
                //   j = j + 1
                //   price[j] = splitted[i]
                // }

                // i = 3
                "new-int 3",
                "push-local 1",

                // j = 0
                "new-int 0",
                "push-local 2",

                "pop-local 1",
                "pop-local 3",
                "jmp-ge-int " + (18 * MM.INSTR_SIZE + 14 * MM.WORD_SIZE),

                // price[j] = splitted[i]
                // price
                "pop-arg 0",
                "push-int 4",
                "get-field",

                // j
                "pop-local 2",

                // splitted[i]
                "pop-local 0",
                "pop-local 1",
                "get-field-dyn",
                "cast-str-int",
                "set-field-dyn",

                // i = i + 2
                "pop-local 1",
                "new-int 2",
                "call " + mm.addConstant("add"),
                "push-local 1",

                // j = j + 1
                "pop-local 2",
                "new-int 1",
                "call " + mm.addConstant("add"),
                "push-local 2",

                "jmp -" + (21 * MM.INSTR_SIZE + 17 * MM.WORD_SIZE),

                // println(weights[0])
//                "pop-arg 0",
//                "push-int 4",
//                "get-field",
//                "push-int 3",
//                "get-field",
//                "syscall " + Syscalls.calls2ints.get("print-int"),

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
                vm.newMethod("init", mm.storeCode(Util.translateBytecode(init)), 4),
                vm.newMethod("load", mm.storeCode(Util.translateBytecode(load)), 1)
        }));

        knapsackClass.$c().methods(dictionary);
    }
}
