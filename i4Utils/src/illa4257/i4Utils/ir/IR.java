package illa4257.i4Utils.ir;

import java.util.List;

public class IR {
    public static boolean isStackBased(final List<Inst> instructions) {
        for (final Inst inst : instructions)
            if (Opcode.STACK.contains(inst.opcode))
                return true;
            else
                for (final Object p : inst.params)
                    if (p == Const.STACK_1 || p == Const.STACK_2)
                        return true;
        return false;
    }
}