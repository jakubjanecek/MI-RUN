package vm;

import java.util.HashMap;
import java.util.Map;

import static vm.Util.int2Byte;

public class Bytecode {

    public static Map<String, Byte> strings2bytes = new HashMap<String, Byte>() {
        {
            put("syscall", int2Byte(0x01));
        }
    };

    public static Map<Byte, String> bytes2strings = new HashMap<Byte, String>();

    {
        for (Map.Entry<String, Byte> entry : strings2bytes.entrySet()) {
            bytes2strings.put(entry.getValue(), entry.getKey());
        }
    }

}
