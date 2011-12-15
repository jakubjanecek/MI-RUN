package vm.mm;

import vm.Util;

/**
 * Represents a pointer into memory (as in C language).
 */
public class Pointer {

    public int address;

    private MM mm;

    public Pointer(int address, MM mm) {
        this.address = address;
        this.mm = mm;
    }

    /**
     * Pointer dereference.
     *
     * @return MM.Obj
     */
    public MM.Obj $() {
        checkNull();

        return mm.new Obj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.PointerIndexedObj
     */
    public MM.PointerIndexedObj $p() {
        checkNull();

        return mm.new PointerIndexedObj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.ByteIndexedObj
     */
    public MM.ByteIndexedObj $b() {
        checkNull();

        return mm.new ByteIndexedObj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.Clazz
     */
    public MM.Clazz $c() {
        checkNull();

        return mm.new Clazz(this);
    }

    public boolean isNull() {
        if (mm.NULL.equals(this)) {
            return true;
        }

        return false;
    }

    private void checkNull() {
        if (isNull()) {
            throw new RuntimeException("Dereferencing NULL pointer!");
        }
    }

    @Override
    public int hashCode() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pointer pointer = (Pointer) o;

        if (address != pointer.address) return false;

        return true;
    }

    @Override
    public String toString() {
        byte[] addressBytes = Util.int2bytes(address);
        return String.format("Pointer: %02X%02X%02X%02X    %d", addressBytes[3], addressBytes[2], addressBytes[1], addressBytes[0], address);
    }
}
