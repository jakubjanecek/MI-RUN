package vm;

import java.util.HashMap;
import java.util.Map;

import static vm.Util.int2Byte;

public class Bytecode {

    public static Map<String, BytecodeInstruction> strings2bytecodes = new HashMap<String, BytecodeInstruction>() {
        {
            // instruction name     bytecode        numOfArgs
            put("syscall", new BytecodeInstruction(int2Byte(0x01), 1));
            put("call", new BytecodeInstruction(int2Byte(0x02), 1));
            put("return", new BytecodeInstruction(int2Byte(0x03), 0));
            put("return-top", new BytecodeInstruction(int2Byte(0x04), 0));
            put("new", new BytecodeInstruction(int2Byte(0x05), 1));
            put("get-field", new BytecodeInstruction(int2Byte(0x06), 0));
            put("set-field", new BytecodeInstruction(int2Byte(0x07), 0));
            put("push-ref", new BytecodeInstruction(int2Byte(0x08), 1));
            put("pop-ref", new BytecodeInstruction(int2Byte(0x09), 0));
            put("push-int", new BytecodeInstruction(int2Byte(0x0A), 1));
            put("pop-int", new BytecodeInstruction(int2Byte(0x0B), 0));
            put("push-local", new BytecodeInstruction(int2Byte(0x0C), 1));
            put("pop-local", new BytecodeInstruction(int2Byte(0x0D), 1));
            put("add-int", new BytecodeInstruction(int2Byte(0x0E), 0));
            put("sub-int", new BytecodeInstruction(int2Byte(0x0F), 0));
            put("mul-int", new BytecodeInstruction(int2Byte(0x10), 0));
            put("div-int", new BytecodeInstruction(int2Byte(0x11), 0));
            put("mod-int", new BytecodeInstruction(int2Byte(0x12), 0));
            put("push-arg", new BytecodeInstruction(int2Byte(0x13), 1));
            put("pop-arg", new BytecodeInstruction(int2Byte(0x14), 1));
            put("set-bytes", new BytecodeInstruction(int2Byte(0x15), 1));
            put("new-int", new BytecodeInstruction(int2Byte(0x16), 1));
            put("new-str", new BytecodeInstruction(int2Byte(0x17), 1));
            put("new-arr", new BytecodeInstruction(int2Byte(0x18), 1));
            // conditional jumps
            // labels??
        }
    };

    public static Map<BytecodeInstruction, String> bytecodes2strings = new HashMap<BytecodeInstruction, String>();
    public static Map<Byte, String> bytes2strings = new HashMap<Byte, String>();

    {
        for (Map.Entry<String, BytecodeInstruction> entry : strings2bytecodes.entrySet()) {
            bytecodes2strings.put(entry.getValue(), entry.getKey());
            bytes2strings.put(entry.getValue().code, entry.getKey());
        }
    }

    public static class BytecodeInstruction {

        public byte code;

        public int numOfArguments;

        public BytecodeInstruction(byte code, int numOfArguments) {
            this.code = code;
            this.numOfArguments = numOfArguments;
        }
    }

}
