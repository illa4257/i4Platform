package illa4257.i4Utils.ir;

import java.util.ArrayList;
import java.util.Arrays;

public class IRMethod {
    public String name;
    public IRType type;
    public final ArrayList<IRAccess> access = new ArrayList<>();
    public final ArrayList<IRType> argumentsTypes = new ArrayList<>();
    public final ArrayList<Inst> instructions = new ArrayList<>();

    public void print() {
        System.out.println("[");
        for (final Inst inst : instructions)
            System.out.println("\t" + inst.opcode + ' ' + Arrays.toString(inst.params) + " >> " + inst.output);
        System.out.println("]");
    }
}