package vm;

import vm.mm.MM;
import vm.mm.Pointer;

import java.io.PrintWriter;

public class Claus {

    public static void main(String... args) {
        MM mm = new MM(1024);

        Pointer clazz = mm.newClazz(null, null, "Car".getBytes());
        Pointer object = mm.newObject(clazz, 2);
        object.$p().field(0, mm.newString("test1".getBytes()));

        System.out.println(new String(object.$().clazz().$c().name().$b().bytes()));
        System.out.println(new String(object.$p().field(0).$b().bytes()));

        PrintWriter out = new PrintWriter(System.out);
        mm.dump(out);
        out.close();
    }
}
