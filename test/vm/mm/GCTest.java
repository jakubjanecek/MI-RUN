package vm.mm;

import org.junit.Test;
import vm.ClausVM;
import vm.Syscalls;
import vm.Util;

import java.util.Iterator;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static vm.Util.bytes2str;
import static vm.Util.str2bytes;

public class GCTest {

    @Test
    public void testRootset() {
        MM mm = new MM(1024, 1024, 1024);

        mm.pushInt(-1); // caller
        mm.pushInt(0); // return address

        Pointer obj = new Pointer(100, mm);
        obj.$().marker(MM.MARKER);
        mm.pushPointer(obj);

        obj = new Pointer(200, mm);
        obj.$().marker(MM.MARKER);
        mm.pushPointer(obj);

        mm.newFrame(2);

        obj = new Pointer(300, mm);
        obj.$().marker(MM.MARKER);
        mm.pushPointer(obj);

        obj = new Pointer(400, mm);
        obj.$().marker(MM.MARKER);
        mm.local(1, obj);

        mm.local(0, mm.arg(0));

        Set<Pointer> rootSet = mm.scanStack();

        assertEquals(4, rootSet.size());
        Iterator<Pointer> it = rootSet.iterator();
        assertEquals(100, it.next().address);
        assertEquals(200, it.next().address);
        assertEquals(400, it.next().address);
        assertEquals(300, it.next().address);

        mm.discardFrame();

        rootSet = mm.scanStack();
        assertEquals(2, rootSet.size());
        it = rootSet.iterator();
        assertEquals(100, it.next().address);
        assertEquals(200, it.next().address);
    }

    @Test
    public void testBaker01() {
        // small heap
        MM mm = new MM(1024, 40, 1024);

        Pointer root1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        root1.$().marker(MM.MARKER);
        root1.$().kind(ObjectKind.POINTER_INDEXED);
        root1.$().gcState(GCState.NORMAL);
        root1.$().size(0);
        mm.pushPointer(root1);

        try {
            mm.alloc(10);
        } catch (RuntimeException ex) {
            // expected
        }

        root1 = mm.popPointer();

        assertEquals(MM.MARKER, root1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, root1.$().kind());
        assertEquals(GCState.NORMAL, root1.$().gcState());
        assertEquals(0, root1.$().size());
    }

    @Test
    public void testBaker02() {
        // small heap
        MM mm = new MM(1024, 60, 1024);

        Pointer root1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        root1.$().marker(MM.MARKER);
        root1.$().kind(ObjectKind.POINTER_INDEXED);
        root1.$().gcState(GCState.NORMAL);
        root1.$().size(0);
        mm.pushPointer(root1);

        Pointer root2 = mm.alloc(mm.byteIndexedObjectSize(4));
        root2.$().marker(MM.MARKER);
        root2.$().kind(ObjectKind.BYTE_INDEXED);
        root2.$().gcState(GCState.NORMAL);
        root2.$().size(4);
        root2.$b().bytes("test".getBytes());
        mm.pushPointer(root2);

        try {
            mm.alloc(10);
        } catch (RuntimeException ex) {
            // expected
        }

        root2 = mm.popPointer();
        root1 = mm.popPointer();

        assertEquals(MM.MARKER, root1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, root1.$().kind());
        assertEquals(GCState.NORMAL, root1.$().gcState());
        assertEquals(0, root1.$().size());

        assertEquals(MM.MARKER, root2.$().marker());
        assertEquals(ObjectKind.BYTE_INDEXED, root2.$().kind());
        assertEquals(GCState.NORMAL, root2.$().gcState());
        assertEquals(4, root2.$().size());
    }

    @Test
    public void testBaker03() {
        // small heap
        MM mm = new MM(1024, 80, 1024);

        Pointer root1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        root1.$().marker(MM.MARKER);
        root1.$().kind(ObjectKind.POINTER_INDEXED);
        root1.$().gcState(GCState.NORMAL);
        root1.$().size(0);
        mm.pushPointer(root1);

        Pointer root2 = mm.alloc(mm.byteIndexedObjectSize(4));
        root2.$().marker(MM.MARKER);
        root2.$().kind(ObjectKind.BYTE_INDEXED);
        root2.$().gcState(GCState.NORMAL);
        root2.$().size(4);
        root2.$b().bytes("test".getBytes());
        mm.pushPointer(root2);

        // garbage
        mm.alloc(10);

        Pointer obj1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        obj1.$().marker(MM.MARKER);
        obj1.$().kind(ObjectKind.POINTER_INDEXED);
        obj1.$().gcState(GCState.NORMAL);
        obj1.$().size(0);

        root2 = mm.popPointer();
        root1 = mm.popPointer();

        assertEquals(MM.MARKER, root1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, root1.$().kind());
        assertEquals(GCState.NORMAL, root1.$().gcState());
        assertEquals(0, root1.$().size());

        assertEquals(MM.MARKER, root2.$().marker());
        assertEquals(ObjectKind.BYTE_INDEXED, root2.$().kind());
        assertEquals(GCState.NORMAL, root2.$().gcState());
        assertEquals(4, root2.$().size());

        assertEquals(MM.MARKER, obj1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, obj1.$().kind());
        assertEquals(GCState.NORMAL, obj1.$().gcState());
        assertEquals(0, obj1.$().size());
    }

