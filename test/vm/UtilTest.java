package vm;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UtilTest {

    @Test
    public void bytes2str() {
        String str = "test";
        assertEquals(str, Util.bytes2str(str.getBytes()));
    }

    @Test
    public void str2bytes() {
        String str = "test";
        byte[] bytes = str.getBytes();
        assertArrayEquals(bytes, Util.str2bytes(str));
    }

    @Test
    public void int2bytes() {
        int num = 123;
        byte[] bytes = new byte[]{0x0, 0x0, 0x0, 0x7B};
        assertArrayEquals(bytes, Util.int2bytes(num));
    }

    @Test
    public void bytes2int() {
        int num = 123;
        byte[] bytes = new byte[]{0x0, 0x0, 0x0, 0x7B};
        assertEquals(num, Util.bytes2int(bytes));
    }

    @Test
    public void translateBytecode() {
        byte[] output = Util.translateBytecode(new String[]{
                "push-ref 100",
                "call 108"
        });

        byte[] expected = new byte[]{Bytecode.strings2bytecodes.get("push-ref").code, 0, 0, 0, 100, Bytecode.strings2bytecodes.get("call").code, 0, 0, 0, 108};

        assertArrayEquals(expected, output);
    }

}
