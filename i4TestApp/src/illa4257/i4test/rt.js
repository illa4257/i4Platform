function d2i(d) {
    if (isNaN(d)) return 0;
    if (d > 2147483647) return 2147483647;
    if (d < -2147483648) return -2147483648;
    return d | 0;
}

function d2l(d) {
    if (isNaN(d)) return 0n;
    if (d > 9223372036854775807) return 9223372036854775807n;
    if (d < -9223372036854775808) return  -9223372036854775808n;
    return BigInt(Math.trunc(d));
}

async function divByZero(class_loader, env, t) {
    let c = await env.getClass(class_loader, t, "java/lang/ArithmeticException"),
       ex = await env.alloc(c, t);
    await c["<init>__Ljava_lang_String_2V"](c, env, t, "/ by zero");
    throw ex;
}

function instanceOf(instance,cls) {
    if (instance == null)
        return 0;
    instance = instance.cls;
    while (instance !== undefined) {
        if (instance.name === cls)
            return true;
        instance = instance.super_cls;
    }
    return false;
}

function virtualMethod(cls,name) {
    while (cls[name] === undefined)
        cls = cls.super_cls;
    return cls;
}

async function Java_str(class_loader,env,t,str) {
    const c = await env.getClass(class_loader, t, "java/lang/String"),
        r = await env.alloc(c, t),
        len = str.length,
        arr=await env.newArr(len);
    for (let i = 0; i < len; i++)
        await env.arrSet(arr, i, str.charCodeAt(i));
    await c["<init>___3CZV"](c,env,t,r,arr,true);
    return r;
}

async function getClass(cls, env, t) {
    let result = await env.getField(cls, "class");
    if (result === undefined) {
        const c = await env.getClass(cls.class_loader, t, "java/lang/Class"),
            r = await env.alloc(c, t);
        await c["<init>__Ljava/lang/ClassLoader_2V"](c,env,t,r,await env.getField(cls.class_loader, 'isSystem')?null:cls.class_loader);
        await env.setField(cls, "class", r);
        await env.setField(r, "class", cls);
        return r;
    }
    return result;
}

class JavaUtilities {
    static async initEnv(class_loader, env) {
        class_loader.loaded['V'] = await env.addClass(class_loader, null, {
            name: 'void',
            isPrimitive: true
        });
        class_loader.loaded['Z'] = await env.addClass(class_loader, null, {
            name: 'boolean',
            isPrimitive: true
        });
        class_loader.loaded['B'] = await env.addClass(class_loader, null, {
            name: 'byte',
            isPrimitive: true
        });
        class_loader.loaded['S'] = await env.addClass(class_loader, null, {
            name: 'short',
            isPrimitive: true
        });
        class_loader.loaded['C'] = await env.addClass(class_loader, null, {
            name: 'char',
            isPrimitive: true
        });
        class_loader.loaded['I'] = await env.addClass(class_loader, null, {
            name: 'int',
            isPrimitive: true
        });
        class_loader.loaded['J'] = await env.addClass(class_loader, null, {
            name: 'long',
            isPrimitive: true
        });
        class_loader.loaded['F'] = await env.addClass(class_loader, null, {
            name: 'float',
            isPrimitive: true
        });
        class_loader.loaded['D'] = await env.addClass(class_loader, null, {
            name: 'double',
            isPrimitive: true
        });
    }

    static async javaStr(class_loader,env,t,str) {
        const c = await env.getClass(class_loader, t, "java/lang/String"),
            r = await env.alloc(c, t),
            len = str.length,
            arr=await env.newArr(len);
        for (let i = 0; i < len; i++)
            await env.arrSet(arr, i, str.charCodeAt(i));
        await c["<init>___3CZV"](c,env,t,r,arr,true);
        return r;
    }

    static async jsStr(env, str) {
        let val = await env.getField(str, "value"),
            len = await env.arrLen(val),
            r = '';
        for (var i = 0; i < len; i++)
            r += String.fromCodePoint(await env.arrGet(val, i));
        return r;
    }
}

