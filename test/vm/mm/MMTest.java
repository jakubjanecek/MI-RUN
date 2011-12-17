package vm.mm;

import org.junit.Before;
import org.junit.Test;
import vm.ClausVM;
import vm.Util;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static vm.Util.*;

public class MMTest {

    private MM mm;
    private ClausVM vm;

    @Before
    public void setup() {
        mm = new MM(1024, 1024, 1024);
        vm = new ClausVM(mm);
    }

    @Test
    public void code() {
        byte[] bytes = new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06};
        CodePointer p = mm.storeCode(bytes);

        mm.setPC(p);
        assertEquals(bytes[0], mm.getByteFromBC());
        byte[] intBytes = Arrays.copyOfRange(bytes, 1, 5);
        assertEquals(Util.bytes2int(intBytes), mm.getIntFromBC());
        assertEquals(bytes[5], mm.getByteFromBC());
    }

    @Test
    public void stack() {
        mm.pushPointer(vm.newInteger(int2bytes(1)));
        mm.pushPointer(vm.newString(str2bytes("two")));
        mm.pushPointer(vm.newInteger(int2bytes(3)));

        assertEquals(3, bytes2int(mm.popPointer().$b().bytes()));
        assertEquals("two", bytes2str(mm.popPointer().$b().bytes()));
        assertEquals(1, bytes2int(mm.popPointer().$b().bytes()));
    }

    @Test(expected = RuntimeException.class)
    public void stackOverflow() {
        MM mm = new MM(1024, 1024, 2);
        ClausVM vm = new ClausVM(mm);

        mm.pushPointer(vm.newInteger(int2bytes(1)));
        mm.pushPointer(vm.newString(str2bytes("two")));
        mm.pushPointer(vm.newInteger(int2bytes(3)));
    }

    @Test(expected = RuntimeException.class)
    public void stackUnderflow() {
        mm.pushPointer(vm.newInteger(int2bytes(1)));

        mm.popPointer();
        mm.popPointer();
    }

    @Test
    public void stackLocals() {
        mm.newFrame(3);

        mm.local(1, vm.newInteger(int2bytes(123)));
        mm.local(0, vm.newInteger(int2bytes(321)));
        mm.local(2, vm.newInteger(int2bytes(999)));

        assertEquals(321, bytes2int(mm.local(0).$b().bytes()));
        assertEquals(123, bytes2int(mm.local(1).$b().bytes()));
        assertEquals(999, bytes2int(mm.local(2).$b().bytes()));

        mm.local(1, vm.newInteger(int2bytes(852)));
        assertEquals(852, bytes2int(mm.local(1).$b().bytes()));

        mm.pushInt(3);

        mm.newFrame(2);

        mm.local(1, vm.newInteger(int2bytes(2)));
        mm.local(0, vm.newInteger(int2bytes(1)));

        mm.pushInt(999);
        mm.pushInt(853);

        assertEquals(1, bytes2int(mm.local(0).$b().bytes()));
        assertEquals(2, bytes2int(mm.local(1).$b().bytes()));
        assertEquals(853, mm.popInt());

        mm.discardFrame();

        assertEquals(3, mm.popInt());

        assertEquals(321, bytes2int(mm.local(0).$b().bytes()));
        assertEquals(852, bytes2int(mm.local(1).$b().bytes()));
        assertEquals(999, bytes2int(mm.local(2).$b().bytes()));

        assertNull(mm.discardFrame());
    }


    @Test
    public void stackArgs() {
        mm.pushPointer(vm.newInteger(int2bytes(2)));
        mm.pushPointer(vm.newInteger(int2bytes(1)));

        mm.newFrame(1);

        mm.local(0, vm.newInteger(int2bytes(321)));
        assertEquals(321, bytes2int(mm.local(0).$b().bytes()));

        assertEquals(1, bytes2int(mm.arg(0).$b().bytes()));
        assertEquals(2, bytes2int(mm.arg(1).$b().bytes()));
    }

}
