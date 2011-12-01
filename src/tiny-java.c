#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <assert.h>

#define __error__( what )                           \
    {                                               \
        fprintf(stderr, "ERROR (%s:%d) : ",         \
                __FILE__, __LINE__);                \
        what;                                       \
        fflush(stderr);                             \
        abort();                                    \
    }

#define error0( msg ) __error__( fprintf (stderr, msg )  )
#define error1( msg, a1 ) __error__( fprintf (stderr, msg, a1 ) )
#define error2( msg, a1, a2 ) __error__( { fprintf (stderr, msg, a1, a2 ); } )

#define __dbg__( what )                             \
    {                                               \
        fprintf(stderr, "DEBUG (%s:%d) : ",         \
                __FILE__, __LINE__);                \
        what;                                       \
        fflush(stderr);                             \
    }

#define dbg0( msg ) __dbg__( fprintf (stderr, msg )  )
#define dbg1( msg, a1 ) __dbg__( fprintf (stderr, msg, a1 ) )
#define dbg2( msg, a1, a2 ) __dbg__( { fprintf (stderr, msg, a1, a2 ); } )
#define dbg3( msg, a1, a2, a3 ) __dbg__( { fprintf (stderr, msg, a1, a2, a3 ); } )

void dbg_break() {
}


typedef enum { 
    POINTER_INDEXED = 0x01,
    BYTE_INDEXED    = 0x02
} obj_kind; 

#define MARKER 0xAFFE

#define obj_header                                      \
    int marker;                                         \
    obj_kind kind;                                      \
    int  size;                                          \
    struct obj *class;                                  \


typedef struct obj {
    obj_header;
    union {
        struct obj *fields[0];
        char        bytes[0];
    } data;
} obj;

#define bytes_to_words(bytes)                                   \
    ( ( (bytes | 0x03) + 1 ) >> 2 )

#define obj_size( obj ) \
    ( (obj) == NULL ? 0 : (obj)->size )

#define obj_size_in_bytes( o )                                          \
    ( (o) == NULL ?                                                     \
      0 :                                                               \
      ((o)->kind == POINTER_INDEXED ?                                   \
       sizeof(obj) + (o)->size * sizeof(int*) :                         \
       sizeof(obj) + (bytes_to_words((o)->size + 1) * sizeof(int*))))   \

#define obj_byte_size( obj )

#define obj_field_get_unchecked( obj, index ) \
    ( (obj)->data.fields[index] )

#define obj_field_get( obj, index ) \
    ( (obj) == NULL ? NULL : obj_field_get_unchecked( obj, index ) )

#define obj_field_set( obj, index, value ) \
    ( (obj)->data.fields[index] = value )

#define obj_bytes( obj ) \
    ( (obj) == NULL ? NULL : (obj)->data.bytes )

typedef obj* (*obj_func0)(obj* this) ;
typedef obj* (*obj_func1)(obj* this, obj* a1) ;
typedef obj* (*obj_func2)(obj* this, obj* a1, obj* a2) ;
/* ... */


typedef struct method {
    obj_header;
    obj *selector;
    union {
        obj_func0 f0;
        obj_func1 f1;
        obj_func2 f2;
        /* ... */
    } code;
} method; 

typedef struct class {
    obj_header;
    obj *name;
    struct class *superclass;
    obj    *methods;
} class;




/* 
   Allocates a new object of given class, object kind
   (pointer-indexed of byte-indexed) and size. 
   for pointer-indexed objects size means number of
   fields. For byte-indexed objects size means length
   of byte array
*/

obj* new_obj(class *cls, obj_kind kind, int size) {
    int size_in_words = 0;
    obj* o;
        
    if (kind == POINTER_INDEXED) {
        size_in_words += size;
    } else {
        size_in_words += bytes_to_words(size + 1); 
    }
    o = (obj*) malloc(sizeof ( obj ) + size_in_words * sizeof(int*));
    /* nil it out */
    bzero( (void*) o, sizeof ( obj ) + size_in_words * sizeof(int*) );
    o->marker = MARKER;
    o->kind = kind;
    o->size = size;
    o->class = (obj*)cls;   
    return o;
}

obj* new_string(class *cls, char *bytes) {
    obj* o = new_obj( cls, BYTE_INDEXED, strlen ( bytes ) );
    strcpy( o->data.bytes , bytes );
    return o;
}

