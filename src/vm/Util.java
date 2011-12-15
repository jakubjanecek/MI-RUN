package vm;

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
}
