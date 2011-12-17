package vm;

import vm.mm.CodePointer;
import vm.mm.MM;
import vm.mm.Pointer;

import static vm.Util.*;

public class BytecodeInterpreter {

    private ClausVM vm;

    private MM mm;

    public BytecodeInterpreter(ClausVM vm, MM mm) {
        this.vm = vm;
        this.mm = mm;
    }

    public void interpret() {
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
                    vm.callMethod(mm.popPointer(), methodSelector);
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
                    Pointer obj = vm.newObject(clazz, bytes2int(clazz.$c().objectSize().$b().bytes()));
                    mm.pushPointer(obj);
                    break;
                // get-field
                case 0x06:
                    int index = mm.popInt();
                    obj = mm.popPointer();
                    mm.pushPointer(obj.$p().field(index));
                    break;
                // set-field
                case 0x07:
                    Pointer setValue = mm.popPointer();
                    index = mm.popInt();
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
                    obj = mm.popPointer();
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

                    mm.pushPointer(vm.newInteger(int2bytes(sum)));
                    break;
                // sub-int
                case 0x0F:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int diff = bytes2int(operand1.$b().bytes()) - bytes2int(operand2.$b().bytes());
                    mm.pushPointer(vm.newInteger(int2bytes(diff)));
                    break;
                // mul-int
                case 0x10:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int product = bytes2int(operand1.$b().bytes()) * bytes2int(operand2.$b().bytes());
                    mm.pushPointer(vm.newInteger(int2bytes(product)));
                    break;
                // div-int
                case 0x11:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int division = bytes2int(operand1.$b().bytes()) / bytes2int(operand2.$b().bytes());
                    mm.pushPointer(vm.newInteger(int2bytes(division)));
                    break;
                // mod-int
                case 0x12:
                    operand2 = mm.popPointer();
                    operand1 = mm.popPointer();
                    int modulo = bytes2int(operand1.$b().bytes()) % bytes2int(operand2.$b().bytes());
                    mm.pushPointer(vm.newInteger(int2bytes(modulo)));
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
                    obj = vm.newInteger(int2bytes(mm.getIntFromBC()));
                    mm.pushPointer(obj);
                    break;
                // new-str
                case 0x17:
                    obj = vm.newString(str2bytes((String) mm.constant(mm.getIntFromBC())));
                    mm.pushPointer(obj);
                    break;
                // new-arr
                case 0x18:
                    mm.pushPointer(vm.newArray((Integer) mm.constant(mm.getIntFromBC())));
                    break;
                // jmp
                case 0x19:
                    int offset = mm.getIntFromBC();
                    jump(mm.getPC().arith(offset));
                    break;
                // jmp-eq-int
                case 0x1A:
                    offset = mm.getIntFromBC();
                    int i1 = bytes2int(mm.popPointer().$b().bytes());
                    int i2 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 == i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                // jmp-neq-int
                case 0x1B:
                    offset = mm.getIntFromBC();
                    i1 = bytes2int(mm.popPointer().$b().bytes());
                    i2 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 != i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                // jmp-gt-int
                case 0x1C:
                    offset = mm.getIntFromBC();
                    i2 = bytes2int(mm.popPointer().$b().bytes());
                    i1 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 > i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                // jmp-ge-int
                case 0x1D:
                    offset = mm.getIntFromBC();
                    i2 = bytes2int(mm.popPointer().$b().bytes());
                    i1 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 >= i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                // jmp-lt-int
                case 0x1E:
                    offset = mm.getIntFromBC();
                    i2 = bytes2int(mm.popPointer().$b().bytes());
                    i1 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 < i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                // jmp-le-int
                case 0x1F:
                    offset = mm.getIntFromBC();
                    i2 = bytes2int(mm.popPointer().$b().bytes());
                    i1 = bytes2int(mm.popPointer().$b().bytes());
                    if (i1 <= i2) {
                        jump(mm.getPC().arith(offset));
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown instruction.");
            }
        }
    }

    public void jump(CodePointer where) {
        mm.setPC(where);
    }

}
