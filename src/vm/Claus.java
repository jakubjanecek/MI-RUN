package vm;

import vm.mm.MM;
import vm.mm.ObjectKind;
import vm.mm.Pointer;

import java.io.PrintWriter;

public class Claus {

    private MM mm;

    private static final byte MARKER = (byte) 0xF0;

    public static void main(String... args) {
        new Claus(new MM(1024));
    }

    public Claus(MM mm) {
        this.mm = mm;

        Pointer clazz = newClazz(null, null, "Car".getBytes());
        Pointer object = newObject(clazz, 2);
        object.$p().field(0, newString("test1".getBytes()));

        System.out.println(new String(object.$().clazz().$c().name().$b().bytes()));
        System.out.println(new String(object.$p().field(0).$b().bytes()));

        PrintWriter out = new PrintWriter(System.out);
        mm.dump(out);
        out.close();
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = mm.alloc(mm.pointerIndexedObjectSize(size));

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().marker(MARKER);
        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        return newObject;
    }

    public Pointer newClazz(Pointer metaclass, Pointer superclass, byte[] name) {
        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(3));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().marker(MARKER);
        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(3);
        newClass.$().clazz(metaclass);
        newClass.$c().name(newString(name));
        newClass.$c().superclazz(superclass);
        newClass.$c().methods(null);

        return newClass;
    }

    public Pointer newString(byte[] str) {
        Pointer newString = mm.alloc(mm.byteIndexedObjectSize(str.length));

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().marker(MARKER);
        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(null);
        newString.$b().bytes(str);

        return newString;
    }

}
