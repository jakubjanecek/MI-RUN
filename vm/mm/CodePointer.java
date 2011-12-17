package vm.mm;

import vm.Util;

/**
 * Represents a pointer into code memory (as in C language).
 */
public class CodePointer {

    public int address;

    private MM mm;

    public CodePointer(int address, MM mm) {
        this.address = address;
        this.mm = mm;
    }

    /**
     * Emulates pointer arithmetic as in C.
     *
     * @param number of bytes to be added or subtracted from this CodePointer
     * @return pointer to a new address byte
     */
    public CodePointer arith(int number) {
        return new CodePointer(address + number, mm);
    }

    @Override
    public int hashCode() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodePointer pointer = (CodePointer) o;

        if (address != pointer.address) return false;

        return true;
    }

    @Override
    public String toString() {
        byte[] addressBytes = Util.int2bytes(address);
        return String.format("CodePointer: %02X%02X%02X%02X    %d", addressBytes[3], addressBytes[2], addressBytes[1], addressBytes[0], address);
    }
}
