package vm;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static String bytes2str(byte[] bytes) {
        return new String(bytes);
    }

    public static byte[] str2bytes(String str) {
        return str.getBytes();
    }

    public static byte[] int2bytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static int bytes2int(byte[] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }

    public static Byte int2Byte(int b) {
        return new Byte((byte) b);
    }

    public static byte[] translateBytecode(String[] lines) {
        List<Byte> output = new ArrayList<Byte>();

        for (String line : lines) {
            String[] parts = line.split(" ");
            Bytecode.BytecodeInstruction opcode = Bytecode.strings2bytecodes.get(parts[0]);

            output.add(opcode.code);

            for (int i = 1; i <= opcode.numOfArguments; i++) {
                String arg = parts[i];
                Integer intVal = Integer.valueOf(arg);
                for (byte b : int2bytes(intVal)) {
                    output.add(b);
                }
            }
        }

        Byte[] out1 = output.toArray(new Byte[0]);
        byte[] out2 = new byte[out1.length];
        for (int i = 0; i < out1.length; i++) {
            out2[i] = out1[i];
        }
        return out2;
    }

    public static void writeOutBC(String[] bc) {
        for (String l : bc) {
            System.out.println(l);
        }
    }

    public static void debug(String debugMsg) {
        boolean debug = false;
        if (debug) {
            System.out.println(debugMsg);
        }
    }

}