class StackTraceElement {
    constructor(cls, method) {
        this.cls = cls;
        this.method = method;
    }
}

class AsyncJavaEnv {
    constructor() {
        this.constantStringPool = {};
    }

    async alloc(cls, t, m={}) {
        m.cls = cls;
        if (cls.super_cls !== undefined) {
            if (m.super_instance === undefined)
                m.super_instance = {};
            m.super_instance = await this.alloc(cls.super_cls, t, m.super_instance);
        }
        m.lock = null;
        m.lock_owner = null;
        m.lock_n = 0;
        m.waiters = new Set();
        await env.preInitCls(cls.class_loader, this, t, m, cls.fields, false);
        return m;
    }
    
    async clone(thread, instance) {
        const c = await this.alloc(instance.cls, thread);
        
        throw new Error();
    }

    getField(inst, name) {
        return inst['_' + name];
    }

    setField(inst, name, val) {
        inst['_' + name] = val;
    }

    async getClass(class_loader, t, cls, init=true) {
        let c = class_loader.loaded[cls];
        if (c === undefined) {
            if (cls.startsWith('[')) {
                let lvl = 1;
                while (cls.charAt(lvl) === '[')
                    lvl++;
                await this.addClass(class_loader, t, c={
                    name: cls,
                    super_cls: "java/lang/Object",
                    fields: {},
                    staticFields: {},
                    consts: {}
                });
                await this.regClass(class_loader, t, c);
            }
            if (c === undefined) {
                console.error("No class", cls);
                const exC = await this.getClass(class_loader, t, 'java/lang/ClassNotFoundException', init);
                const ex = await this.alloc(exC, t);
                await exC['<init>__Ljava/lang/String_2V'](exC, this, t, ex, await JavaUtilities.javaStr(class_loader, this, t, "Class \"" + cls + "\" not found"));
                throw ex;
            }
        }
        if (init)
            if (!c.init) {
                const p = Promise.withResolvers();
                c.init = p.promise;
                t.initClasses.push(c.name);
                try {
                    if (c.fields !== undefined)
                        await env.preInitCls(class_loader, this, t, c, c.fields, true);
                    if (cls === 'java/lang/Class$Atomic')
                        console.log(c["<clinit>__V"] !== undefined);
                    if (c["<clinit>__V"] !== undefined)
                        await c["<clinit>__V"](c, this, t);
                } catch (e) {
                    p.reject(e);
                    console.error("Failed to init", cls, e)
                    throw e;
                } finally {
                    t.initClasses.splice(t.initClasses.indexOf(c.name), 1);
                }
                p.resolve(undefined);
            } else if (t.initClasses.indexOf(c.name) === -1)
                await c.init;
        return c;
    }

    async addClass(class_loader, t, cls) {
        class_loader.loaded[cls.name] = cls;
        cls.class_loader = class_loader;
        return cls;
    }

    async regClass(class_loader, t, cls) {
        if (cls.super_cls !== undefined)
            try {
                cls.super_cls = await this.getClass(class_loader, t, cls.super_cls, false);
            } catch (e) {
                console.error("Add class " + cls.super_cls + " for " + cls.name);
                throw e;
            }
        if (!cls.fields)
            cls.fields = {};
        if (!cls.methods)
            cls.methods = {};
        for (const [key, method] of Object.entries(cls.methods))
            cls[key] = method.code;
        const staticFieldOrder = [], objectFieldOrder = [];
        if (cls.super_cls) {
            staticFieldOrder.push(...cls.super_cls.staticFieldOrder);
            objectFieldOrder.push(...cls.super_cls.objectFieldOrder);
        }
        for (const [k, v] of Object.entries(cls.fields))
            ((v.flags & 0x0008) === 0 ? objectFieldOrder : staticFieldOrder).push(k);
        cls.staticFieldOrder = staticFieldOrder;
        cls.objectFieldOrder = objectFieldOrder;
    }

    arrLen(arr) {
        return arr.arr.length;
    }

