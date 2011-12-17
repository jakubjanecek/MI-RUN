package vm;

import org.junit.Before;
import org.junit.Test;
import vm.mm.CodePointer;
import vm.mm.MM;
import vm.mm.Pointer;

import java.io.File;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static vm.Util.*;

public class ClausVMTest {

    private MM mm;

    private ClausVM vm;

    @Before
    public void setup() {
        int size = 4096;
        mm = new MM(size, size, size);
        vm = new ClausVM(mm);
    }

    @Test
    public void objectCreation() {
        Pointer classCar = vm.newClazz(str2bytes("Car"), 1);

        String[] methodBytecode = new String[]{
                "push-ref " + vm.newString(str2bytes("test syscall from method")).address,
                "syscall " + Syscalls.calls2ints.get("print"),

                "return"
        };

        CodePointer methodPointer = mm.storeCode(Util.translateBytecode(methodBytecode));
        Pointer methodDictionaryOfCar = vm.newMethodDictionary(asList(new Integer[]{vm.newMethod("drive", methodPointer, 0)}));
        classCar.$c().methods(methodDictionaryOfCar);

        Pointer objectCar = vm.newObject(classCar, bytes2int(classCar.$c().objectSize().$b().bytes()));
        objectCar.$p().field(0, vm.newString(str2bytes("test1")));

        // entry-point
        String[] entryPoint = new String[]{
                "push-ref " + objectCar.address,
                "call " + mm.addConstant("getObjectID"),

                "push-ref " + objectCar.address,
                "call " + mm.addConstant("drive"),

                "return"
        };
        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        assertEquals("Car", bytes2str(objectCar.$().clazz().$c().name().$b().bytes()));
        assertEquals("test1", bytes2str(objectCar.$p().field(0).$b().bytes()));

        vm.run(entryPointPointer);
    }

    @Test
    public void syscall() {
        mm.pushPointer(vm.newString(str2bytes("testing syscall directly")));
        Syscalls.ints2calls.get(1).call();
    }

    @Test(expected = RuntimeException.class)
    public void nonexistingMethod() {
        Pointer someClass = vm.newClazz(str2bytes("SomeClass"));
        Pointer anObject = vm.newObject(someClass, 0);

        vm.callMethod(anObject, "nonexisting");
    }

    @Test
    public void openFile() {
        String filename = "/Users/platinix/test.out";
        mm.pushPointer(vm.newString(str2bytes(filename)));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("open-file-w")).call();

        assertTrue(new File(filename).exists());

