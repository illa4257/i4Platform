package i4l;

import i4l.common.*;
import i4l.common.preprocessors.*;

import java.util.*;

public class I4LResolver {
    public Map<String, Object> defined = new HashMap<>();
    public ArrayList<String> errors = new ArrayList<>();

    private int ignore = 0;

    private boolean hadSuccess = false, success = false;

    public static boolean isInstance(final String type, final Object obj) {
        if (type.equals("Object"))
            return obj != null;
        return false;
    }

    public static boolean eq(final I4LMethod method, final I4LCall call) {
        if (!method.name.equals(call.target))
            return false;
        final int l = method.args.size();
        for (int i = 0; i < l; i++) {
            final I4LArg a = method.args.get(i);
            final String mt = a.params.get(a.params.size() - 2);

            final Object ct = call.args.get(i);
            if (!isInstance(mt, ct))
                return false;
        }
        return true;
    }

    public void unlink(final List<Object> code) {
        unlink(code, code);
    }

    public void unlink(final List<Object> root, final List<Object> code) {
        final ArrayList<Integer> r = new ArrayList<>();
        int i = 0;
        for (final Object o : code) {
            if (o instanceof I4LCode)
                unlink(root, ((I4LCode) o).code);
            if (o instanceof I4LMethod) {
                final I4LMethod m = (I4LMethod) o;
                if (!m.tags.contains("unlink"))
                    continue;
                m.tags.remove("unlink");
                unlink(root, m.args.size(), m);
                if (m.tags.contains("private"))
                    r.add(0, i);
            }
            i++;
        }
        for (final int l : r)
            code.remove(l);
    }

    private Object patch(final Object o, final HashMap<String, String> mapping) {
        if (o instanceof I4LCall) {
            final I4LCall c = new I4LCall();
            c.target = ((I4LCall) o).target;
            c.args = new ArrayList<>();
            for (final Object a : ((I4LCall) o).args)
                c.args.add(patch(a, mapping));
            return c;
        }
        if (o instanceof I4LGet) {
            if (mapping.containsKey(((I4LGet) o).path))
                return new I4LGet(mapping.get(((I4LGet) o).path));
            return o;
        }
        if (o instanceof I4LNewVar) {
            final I4LNewVar nv = new I4LNewVar();
            nv.params = ((I4LNewVar) o).params;
            for (final I4LNewVar.Var v : ((I4LNewVar) o).vars)
                if (mapping.containsKey(v.name))
                    nv.vars.add(new I4LNewVar.Var(mapping.get(v.name), v.value));
                else
                    nv.vars.add(v);
            return nv;
        }
        throw new RuntimeException("Unknown operation " + o);
    }

    private int ci = 0;

    public void unlink(final List<Object> code, final int argLen, final I4LMethod m) {
        int i = 0;
        final ArrayList<Integer> r = new ArrayList<>();
        for (final Object o : code) {
            if (o instanceof I4LCall && ((I4LCall) o).args.size() == argLen && eq(m, (I4LCall) o)) {
                System.out.println(o);
                r.add(0, i);
            }
            if (o instanceof I4LCode)
                unlink(((I4LCode) o).code, argLen, m);
            i++;
        }
        for (int ll : r) {
            int l = ll;
            final I4LCall c = (I4LCall) code.remove(l);
            final HashMap<String, String> mappings = new HashMap<>();
            int ia = 0;
            for (final I4LArg a : m.args) {
                final String t = a.params.get(a.params.size() - 2),
                            n = a.params.get(a.params.size() - 1);

                final String nn = "call_" + ci + "_" + n;
                mappings.put(n, nn);
                final I4LNewVar nv = new I4LNewVar();
                nv.params = Collections.singletonList(t);
                nv.vars.add(new I4LNewVar.Var(nn, c.args.get(ia++)));
                code.add(l, nv);
                l++;
            }
            for (final Object o : m.code)
                code.add(l++, patch(o, mappings));
            ci++;
        }
    }

    public void resolve(final ArrayList<Object> code) throws Exception {
        final ArrayList<Integer> r = new ArrayList<>();
        int i = 0;
        for (final Object o : code) {
            if (ignore == 0) {
                if (o instanceof I4LPDefine)
                    defined.put(((I4LPDefine) o).name, ((I4LPDefine) o).value);
                if (o instanceof I4LPError)
                    errors.add(((I4LPError) o).message);
            }

            if (o instanceof I4LPIf) {
                if (ignore == 0) {
                    success = hadSuccess = check(((I4LPIf) o).condition);
                    if (success)
                        ignore--;
                }
                ignore++;
            }

            if (o instanceof I4LPElse) {
                if (
                        success && ignore == 0 ||
                                !success && ignore == 1
                ) {
                    if (((I4LPElse) o).another != null) {
                        if (success) {
                            success = false;
                            ignore++;
                        } else if (!hadSuccess) {
                            success = hadSuccess = check(((I4LPElse) o).another);
                            if (success)
                                ignore--;
                        }
                    } else {
                        if (success) {
                            success = false;
                            ignore++;
                        } else if (!hadSuccess) {
                            success = hadSuccess = true;
                            ignore--;
                        }
                    }
                }
            }

            if (o instanceof I4LPEnd && !success)
                ignore--;

            if (o instanceof I4LCode)
                resolve(((I4LCode) o).code);
            if (ignore > 0 || o instanceof I4LPreprocessor)
                r.add(0, i);
            i++;
        }
        for (final int n : r)
            code.remove(n);
    }

    public boolean check(final I4LPreprocessor p) throws Exception {
        if (p instanceof I4LPIf)
            return check(((I4LPIf) p).condition);
        if (p instanceof I4LPDefined)
            return defined.containsKey(((I4LPDefined) p).def);
        throw new Exception("Unknown preprocessor " + p.getClass().getName());
    }
}