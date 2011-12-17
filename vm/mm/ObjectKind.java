package vm.mm;

public enum ObjectKind {

    POINTER_INDEXED((byte) 0x01), BYTE_INDEXED((byte) 0x02);
    public byte value;

    ObjectKind(byte value) {
        this.value = value;
    }

    public static ObjectKind fromValue(byte value) {
        for (ObjectKind kind : values()) {
            if (kind.value == value) {
                return kind;
            }
        }

        return POINTER_INDEXED;
    }
}
