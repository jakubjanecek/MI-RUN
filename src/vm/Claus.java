package vm;

import vm.mm.MM;
import vm.mm.Method;
import vm.mm.ObjectKind;
import vm.mm.Pointer;

import java.io.PrintWriter;

import static vm.Util.bytes2str;
import static vm.Util.str2bytes;

public class Claus {

    private MM mm;

    private Pointer metaclass;

    private Pointer classOfObject;

    public void test() {
        Pointer clazz = newClazz(str2bytes("Car"), null);
        Pointer object = newObject(clazz, 2);
        object.$p().field(0, newString(str2bytes("test1")));

        System.out.println(bytes2str(object.$().clazz().$c().name().$b().bytes()));
        System.out.println(bytes2str(object.$p().field(0).$b().bytes()));

        PrintWriter out = new PrintWriter(System.out);
        mm.dump(out);
        out.close();
    }

    public static void main(String... args) {
        new Claus(new MM(1024, 1024));
    }

    public Claus(MM mm) {
        this.mm = mm;

        bootstrap();

        test();
    }

    public void bootstrap() {
        metaclass = newClazz(str2bytes("Metaclass"), null);
        classOfObject = newClazz(str2bytes("Object"), null);
        metaclass.$c().metaclass(metaclass);
        metaclass.$c().superclass(classOfObject);

        // TODO
        // prepare classes such as Integer, String, Array and so on
    }

    public Pointer newObject(Pointer clazz, int size) {
        Pointer newObject = mm.alloc(mm.pointerIndexedObjectSize(size));

        if (newObject == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newObject.$().kind(ObjectKind.POINTER_INDEXED);
        newObject.$().size(size);
        newObject.$().clazz(clazz);

        return newObject;
    }

    public Pointer newClazz(byte[] name, Pointer superclass) {
        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(3));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(3);

        if (metaclass != null) {
            newClass.$().clazz(metaclass);
        }

        if (superclass == null) {
            superclass = classOfObject;
        }
        newClass.$c().superclass(superclass);

        newClass.$c().name(newString(name));
        newClass.$c().methods(null);

        return newClass;
    }

    public Pointer newString(byte[] str) {
        Pointer newString = mm.alloc(mm.byteIndexedObjectSize(str.length));

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(null);
        newString.$b().bytes(str);

        return newString;
    }
//    obj* send0 ( obj* rec, char *selector ) {
//        method* m = lookup ( (class*)rec->class, selector );
//        if (m == NULL) error1("No method found (%s)\n", selector);
//        return m->code.f0(rec);
//    }

//    method* lookup ( class *search, char* selector) {
//        class* cls = search;
//        int i;
//        while ( cls ) {
//            for ( i = 0; i < obj_size ( cls->methods ) ; i++ ) {
//                if (obj_field_get(cls->methods, i) != NULL) {
//                    method* m = (method*)obj_field_get(cls->methods, i);
//                    if (strcmp(selector, obj_bytes(m->selector)) == 0) 
//                        return m;                
//                }
//            }
//            cls = cls->superclass;
//        }
//        return NULL;
//    }

    private void callMethod(Pointer obj, String selector) {
        Pointer objectClass = obj.$().clazz();
        Method method = methodLookup(objectClass, selector);

        if (method != null) {
            // TODO
            // call it
        } else {
            throw new RuntimeException("Method '" + selector + "' not found in class '" + bytes2str(objectClass.$c().name().$b().bytes()) + "'");
        }
    }

    private Method methodLookup(Pointer clazz, String selector) {
        Pointer current = clazz;
        while (current != null) {
            MM.Clazz c = current.$c();
            Pointer classMethods = c.methods();
            
            current = c.superclass();
        }

        return null;
    }


}