        new File(filename).delete();
    }

    @Test
    public void writeAndCloseFile() {
        String filename = "/Users/platinix/test1.out";
        mm.pushPointer(vm.newString(str2bytes(filename)));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("open-file-w")).call();

        Pointer handle = mm.popPointer();

        mm.pushPointer(handle);
        mm.pushPointer(vm.newString(str2bytes("test write success")));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("write-line")).call();

        mm.pushPointer(handle);
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("close-file-w")).call();

        new File(filename).delete();
    }

    @Test
    public void writeAndReadAndCloseFile() {
        String filename = "/Users/platinix/test2.out";
        mm.pushPointer(vm.newString(str2bytes(filename)));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("open-file-w")).call();

        Pointer handle = mm.popPointer();

        String expected = "test write success";

        mm.pushPointer(handle);
        mm.pushPointer(vm.newString(str2bytes(expected)));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("write-line")).call();

        mm.pushPointer(handle);
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("close-file-w")).call();

        mm.pushPointer(vm.newString(str2bytes(filename)));
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("open-file-r")).call();

        handle = mm.popPointer();

        mm.pushPointer(handle);
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("read-line")).call();

        assertEquals(expected, bytes2str(mm.popPointer().$b().bytes()));

        mm.pushPointer(handle);
        Syscalls.ints2calls.get(Syscalls.calls2ints.get("close-file-r")).call();

        new File(filename).delete();
    }

    @Test
    public void testInteger() {
        String[] entryPoint = new String[]{
                "new-int 123",
                "new-int 321",
                "call " + mm.addConstant("add"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-int 10",
                "new-int 8",
                "call " + mm.addConstant("subtract"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-int 10",
                "new-int 8",
                "call " + mm.addConstant("multiply"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-int 10",
                "new-int 5",
                "call " + mm.addConstant("divide"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-int 10",
                "new-int 8",
                "call " + mm.constantIndex("divide"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-int 5",
                "new-int 2",
                "call " + mm.addConstant("modulo"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer);
    }

    @Test
    public void testString() {
        String[] entryPoint = new String[]{
                "new-str " + mm.addConstant("new string"),
                "syscall " + Syscalls.calls2ints.get("print"),

                "new-str " + mm.addConstant("new string"),
                "call " + mm.addConstant("length"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "new-str " + mm.addConstant("Hello "),
                "new-str " + mm.addConstant("World!"),
                "call " + mm.addConstant("append"),
                "syscall " + Syscalls.calls2ints.get("print"),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer);
    }

    @Test
    public void testArray() {
        String[] entryPoint = new String[]{
                "new-arr " + mm.addConstant(10),
                "push-local 0",
                "pop-local 0",
                "call " + mm.addConstant("length"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "push-ref " + vm.newInteger(int2bytes(9)).address,
                "push-int 2",
                "pop-local 0",
                "call " + mm.addConstant("set"),

                "push-int 2",
                "pop-local 0",
                "call " + mm.addConstant("get"),
                "syscall " + Syscalls.calls2ints.get("print-int"),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer, 1);
    }

    @Test
    public void testTextFileReader() {
        String[] entryPoint = new String[]{
                "new " + vm.getClazz("TextFileReader").address,
                "push-local 0",
                "push-ref " + vm.newString(str2bytes("/Users/platinix/test.txt")).address,
                "pop-local 0",
                "call " + mm.addConstant("open"),

                "pop-local 0",
                "call " + mm.addConstant("readLine"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "pop-local 0",
                "call " + mm.addConstant("readLine"),
                "syscall " + Syscalls.calls2ints.get("print"),

                "pop-local 0",
                "call " + mm.addConstant("close"),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer, 1);
    }

    @Test
    public void testTextFileWriter() {
        String[] entryPoint = new String[]{
                "new " + vm.getClazz("TextFileWriter").address,
                "push-local 0",
                "push-ref " + vm.newString(str2bytes("/Users/platinix/test1.txt")).address,
                "pop-local 0",
                "call " + mm.addConstant("open"),

                "push-ref " + vm.newString(str2bytes("hellow world!")).address,
                "pop-local 0",
                "call " + mm.addConstant("writeLine"),


                "pop-local 0",
                "call " + mm.addConstant("close"),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer, 1);
    }

    @Test
    public void testJumps() {
        String[] entryPoint = new String[]{
                // unconditional jump
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if integers equal
                "new-int 1",
                "new-int 1",
                "jmp-eq-int " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if integers equal
                "new-int 1",
                "new-int 2",
                "jmp-eq-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if integers not equal
                "new-int 1",
                "new-int 1",
                "jmp-neq-int " + (MM.INSTR_SIZE + MM.WORD_SIZE + MM.INSTR_SIZE + MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if integers not equal
                "new-int 2",
                "new-int 1",
                "jmp-neq-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer greater than second integer
                "new-int 2",
                "new-int 1",
                "jmp-gt-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer greater than second integer
                "new-int 1",
                "new-int 1",
                "jmp-gt-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer less than second integer
                "new-int 2",
                "new-int 1",
                "jmp-lt-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer less than second integer
                "new-int 1",
                "new-int 2",
                "jmp-lt-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer greater than or equal second integer
                "new-int 2",
                "new-int 2",
                "jmp-ge-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer greater than or equal second integer
                "new-int 2",
                "new-int 1",
                "jmp-ge-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer greater than or equal second integer
                "new-int 2",
                "new-int 3",
                "jmp-ge-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer less than or equal second integer
                "new-int 2",
                "new-int 2",
                "jmp-le-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer less than or equal second integer
                "new-int 2",
                "new-int 1",
                "jmp-le-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // conditional jump if first integer less than or equal second integer
                "new-int 2",
                "new-int 3",
                "jmp-le-int " + (3 * MM.INSTR_SIZE + 3 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("did not jump"),
                "syscall " + Syscalls.calls2ints.get("print"),
                "jmp " + (2 * MM.INSTR_SIZE + 2 * MM.WORD_SIZE),
                "new-str " + mm.addConstant("jumped"),
                "syscall " + Syscalls.calls2ints.get("print"),

                // jump back (for example in while cycle)
                // int i = 0
                // while (i < 20) {
                //   i = i + 1
                //   print(i)
                // }
                "new-int 0",
                "push-local 0",
                "pop-local 0",
                "new-int 20",
                "jmp-ge-int " + (7 * MM.INSTR_SIZE + 7 * MM.WORD_SIZE),
                "pop-local 0",
                "syscall " + Syscalls.calls2ints.get("print-int"),
                "pop-local 0",
                "new-int 1",
                "call " + mm.addConstant("add"),
                "push-local 0",
                "jmp -" + (10 * MM.INSTR_SIZE + 10 * MM.WORD_SIZE),

                "return"
        };

        CodePointer entryPointPointer = mm.storeCode(Util.translateBytecode(entryPoint));

        vm.run(entryPointPointer, 1);
    }
}
