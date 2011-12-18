package vm.mm;

public enum GCState {

    NORMAL((byte) 0xAA), COPIED((byte) 0xBB);
    public byte value;

    GCState(byte value) {
        this.value = value;
    }

    public static GCState fromValue(byte value) {
        for (GCState kind : values()) {
            if (kind.value == value) {
                return kind;
            }
        }

        return NORMAL;
    }

}