    @Test
    public void testBaker04() {
        // small heap
        MM mm = new MM(1024, 90, 1024);

        Pointer root1 = mm.alloc(mm.pointerIndexedObjectSize(1));
        root1.$().marker(MM.MARKER);
        root1.$().kind(ObjectKind.POINTER_INDEXED);
        root1.$().gcState(GCState.NORMAL);
        root1.$().size(1);
        root1.$p().field(0, mm.NULL);
        mm.pushPointer(root1);

        Pointer root2 = mm.alloc(mm.byteIndexedObjectSize(4));
        root2.$().marker(MM.MARKER);
        root2.$().kind(ObjectKind.BYTE_INDEXED);
        root2.$().gcState(GCState.NORMAL);
        root2.$().size(4);
        root2.$b().bytes("test".getBytes());
        mm.pushPointer(root2);

        // garbage
        Pointer a = mm.alloc(mm.pointerIndexedObjectSize(0));
        a.$().marker(MM.MARKER);
        a.$().kind(ObjectKind.POINTER_INDEXED);
        a.$().gcState(GCState.NORMAL);
        a.$().size(0);

        Pointer obj1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        obj1.$().marker(MM.MARKER);
        obj1.$().kind(ObjectKind.POINTER_INDEXED);
        obj1.$().gcState(GCState.NORMAL);
        obj1.$().size(0);

        root2 = mm.popPointer();
        root1 = mm.popPointer();

        root1.$p().field(0, obj1);

        assertEquals(MM.MARKER, root1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, root1.$().kind());
        assertEquals(GCState.NORMAL, root1.$().gcState());
        assertEquals(1, root1.$().size());

        assertEquals(MM.MARKER, root2.$().marker());
        assertEquals(ObjectKind.BYTE_INDEXED, root2.$().kind());
        assertEquals(GCState.NORMAL, root2.$().gcState());
        assertEquals(4, root2.$().size());

        assertEquals(MM.MARKER, obj1.$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, obj1.$().kind());
        assertEquals(GCState.NORMAL, obj1.$().gcState());
        assertEquals(0, obj1.$().size());

        assertEquals(MM.MARKER, root1.$p().field(0).$().marker());
        assertEquals(ObjectKind.POINTER_INDEXED, root1.$p().field(0).$().kind());
        assertEquals(GCState.NORMAL, root1.$p().field(0).$().gcState());
        assertEquals(0, root1.$p().field(0).$().size());
    }

    @Test
    public void testBaker05() {
        // small heap
        MM mm = new MM(1024, 102, 1024);

        Pointer root1 = mm.alloc(mm.pointerIndexedObjectSize(0));
        root1.$().marker(MM.MARKER);
        root1.$().kind(ObjectKind.POINTER_INDEXED);
        root1.$().gcState(GCState.NORMAL);
        root1.$().size(0);
        mm.pushPointer(root1);

        Pointer root2 = mm.alloc(mm.pointerIndexedObjectSize(1));
        root2.$().marker(MM.MARKER);
        root2.$().kind(ObjectKind.POINTER_INDEXED);
        root2.$().gcState(GCState.NORMAL);
        root2.$().size(1);
        root2.$p().field(0, mm.NULL);
        mm.pushPointer(root2);

        Pointer obj1 = mm.alloc(mm.byteIndexedObjectSize("test".getBytes().length));
        obj1.$().marker(MM.MARKER);
        obj1.$().kind(ObjectKind.BYTE_INDEXED);
        obj1.$().gcState(GCState.NORMAL);
        obj1.$b().size("test".getBytes().length);
        obj1.$b().bytes("test".getBytes());

        root2.$p().field(0, obj1);

        mm.alloc(10);
        mm.alloc(5);

        root2 = mm.popPointer();

        assertEquals("test", bytes2str(root2.$p().field(0).$b().bytes()));
    }

    @Test
    public void testGC() {
        MM mm = new MM(1024, 800, 1024);
        ClausVM vm = new ClausVM(mm);

        String[] entryPointBC = new String[]{
                "push-ref " + vm.newString(str2bytes("GC test")).address,
                "syscall " + Syscalls.calls2ints.get("print"),

                "new-arr " + mm.addConstant(5),
                "pop-ref",
                "new-arr " + mm.addConstant(5),

                "return"
        };

        CodePointer entryPoint = mm.storeCode(Util.translateBytecode(entryPointBC));
        vm.run(entryPoint);
    }

}
