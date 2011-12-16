package vm;

import org.junit.Before;
import org.junit.Test;
import vm.mm.CodePointer;
import vm.mm.MM;
import vm.mm.Method;
import vm.mm.Pointer;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static vm.Util.*;

public class ClausTest {

    private MM mm;

    private Claus vm;

    @Before
    public void setup() {
        mm = new MM(1024, 1024, 1024);
        vm = new Claus(mm);
    }

    @Test
    public void objectCreation() {
        Pointer classCar = vm.newClazz(str2bytes("Car"), null);

        byte[] printSyscall = int2bytes(Syscalls.calls2ints.get("print"));
        byte[] methodBytecode = new byte[]{
                Bytecode.strings2bytecodes.get("syscall").code, printSyscall[0], printSyscall[1], printSyscall[2], printSyscall[3]
        };

        CodePointer methodPointer = mm.storeCode(methodBytecode);
        Pointer methodDictionaryOfCar = vm.newMethodDictionary(asList(new Method[]{vm.newMethod("drive", methodPointer)}));
        classCar.$c().methods(methodDictionaryOfCar);

        Pointer objectCar = vm.newObject(classCar, 2);
        objectCar.$p().field(0, vm.newString(str2bytes("test1")));

        assertEquals("Car", bytes2str(objectCar.$().clazz().$c().name().$b().bytes()));
        assertEquals("test1", bytes2str(objectCar.$p().field(0).$b().bytes()));

        vm.callMethod(objectCar, "getObjectID");
        mm.push(vm.newString(str2bytes("test syscall from method")));
        vm.callMethod(objectCar, "drive");
    }

    @Test
    public void syscall() {
        mm.push(vm.newString(str2bytes("testing syscall directly")));
        Syscalls.ints2calls.get(1).call();
    }

    @Test(expected = RuntimeException.class)
    public void nonexistingMethod() {
        Pointer someClass = vm.newClazz(str2bytes("SomeClass"));
        Pointer anObject = vm.newObject(someClass, 0);

        vm.callMethod(anObject, "nonexisting");
    }
}
