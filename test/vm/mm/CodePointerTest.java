package vm.mm;

import org.junit.Test;

import static org.junit.Assert.*;

public class CodePointerTest {

    @Test
    public void equals() {
        CodePointer p1 = new CodePointer(123, null);
        CodePointer p2 = new CodePointer(123, null);
        CodePointer p3 = new CodePointer(321, null);

        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
        assertFalse(p1.equals(p3));
        assertFalse(p2.equals(p3));
        assertFalse(p3.equals(p1));
        assertFalse(p3.equals(p2));
    }

    @Test
    public void arithmetic() {
        CodePointer p1 = new CodePointer(123, null);

        assertEquals(123, p1.address);
        p1 = p1.arith(10);
        assertEquals(123 + 10, p1.address);
        p1 = p1.arith(-20);
        assertEquals(123 - 10, p1.address);
    }

}
