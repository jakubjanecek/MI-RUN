package vm.mm;

public class Method {

    private String selector;

    private CodePointer bytecode;

    public Method(String selector, CodePointer bytecode) {
        this.selector = selector;
        this.bytecode = bytecode;
    }

    public String selector() {
        return selector;
    }

    public CodePointer bytecodePointer() {
        return bytecode;
    }

}
