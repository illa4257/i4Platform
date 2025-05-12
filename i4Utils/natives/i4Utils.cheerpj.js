async function Java_illa4257_i4Utils_web_CheerpJClientFactory_fetch0(
    lib, req
) {
    let cfg = {
        method: req.method.toUpperCase(),
        headers: {},
        body: null
    };

    let iter = await (await req.clientHeaders.entrySet()).iterator(), item;
    while (await iter.hasNext()) {
        item = await iter.next();
        cfg.headers[await item.getKey()] = await item.getValue();
    }

    req.outputStream = await new (await lib.illa4257.i4Utils.io.NullOutputStream);

    if (req.hasContent)
        if (cfg.method != "GET" && cfg.method != "HEAD")
            if (req.bodyOutput != null)
                cfg.body = req.bodyOutput;
            else {
                let streamController;
                cfg.duplex = "half";
                cfg.body = new ReadableStream({
                    start(controller) {
                        streamController = controller;
                    }
                }).pipeThrough(new TextEncoderStream());

                req.outputStream = await new (await lib.illa4257.i4Utils.integration.CheerpJControllerReader)(streamController);
            }
        else
            console.warn("GET/HEAD requests cannot have body.");

    return cfg;
}

async function Java_illa4257_i4Utils_web_CheerpJClientFactory_fetch1(
    lib, req, cfg
) {
    let f = await fetch(await req.uri.toString(), cfg);
    req.responseCode = f.status;
    req.responseStatus = f.status == 200 ? "OK" : f.statusText;
    for (var e of f.headers.entries())
        await req.serverHeaders.put(e[0], e[1]);
    req.inputStream = await new (await lib.illa4257.i4Utils.integration.CheerpJBYOBReader)(await f.body.getReader({mode:"byob"}));
}

async function Java_illa4257_i4Utils_integration_CheerpJBYOBReader_read(lib, o, l) {
    let { done, value } = await o.reader.read(new Int8Array(l));
    if (done)
        return null;
    return value;
}

async function Java_illa4257_i4Utils_integration_CheerpJBYOBReader_close(lib, o, l) {
    await o.reader.cancel();
}

async function Java_illa4257_i4Utils_integration_CheerpJControllerReader_writeByte(lib, o, b) {
    await o.controller.enqueue(new Uint8Array([b]));
}

async function Java_illa4257_i4Utils_integration_CheerpJControllerReader_write(lib, o, arr, off, len) {
    await o.controller.enqueue(arr.subarray(off, off + len));
}

async function Java_illa4257_i4Utils_integration_CheerpJControllerReader_close(lib, o) {
    await o.controller.close();
}

export default {
    Java_illa4257_i4Utils_web_CheerpJClientFactory_fetch0,
    Java_illa4257_i4Utils_web_CheerpJClientFactory_fetch1,
    Java_illa4257_i4Utils_integration_CheerpJBYOBReader_read,
    Java_illa4257_i4Utils_integration_CheerpJBYOBReader_close,
    Java_illa4257_i4Utils_integration_CheerpJControllerReader_writeByte,
    Java_illa4257_i4Utils_integration_CheerpJControllerReader_write,
    Java_illa4257_i4Utils_integration_CheerpJControllerReader_close
}