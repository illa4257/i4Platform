class AsyncJavaEnv {
    constructor() {}

    alloc(cls) {
        return {cls:cls,lock:null,lock_owner:null,lock_n:0,waiters:[]};
    }

    getField(inst, name) {
        return inst['_' + name];
    }

    setField(inst, name, val) {
        inst['_' + name] = val;
    }

    async getClass(class_loader, cls) {
        return class_loader.loaded[cls]
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

console.log("rt.js loaded!");