    arrGet(arr, index) {
        return arr.arr[index];
    }

    arrSet(arr, index, val) {
        arr.arr[index] = val;
    }

    async newArr(len) {
        const arr = await this.alloc(await this.getClass(class_loader, null, "[Ljava/lang/Object", false), null);
        arr.arr = new Array(len);
        return arr;
    }

    async callNative(cls, env, t, methodName, encArgs, ...args) {
        let method1 = 'Java_' + cls.name.replaceAll(new RegExp("[/,()]","g"),"_") + "_" + methodName,
            method2 = window[method1];
        if (method2 === undefined) {
            let method3 = method1 + encArgs;
            method2 = window[method3];
            if (method2 === undefined)
                throw new Error("Missing Native Method: " + method1 + "(cls, env, thread, ...args) or " + method3 + "(cls, env, thread, ...args)");
        }
        return await method2(cls, env, t, ...args);
    }

    async monitorEnter(t, instance) {
        if (instance.lock !== null && instance.lock_owner === t) {
            instance.lock_n++;
            return;
        }
        while (instance.lock != null)
            await instance.lock.promise;
        instance.lock = Promise.withResolvers();
        instance.lock_owner = t;
        instance.lock_n = 1;
    }

    monitorExit(t, instance) {
        if (instance.lock === null || instance.lock_owner !== t)
            throw new Error("Illegal monitor state");
        if (--instance.lock_n === 0) {
            const l = instance.lock;
            instance.lock_owner = instance.lock = null;
            l.resolve();
        }
    }

    async monitorWait(t, instance, timeout) {
        if (instance.lock === null || instance.lock_owner !== t)
            throw new Error("Illegal monitor state");
        const w = Promise.withResolvers();
        instance.waiters.add(w);
        const n = instance.lock_n;
        let l = instance.lock;
        instance.lock_n = 0;
        instance.lock_owner = instance.lock = null;
        l.resolve();
        if (timeout > 0) {
            const timeout = setTimeout(() => w.resolve(undefined), Number(timeout));
            await w.promise;
            clearTimeout(timeout);
        } else
            await w.promise;
        instance.waiters.delete(w);
        await env.monitorEnter(t, instance);
        instance.lock_n = n;
    }

    initThread(thread) {
        thread.initClasses = [];
        thread.stack = [];
        console.log("new_thread", thread);
    }

    traceEnter(cls, thread, method) {
        thread.stack.push(cls, method);
    }

    traceExit(thread) {
        thread.stack.pop();
        thread.stack.pop();
    }

    traceDump(thread) {
        let trace = [];
        for (let i = 0; i < thread.stack.length; i++)
            trace.push(new StackTraceElement(thread.stack[i++], thread.stack[i]));
        return trace;
    }

    async preInitCls(class_loader, env, t, instance, fields, isStatic) {
        let c = isStatic ? (f => (f & 0x0008) !== 0) : (f => (f & 0x0008) === 0);
        for (const [key, field] of Object.entries(fields))
            if (c(field.flags))
                if (field.value != null)
                    instance['_' + key] = typeof(field.value) == 'string' ?
                            await JavaUtilities.javaStr(class_loader, env, t, field.value) : field.value;
                else
                    switch (field.type) {
                        case 'Z':
                            instance['_' + key] = false;
                            break;
                        case 'B':
                        case 'S':
                        case 'C':
                        case 'I':
                        case 'F':
                        case 'D':
                            instance['_' + key] = 0;
                            break;
                        case 'J':
                            instance['_' + key] = 0n;
                            break;
                        default:
                            //console.warn("Uknown type", field.type);
                            break;
                    }
    }
    
    async internJSString(class_loader, t, str) {
        let s = this.constantStringPool['_' + str];
        if (s)
            return s;
        const s2 = await JavaUtilities.javaStr(class_loader, this, t, str);
        s = this.constantStringPool['_' + str];
        if (s)
            return s;
        this.constantStringPool['_' + str] = s2;
        return s2;
    }
    
    async getJavaString(str) {
        return this.constantStringPool['_' + str];
    }
    
