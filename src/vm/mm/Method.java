package vm.mm;

public class Method {

    private String selector;

    private byte[] bytecode;

    public Method(String selector) {
        this.selector = selector;
//        bytecode = new ArrayList<String>();
    }

    public String selector() {
        return selector;
    }

    public byte[] bytecode() {
        return null;
//        return bytecode;
    }

}
