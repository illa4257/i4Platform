package illa4257.i4Utils.ir;

import java.util.ArrayList;
import java.util.HashMap;

public class IRPhi {
    public final int index;
    public final ArrayList<Object> vars = new ArrayList<>();

    public IRPhi(final int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "PHI" + vars;
    }

    public static void resolve(final ArrayList<Inst> instructions) {
        final ArrayList<Object> vars = new ArrayList<>();
        final HashMap<Object, ArrayList<Object>> defines = new HashMap<>();
        final ArrayList<ArrayList<Object>> catchers = new ArrayList<>();
        for (final Inst inst : instructions) {
            if (inst.opcode == Opcode.ANCHOR) {
                final ArrayList<Object> l = new ArrayList<>();
                defines.put(inst.params[0], l);
                catchers.add(l);
                break;
            }
            if (Opcode.STOPPERS.contains(inst.opcode)) {
                catchers.clear();
                break;
            }
        }
    }
}