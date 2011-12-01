package vm;

import vm.mm.MemoryManager;

public class Claus {

    public static void main(String... args) {
        MemoryManager mm = new MemoryManager(1024);

        MemoryManager.Clazz clazz = (MemoryManager.Clazz) mm.newClazz(null, null, "Car".getBytes());
        MemoryManager.Obj object = mm.newObject(clazz, 2);

        System.out.println(new String(((MemoryManager.ByteIndexedObj) object.clazz().name()).bytes()));
    }
}
