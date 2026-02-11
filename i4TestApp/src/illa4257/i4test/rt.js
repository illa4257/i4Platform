async function divByZero(class_loader, env, t) {
    let c = await env.getClass(class_loader, "java/lang/ArithmeticException"),
       ex = await env.alloc(c);
    await c["<init>(java/lang/String)void"](c, env, t, "/ by zero");
    throw ex;
}

async function addClass(class_loader, env, t, cls) {
    class_loader.loaded[cls.name] = cls;
    cls.class_loader = class_loader;
    if (cls.super_cls !== undefined)
        cls.super_cls = await env.getClass(class_loader, t, cls.super_cls);
}

function instanceOf(instance,cls) {
    return true;
}

function virtualMethod(cls,name) {
    while (cls[name] === undefined)
        cls = cls.super_cls;
    return cls;
}

async function Java_str(class_loader,env,t,str) {
    const c = await env.getClass(class_loader, t, "java/lang/String"),
        r = await env.alloc(c),
        len = str.length,
        arr=await env.newArr(len);
    for (let i = 0; i < len; i++)
        await env.arrSet(arr, i, str.charCodeAt(i));
    await c["<init>(char[)void"](c,env,t,r,arr);
    return r;
}

class AsyncJavaEnv {
    constructor() {}

    alloc(cls,m={}) {
        m.cls = cls;
        if (cls.super_cls !== undefined) {
            if (m.super_instance === undefined)
                m.super_instance = {};
            m.super_instance = this.alloc(cls.super_cls, m.super_instance);
        }
        m.lock = null;
        m.lock_owner = null;
        m.lock_n = 0;
        m.waiters = [];
        return m;
    }

    getField(inst, name) {
        return inst['_' + name];
    }

    setField(inst, name, val) {
        inst['_' + name] = val;
    }

    async getClass(class_loader, t, cls) {
        let c = class_loader.loaded[cls];
        if (c === undefined) throw new Error("No class " + cls);
        if (!c.init) {
            c.init = true;
            if (c["<clinit>()void"] !== undefined)
                await c["<clinit>()void"](c, this, t);
        }
        return c;
    }

    arrLen(arr) {
        return arr.length;
    }

    arrGet(arr, index) {
        return arr[index];
    }

    arrSet(arr, index, val) {
        arr[index] = val;
    }

    newArr(len) {
        return new Array(len);
    }

    async callNative(cls, env, t, methodName, ...args) {
        let method1 = 'Java_' + cls.name.replaceAll(new RegExp("[/,()]","g"),"_") + "_" + methodName.replaceAll(new RegExp("[/,()]","g"),"_"),
            method2 = window[method1];
        if (method2 === undefined)
            throw new Error("Missing Native Method: " + method1 + "(cls, env, thread, ...args)");
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
        if (instance.lock === null || instance.lock.lock_owner !== t)
            throw new Error("Illegal monitor state");
        if (--instance.lock_n === 0) {
            const l = instance.lock;
            instance.lock_owner = instance.lock = null;
            l.resolve();
        }
    }
}

// natives
function Java_java_lang_Object_registerNatives__void(cls, env, thread, ...args) {}
function Java_java_lang_ClassLoader_registerNatives__void(cls, env, thread, ...args) {}
function Java_java_lang_System_registerNatives__void(cls, env, thread, ...args) {}
function Java_sun_misc_Unsafe_registerNatives__void(cls, env, thread, ...args) {}

console.log("rt.js loaded!");