package vm;

import org.junit.Before;
import org.junit.Test;
import vm.mm.CodePointer;
import vm.mm.MM;
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

        byte[] stringRef = int2bytes(vm.newString(str2bytes("test syscall from method")).address);
        byte[] printSyscall = int2bytes(Syscalls.calls2ints.get("print"));
        byte[] methodBytecode = new byte[]{
                Bytecode.strings2bytecodes.get("push-ref").code, stringRef[0], stringRef[1], stringRef[2], stringRef[3],
                Bytecode.strings2bytecodes.get("syscall").code, printSyscall[0], printSyscall[1], printSyscall[2], printSyscall[3],

                Bytecode.strings2bytecodes.get("return").code
        };

        CodePointer methodPointer = mm.storeCode(methodBytecode);
        Pointer methodDictionaryOfCar = vm.newMethodDictionary(asList(new Integer[]{vm.newMethod("drive", methodPointer, 0)}));
        classCar.$c().methods(methodDictionaryOfCar);

        Pointer objectCar = vm.newObject(classCar, 2);
        objectCar.$p().field(0, vm.newString(str2bytes("test1")));

        // entry-point
        byte[] carRef = int2bytes(objectCar.address);
        byte[] getObjectIDMethodName = int2bytes(vm.newString(str2bytes("getObjectID")).address);
        byte[] driveMethodName = int2bytes(vm.newString(str2bytes("drive")).address);
        byte[] entryPoint = new byte[]{
                Bytecode.strings2bytecodes.get("push-ref").code, carRef[0], carRef[1], carRef[2], carRef[3],
                Bytecode.strings2bytecodes.get("call").code, getObjectIDMethodName[0], getObjectIDMethodName[1], getObjectIDMethodName[2], getObjectIDMethodName[3],

                Bytecode.strings2bytecodes.get("push-ref").code, carRef[0], carRef[1], carRef[2], carRef[3],
                Bytecode.strings2bytecodes.get("call").code, driveMethodName[0], driveMethodName[1], driveMethodName[2], driveMethodName[3],

                Bytecode.strings2bytecodes.get("return").code
        };
        CodePointer entryPointPointer = mm.storeCode(entryPoint);

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
}