    async internJavaString(instance) {
        const str = '_' + await JavaUtilities.jsStr(this, instance);
        if (this.constantStringPool[str])
            return this.constantStringPool[str];
        this.constantStringPool[str] = instance;
        return instance;
    }
    
    async objectGetVolatile(obj, index) {
        return obj['_' + obj.cls.objectFieldOrder[index]];
    }
    
    async objectCompareAndSwapObject(obj, index, expected, newValue) {
        const n = '_' + obj.cls.objectFieldOrder[index];
        if (obj[n] === expected) {
            obj[n] = newValue;
            return 1;
        } else
            return 0;
    }
    
    async objectCompareAndSwapInt(obj, index, expected, newValue) {
        return await this.objectCompareAndSwapObject(obj, index, expected, newValue);
    }
}

// natives
function Java_java_lang_Object_registerNatives(cls, env, thread, ...args) {}
function Java_java_lang_ClassLoader_registerNatives(cls, env, thread, ...args) {}
function Java_java_lang_System_registerNatives(cls, env, thread, ...args) {}
function Java_sun_misc_Unsafe_registerNatives(cls, env, thread, ...args) {}
function Java_java_lang_Class_registerNatives(cls, env, thread, ...args) {}
function Java_java_lang_Thread_registerNatives(cls, env, thread, ...args) {}
function Java_java_io_FileInputStream_initIDs(cls, env, thread, ...args) {}
function Java_java_io_FileDescriptor_initIDs(cls, env, thread, ...args) {}
function Java_java_io_FileOutputStream_initIDs(cls, env, thread, ...args) {}

async function Java_java_lang_System_arraycopy(cls, env, thread, src, srcPos, dest, destPos, length) {
    const sd = srcPos + length;
    for (; srcPos < sd; srcPos++, destPos++)
        await env.arrSet(dest, destPos, await env.arrGet(src, srcPos));
}

async function Java_java_lang_Object_getClass(cls, env, thread, obj) {
    return await getClass(obj.cls, env, thread);
}

let counter = 1;
async function Java_java_lang_Object_hashCode(cls, env, thread, instance) {
    const result = await env.getField(instance, 'identityHashCode');
    if (result === undefined) {
        const c = (counter++) | 0;
        await env.setField(instance, "identityHashCode", c);
        return c;
    }
    return result;
}

async function Java_java_lang_Object_clone__Ljava_lang_Object_2(cls, env, thread, instance) {
    return await env.clone(thread, instance);
}

function Java_sun_misc_Unsafe_arrayBaseOffset(cls, env, thread, inst, c) { return 0; }

async function Java_sun_misc_Unsafe_arrayIndexScale(cls, env, thread, inst, c) {
    c = (await env.getField(c, "class")).name;
    let i = 0;
    while (c.charAt(i) === '[')
        i++;
    c = c.charAt(i);
    switch (c) {
        case 'Z': return 1;
        case 'B': return 1;
        case 'S': return 2;
        case 'C': return 2;
        case 'I': return 4;
        case 'J': return 8;
        case 'F': return 4;
        case 'D': return 8;
        case 'L': return 4;
        default: throw new Error("Unknown type: " + c);
    }
}

function Java_sun_misc_Unsafe_addressSize(cls, env, thread, ...args) { return 4; }

async function Java_java_lang_Class_getPrimitiveClass(cls, env, thread, str) {
    let t = await JavaUtilities.jsStr(env, str);
    switch (t) {
        case 'short': break;
        case 'char': break;
        case 'int': break;
        case 'long': break;
        case 'float': break;
        case 'double': break;
        default: throw new Error("Unknown type " + t);
    }
    return await getClass(await env.getClass(cls.class_loader, thread, t), env, thread);
}

async function Java_sun_reflect_Reflection_getCallerClass__Ljava_lang_Class_2(cls, env, thread) {
    let dump = await env.traceDump(thread);
    dump.pop();
    return await getClass(dump.pop().cls, env, thread);
}

