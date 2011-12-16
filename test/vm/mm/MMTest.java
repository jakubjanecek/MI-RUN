package vm.mm;

import org.junit.Before;
import org.junit.Test;
import vm.Claus;
import vm.Util;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static vm.Util.*;

public class MMTest {

    private MM mm;
    private Claus vm;

    @Before
    public void setup() {
        mm = new MM(1024, 1024, 1024);
        vm = new Claus(mm);
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
        mm.push(vm.newInteger(int2bytes(1)));
        mm.push(vm.newString(str2bytes("two")));
        mm.push(vm.newInteger(int2bytes(3)));

        assertEquals(3, bytes2int(mm.pop().$b().bytes()));
        assertEquals("two", bytes2str(mm.pop().$b().bytes()));
        assertEquals(1, bytes2int(mm.pop().$b().bytes()));
    }

    @Test(expected = RuntimeException.class)
    public void stackOverflow() {
        MM mm = new MM(1024, 1024, 2);
        Claus vm = new Claus(mm);

        mm.push(vm.newInteger(int2bytes(1)));
        mm.push(vm.newString(str2bytes("two")));
        mm.push(vm.newInteger(int2bytes(3)));
    }

    @Test(expected = RuntimeException.class)
    public void stackUnderflow() {
        mm.push(vm.newInteger(int2bytes(1)));

        mm.pop();
        mm.pop();
    }

}