/* 
   Instantiates a class object with given metaclass, 
   name and superclass.
*/
class* new_class(class *metaclass, char* name, class *superclass) {
    class* c = (class*) new_obj(metaclass, POINTER_INDEXED, 3);
    obj * name_o = new_string(NULL/* TODO */,  name);
    c->name = name_o;
    c->superclass = superclass;
    c->methods = NULL;
    return c;
}

/* 
   Defines a method with given selector in given class. obj_func0
   should point to a method's machine code.
*/
method* def_method(class *cls, char *selector, int n_args, obj_func0 f) {
    int i;
    obj *sel;
    method* m = (method*) new_obj(NULL/* TODO */, POINTER_INDEXED, 2);
    sel = new_string(NULL/* TODO */,  selector);
    m->selector = sel;
    switch (n_args) {
    case 0: 
        m->code.f0 = (obj_func0)f;
        break;
    case 1:
        m->code.f1 = (obj_func1)f;
        break;
    case 2:
        m->code.f2 = (obj_func2)f;
        break;
    default:
        error1("Only method with less than 2 args are supported (%d given)\n", 
               n_args);
        break;
    }
    if (cls->methods == NULL) {
        obj* ms = new_obj(NULL/* TODO */, POINTER_INDEXED, 2);
        cls->methods = ms;
    }
     
    while ( 1 ) {
        for (i = 0 ; i < obj_size( cls->methods ) ; i++) {
            if (obj_field_get(cls->methods, i) == NULL) {
                obj_field_set(cls->methods, i, (obj*) m);
                return m;
            }      
        }
        obj *methods_new = new_obj(NULL/* TODO */, POINTER_INDEXED, obj_size(cls->methods)*2);
        for (i = 0 ; i < obj_size ( cls->methods ) ; i++)
            obj_field_set(methods_new, i, obj_field_get(cls->methods, i));
        cls->methods = methods_new;
    }    
}

method* lookup ( class *search, char* selector) {
    class* cls = search;
    int i;
    while ( cls ) {
        for ( i = 0; i < obj_size ( cls->methods ) ; i++ ) {
            if (obj_field_get(cls->methods, i) != NULL) {
                method* m = (method*)obj_field_get(cls->methods, i);
                if (strcmp(selector, obj_bytes(m->selector)) == 0) 
                    return m;                
            }
        }
        cls = cls->superclass;
    }
    return NULL;
}

obj* send0 ( obj* rec, char *selector ) {
    method* m = lookup ( (class*)rec->class, selector );
    if (m == NULL) error1("No method found (%s)\n", selector);
    return m->code.f0(rec);    
}

#undef  SEND0
#define SEND0 send0

#ifdef USE_ILC
#   undef  SEND0
#   define SEND0 SEND0_ILC
#   error "SEND0_ILC not defined"
#else
#   undef  SEND0
#   define SEND0 send0
#endif

static obj* expected;

obj* A__foo(obj* this) {
    return expected;
    //return NULL;
}

obj* B__foo(obj* this) {
    return NULL;
}


int main ( int argc, char **argv ) {
    int i;

#ifdef USE_ILC
    printf("USE_ILC\n");
#else
    printf("DO NOT USE ILC\n");
#endif
    
    expected    = new_obj(NULL, POINTER_INDEXED, 0);


    class  *A   = new_class(NULL, "A", NULL);
    method *m   = def_method(A, "foo", 0, &A__foo);    
    obj    *a   = new_obj(A, POINTER_INDEXED, 0);


    class  *B   = new_class(NULL, "B", A);

            m   = def_method(B, "foo", 0, &B__foo);
    obj    *b   = new_obj(B, POINTER_INDEXED, 0);

    
    obj* a_foo = SEND0 ( a , "foo" );
    assert(a_foo == expected);

    obj* b_foo = SEND0 ( b , "foo" );
    assert(b_foo == NULL);


    for (i = 0; i < (1000 * 1000 * 100); i++) {
        obj *r = SEND0 ( a, "foo" );
        assert (r ==  expected);
    } 

    return 0;
}

/* 
   Homeworks:
   1) Change the code above to use VMTs instead of
      method dictionaries. 
   2) Implement a multiple inheritance.
   3) Extend ILC to use polymorphic inline cache 
      iff ILC fails. Measure its impact on performance.
*/
