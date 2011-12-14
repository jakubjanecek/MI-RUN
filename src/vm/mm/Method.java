package vm.mm;

public class Method {

    private String selector;

    private byte[] bytecode;

    public Method(String selector, byte[] bytecode) {
        this.selector = selector;
        this.bytecode = bytecode;
    }

    public String selector() {
        return selector;
    }

    public byte[] bytecode() {
        return bytecode;
    }

}
