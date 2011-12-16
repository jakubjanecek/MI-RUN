package vm;

import java.util.HashMap;
import java.util.Map;

import static vm.Util.int2Byte;

public class Bytecode {

    public static Map<String, BytecodeInstruction> strings2bytecodes = new HashMap<String, BytecodeInstruction>() {
        {
            put("syscall", new BytecodeInstruction(int2Byte(0x01), 1));
        }
    };

    public static Map<BytecodeInstruction, String> bytecodess2strings = new HashMap<BytecodeInstruction, String>();

    {
        for (Map.Entry<String, BytecodeInstruction> entry : strings2bytecodes.entrySet()) {
            bytecodess2strings.put(entry.getValue(), entry.getKey());
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