async function Java_java_lang_Class_desiredAssertionStatus0(cls, env, thread, c) {
    return false;
}

const floatBuffer = new Float32Array(1), doubleBuffer = new Float64Array(1),
    intBuffer = new Int32Array(floatBuffer.buffer), longBuffer = new BigInt64Array(doubleBuffer.buffer);

function Java_java_lang_Float_floatToRawIntBits(cls, env, thread, f) {
    floatBuffer[0] = f;
    return intBuffer[0];
}
function Java_java_lang_Double_doubleToRawLongBits(cls, env, thread, d) {
    doubleBuffer[0] = d;
    return longBuffer[0];
}
function Java_java_lang_Double_longBitsToDouble(cls, env, thread, l) {
    longBuffer[0] = l;
    return doubleBuffer[0];
}

async function Java_sun_misc_VM_initialize(cls, env, thread, ...args) {}

async function Java_java_security_AccessController_doPrivileged(cls, env, thread, action) {
    return await action.cls['run__Ljava/lang/Object_2'](action.cls, env, thread, action);
}

async function Java_java_lang_System_initProperties(cls, env, thread, props) {
    const put = async (k,v) => {
        await props.cls['setProperty__Ljava/lang/String_2Ljava/lang/String_2Ljava/lang/Object_2'](
                props.cls, env, thread, props,
                await JavaUtilities.javaStr(cls.class_loader, env, thread, k),
                await JavaUtilities.javaStr(cls.class_loader, env, thread, v)
        );
    };
    await put('file.encoding', 'UTF-8');
    await put('file.separator', '/');
    await put('path.separator', ':');
    return props;
}

function Java_java_lang_Thread_currentThread(cls, env, thread) { return thread; }

async function Java_java_lang_Throwable_fillInStackTrace__ILjava_lang_Throwable_2(cls, env, thread, instance, dummy) {
    console.log(dummy);
    if (dummy !== 0)
        throw new Error("Dummy isn't zero");
    const d = await env.traceDump(thread);
    d.err = new Error();
    await env.setField(instance, 'backtrace', d);
    return instance;
}

function Java_java_security_AccessController_getStackAccessControlContext(cls, env, thread) {
    return null;
}

function Java_java_lang_Thread_setPriority0(cls, env, thread, ...args) {}

function Java_java_lang_Thread_isAlive(cls, env, thread, instance) {
    return false;
}

async function Java_java_lang_Class_getName0(cls, env, thread, instance) {
    return await JavaUtilities.javaStr(cls.class_loader, env, thread, cls.name.replaceAll('/', '.'));
}

async function Java_java_lang_Thread_start0(cls, env, thread, instance) {
    const c = await virtualMethod(instance.cls, 'run__V');
    await env.initThread(instance);
    c['run__V'](c, env, instance, instance);
}

async function Java_java_lang_Object_wait__JV(cls, env, thread, instance, timeout) {
    await env.monitorWait(thread, instance, timeout);
}

async function Java_sun_misc_Unsafe_objectFieldOffset__Ljava_lang_reflect_Field_2J(cls, env, thread, instance, field) {
    const n = await JavaUtilities.jsStr(env,
        await field.cls['getName__Ljava/lang/String_2'](field.cls, env, thread, field)
    );
    const index = (await env.getField(await env.getField(field, "clazz"), "class")).objectFieldOrder.indexOf(n);
    if (index === -1)
        throw new Error("The field not found in the object field order!");
    return index;
}

async function Java_sun_reflect_Reflection_getClassAccessFlags__Ljava_lang_Class_2I(cls, env, thread, target) {
    return (await env.getField(target, "class")).flags;
}

async function Java_java_lang_Class_isPrimitive__Z(cls, env, thread, target) {
    return (await env.getField(target, "class")).isPrimitive ? 1 : 0;
}

async function Java_java_lang_Class_isInterface__Z(cls, env, thread, target) {
    return ((await env.getField(target, "class")).flags & 0x0200) !== 0 ? 1 : 0;
}

