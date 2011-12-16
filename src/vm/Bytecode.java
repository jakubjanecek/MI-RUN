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
            put("push-ref", new BytecodeInstruction(int2Byte(0x04), 1));
            put("pop-ref", new BytecodeInstruction(int2Byte(0x05), 0));
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
