package vm;

import java.util.HashMap;
import java.util.Map;

public class Syscalls {

    public static Map<Integer, Claus.Syscall> ints2calls = new HashMap<Integer, Claus.Syscall>();

    public static Map<String, Integer> calls2ints = new HashMap<String, Integer>();

    public static void generateReversedTable() {
        for (Map.Entry<Integer, Claus.Syscall> entry : ints2calls.entrySet()) {
            calls2ints.put(entry.getValue().name, entry.getKey());
        }
    }

}
