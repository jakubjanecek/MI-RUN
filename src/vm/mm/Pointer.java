package vm.mm;

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
        return mm.new Obj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.PointerIndexedObj
     */
    public MM.PointerIndexedObj $p() {
        return mm.new PointerIndexedObj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.ByteIndexedObj
     */
    public MM.ByteIndexedObj $b() {
        return mm.new ByteIndexedObj(this);
    }

    /**
     * Pointer dereference.
     *
     * @return MM.Clazz
     */
    public MM.Clazz $c() {
        return mm.new Clazz(this);
    }

}
