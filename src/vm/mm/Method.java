package vm.mm;

public class Method {

    private String selector;

    private CodePointer bytecode;

    private int numOfLocals;

    public Method(String selector, CodePointer bytecode, int numOfLocals) {
        this.selector = selector;
        this.bytecode = bytecode;
        this.numOfLocals = numOfLocals;
    }

    public String selector() {
        return selector;
    }

    public CodePointer bytecodePointer() {
        return bytecode;
    }

    public int numOfLocals() {
        return numOfLocals;
    }

}
