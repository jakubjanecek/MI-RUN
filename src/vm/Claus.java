package vm;

import vm.mm.MM;
import vm.mm.Method;
import vm.mm.ObjectKind;
import vm.mm.Pointer;

import java.io.PrintWriter;
import java.util.List;

import static java.util.Arrays.asList;
import static vm.Util.*;

public class Claus {

    private MM mm;

    private Pointer metaclass;

    private Pointer classOfObject;

    private Pointer classOfString;

    private Pointer classOfInteger;

    public void test() {
        Pointer classCar = newClazz(str2bytes("Car"), null);
        Pointer methodDictionaryOfCar = newMethodDictionary(asList(new Method[]{newMethod("drive", new byte[0])}));
        classCar.$c().methods(methodDictionaryOfCar);

        Pointer objectCar = newObject(classCar, 2);
        objectCar.$p().field(0, newString(str2bytes("test1")));

        System.out.println(bytes2str(objectCar.$().clazz().$c().name().$b().bytes()));
        System.out.println(bytes2str(objectCar.$p().field(0).$b().bytes()));

        callMethod(objectCar, "getObjectID");
        callMethod(objectCar, "drive");

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
        metaclass = newClazz(str2bytes("Metaclass"));
        classOfObject = newClazz(str2bytes("Object"), mm.NULL);
        metaclass.$c().metaclass(metaclass);
        metaclass.$c().superclass(classOfObject);

        Pointer methodDictionaryOfObject = newMethodDictionary(asList(new Method[]{newMethod("getObjectID", new byte[0])}));
        classOfObject.$c().methods(methodDictionaryOfObject);

        classOfString = newClazz(str2bytes("String"));
        classOfInteger = newClazz(str2bytes("Integer"));

        // TODO
        // prepare classes such as Integer, String, Array and so on
        // necessary to add methdos to them
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

    public Pointer newClazz(byte[] name) {
        return newClazz(name, null);
    }

    public Pointer newClazz(byte[] name, Pointer superclass) {
        Pointer newClass = mm.alloc(mm.pointerIndexedObjectSize(4));

        if (newClass == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newClass.$().kind(ObjectKind.POINTER_INDEXED);
        newClass.$().size(4);

        if (metaclass != null) {
            newClass.$().clazz(metaclass);
        }

        if (superclass == null) {
            superclass = classOfObject;
        }
        newClass.$c().superclass(superclass);

        newClass.$c().name(newString(name));
        newClass.$c().methods(mm.NULL);

        return newClass;
    }

    public Method newMethod(String selector, byte[] bytecode) {
        return new Method(selector, bytecode);
    }

    public Pointer newMethodDictionary(List<Method> methods) {
        Pointer methodDictionary = newObject(classOfObject, methods.size());

        for (int i = 0; i < methods.size(); i++) {
            methodDictionary.$p().field(i, newInteger(int2bytes(mm.addMethod(methods.get(i)))));
        }

        return methodDictionary;
    }

    public Pointer newString(byte[] str) {
        Pointer newString = mm.alloc(mm.byteIndexedObjectSize(str.length));

        if (newString == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newString.$().kind(ObjectKind.BYTE_INDEXED);
        newString.$().size(str.length);
        newString.$().clazz(classOfString);
        newString.$b().bytes(str);

        return newString;
    }

    public Pointer newInteger(byte[] integer) {
        Pointer newInteger = mm.alloc(mm.byteIndexedObjectSize(MM.WORD_SIZE));

        if (newInteger == null) {
            throw new RuntimeException("Not enough memory!");
        }

        newInteger.$().kind(ObjectKind.BYTE_INDEXED);
        newInteger.$().size(MM.WORD_SIZE);
        newInteger.$().clazz(classOfInteger);
        newInteger.$b().bytes(integer);

        return newInteger;
    }

    private void interpret() {

    }

    private void callMethod(Pointer obj, String selector) {
        Pointer objectClass = obj.$().clazz();
        Method method = lookupMethod(objectClass, selector);

        if (method != null) {
            // TODO
            // call it
            System.out.println("Calling method '" + method.selector() + "'");
        } else {
            throw new RuntimeException("Method '" + selector + "' not found in class '" + bytes2str(objectClass.$c().name().$b().bytes()) + "'");
        }
    }

    private Method lookupMethod(Pointer clazz, String selector) {
        Pointer current = clazz;
        while (!current.isNull()) {
            MM.Clazz c = current.$c();
            Pointer classMethods = c.methods();

            if (!classMethods.isNull()) {
                MM.PointerIndexedObj methodDictionary = classMethods.$p();
                int size = methodDictionary.size();

                for (int i = 0; i < size; i++) {
                    Pointer methodPointer = methodDictionary.field(i);
                    int methodIndex = bytes2int(methodPointer.$b().bytes());

                    Method m = mm.method(methodIndex);

                    if (m.selector().equals(selector)) {
                        return m;
                    }
                }
            }

            current = c.superclass();
        }

        return null;
    }


}