async function Java_java_lang_Class_forName0__Ljava_lang_String_2ZLjava_lang_ClassLoader_2Ljava_lang_Class_2Ljava_lang_Class_2(cls, env, thread, name, init, loader, caller) {
    //console.log(await JavaUtilities.jsStr(env, name), init, loader, caller);
    if (!loader)
        loader = cls.class_loader;
    const c = await env.getClass(loader, thread,
        (await JavaUtilities.jsStr(env, name)).replaceAll('.', '/'), init);
    if (c)
        return await getClass(c, env, mainThread);
    //console.log(c);
    // class_loader.loaded['java/lang/ClassNotFoundException']
    throw new Error();
}

async function Java_java_lang_String_intern__Ljava_lang_String_2(cls, env, thread, instance) {
    return await env.internJavaString(instance);
}

async function Java_sun_misc_Unsafe_compareAndSwapObject__Ljava_lang_Object_2JLjava_lang_Object_2Ljava_lang_Object_2Z(cls, env, thread, instance, obj, fieldIndex, expected, newValue) {
    return await env.objectCompareAndSwapObject(obj, fieldIndex, expected, newValue);
}

async function Java_sun_misc_Unsafe_compareAndSwapInt__Ljava_lang_Object_2JIIZ(cls, env, thread, instance, obj, fieldIndex, expected, newValue) {
    return await env.objectCompareAndSwapInt(obj, fieldIndex, expected, newValue);
}

async function Java_sun_misc_Unsafe_getIntVolatile__Ljava_lang_Object_2JI(cls, env, thread, instance, obj, offset) {
    return await env.objectGetVolatile(obj, offset);
}

async function Java_java_lang_System_setIn0__Ljava_io_InputStream_2V(cls, env, thread, stream) {
    const c = await env.getClass(cls.class_loader, thread, "java/lang/System");
    await env.setField(c, "in", stream);
}

async function Java_java_lang_Class_getDeclaredFields0__Z_3Ljava_lang_reflect_Field_2(cls, env, thread, instance, publicOnly) {
    const c = await env.getClass(cls.class_loader, thread, "java/lang/reflect/Field"),
        filter = publicOnly ? f => (f.flags & 0x0001) === 0 : _f => false;
    const l = [];
    for (const [key, f] of Object.entries((await env.getField(instance, 'class')).fields)) {
        const field = await env.alloc(c, thread);
        if (filter(field))
            continue;
        c['<init>__Ljava/lang/Class_2Ljava/lang/String_2Ljava/lang/Class_2IILjava/lang/String_2_3BV'](c, env, thread,
            field, instance, await env.internJavaString(await JavaUtilities.javaStr(c.class_loader, env, thread, key)),
            await getClass(
                await env.getClass(c.class_loader, thread, f.type, false),
                env, thread),
            f.flags, -1, null, null);
        l.push(field);
    }
    const r = await env.newArr(l.length);
    for (const [i, v] of l.entries())
        await env.arrSet(r, i, v);
    return r;
}

async function Java_java_lang_Class_getDeclaredConstructors0__Z_3Ljava_lang_reflect_Constructor_2(cls, env, thread, instance, publicOnly) {
    const l = [], filter = publicOnly ? f => (f.flags & 0x0001) === 0 : _f => false,
        rc = await env.getClass(cls.class_loader, thread, "java/lang/reflect/Constructor");
    for (const [key, f] of Object.entries((await env.getField(instance, 'class')).methods)) {
        if (!key.startsWith("<init>") || filter(f.flags))
            continue;
        const constr = await env.alloc(rc, thread);
        await rc['<init>__Ljava/lang/Class_2_3Ljava/lang/Class_2_3Ljava/lang/Class_2IILjava/lang/String_2_3B_3BV'](rc, env, thread, constr, instance, await env.newArr(0), await env.newArr(0), f.flags, null, null);
        l.push(constr);
    }
    const r = await env.newArr(l.length);
    for (const [i, v] of l.entries())
        await env.arrSet(r, i, v);
    return r;
}

console.log("rt.js loaded!");