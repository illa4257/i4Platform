package illa4257.i4Utils.ir;

import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.logger.i4Logger;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

public class IROperandStack {
    private static class Slot {
        //public final ArrayList<Inst> insts = new ArrayList<>();
        public final Inst inst;

        public Slot(Inst inst) {
            this.inst = inst;
            //this.insts.add(inst);
        }

        @Override
        public String toString() {
            return "SLOT(" + inst + ")";
        }
    }

    public static void resolve(final ArrayList<Inst> instructions) {
        int tmp = 0;
        final ArrayList<Object> bits64 = new ArrayList<>();
        bits64.add(Const.STACK_2);
        final HashMap<Object, Stack<Slot>> anchors = new HashMap<>();
        final HashMap<Object, ArrayList<Stack<Slot>>> stackLater = new HashMap<>();
        //final Stack<Slot> stack = new Stack<>();
        final ArrayList<Stack<Slot>> stacks = new ArrayList<>();
        stacks.add(new Stack<>());
        int i = 0;
        try {
            for (; i < instructions.size(); i++) {
                final Inst inst = instructions.get(i);
                if (inst.opcode == Opcode.CHECK_CAST)
                    continue;
                if (inst.opcode == Opcode.ANCHOR) {
                    final ArrayList<Stack<Slot>> l = stackLater.remove(inst.params[0]);
                    if (l != null)
                        for (final Stack<Slot> s : l) {
                            if (!stacks.isEmpty() && stacks.get(0).size() != s.size())
                                throw new RuntimeException("Different sizes: " + stacks.get(0).size() + " vs " + s.size());
                            stacks.add(s);
                        }
                    final Stack<Slot> s = new Stack<>();
                    s.addAll(stacks.get(0));
                    anchors.put(inst.params[0], s);
                    continue;
                }
                if (inst.opcode == Opcode.DUP) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v = stack.pop();
                        stack.push(v);
                        stack.push(v);
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.DUP_x1) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v2 = stack.pop(), v1 = stack.pop();
                        stack.push(v2);
                        stack.push(v1);
                        stack.push(v2);
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.DUP_x2) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v1 = stack.pop(), v2 = stack.pop();
                        if (bits64.contains(v2)) {
                            stack.push(v1);
                            stack.push(v2);
                        } else {
                            final Slot v3 = stack.pop();
                            stack.push(v1);
                            stack.push(v3);
                            stack.push(v2);
                        }
                        stack.push(v1);
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.DUP2) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v1 = stack.pop();
                        if (bits64.contains(v1.inst.output))
                            stack.push(v1);
                        else {
                            final Slot v2 = stack.pop();
                            stack.push(v2);
                            stack.push(v1);
                            stack.push(v2);
                        }
                        stack.push(v1);
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.DUP2_x1) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v1 = stack.pop(), v2 = stack.pop();
                        if (bits64.contains(v1)) {
                            stack.push(v1);
                            if (bits64.contains(v2))
                                throw new RuntimeException("Illegal usage");
                            stack.push(v2);
                            stack.push(v1);
                        } else {
                            final Slot v3 = stack.pop();
                            if (bits64.contains(v2) || bits64.contains(v3))
                                throw new RuntimeException("Illegal usage");
                            stack.push(v2);
                            stack.push(v1);
                            stack.push(v3);
                            stack.push(v2);
                            stack.push(v1);
                        }
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.DUP2_x2) {
                    for (final Stack<Slot> stack : stacks) {
                        final Slot v1 = stack.pop(), v2 = stack.pop();
                        if (bits64.contains(v1)) {
                            if (bits64.contains(v2)) { // Form 4
                                stack.push(v1);
                                stack.push(v2);
                                stack.push(v1);
                            } else {
                                final Slot v3 = stack.pop();
                                if (bits64.contains(v3))
                                    throw new RuntimeException("Illegal usage");
                                // Form 2
                                stack.push(v1);
                                stack.push(v3);
                                stack.push(v2);
                                stack.push(v1);
                            }
                        } else if (bits64.contains(v2))
                            throw new RuntimeException("Illegal usage");
                        else {
                            final Slot v3 = stack.pop();
                            if (bits64.contains(v3)) { // Form 3
                                stack.push(v2);
                                stack.push(v1);
                                stack.push(v3);
                                stack.push(v2);
                                stack.push(v1);
                            } else {
                                final Slot v4 = stack.pop();
                                if (bits64.contains(v4))
                                    throw new RuntimeException("Illegal usage");
                                stack.push(v2);
                                stack.push(v1);
                                stack.push(v4);
                                stack.push(v3);
                                stack.push(v2);
                                stack.push(v1);
                            }
                        }
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.POP) {
                    for (final Stack<Slot> stack : stacks) {
                        final Inst inst2 = stack.pop().inst;
                        if (inst2.output == Const.STACK_1 || inst2.output == Const.STACK_2)
                            inst2.output = null;
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                if (inst.opcode == Opcode.POP2) {
                    for (final Stack<Slot> stack : stacks) {
                        final Inst inst2 = stack.pop().inst, inst3;
                        if (!bits64.contains(inst2.output)) {
                            inst3 = stack.pop().inst;
                            if (inst3.output == Const.STACK_1 || inst3.output == Const.STACK_2)
                                inst3.output = null;
                        }
                        if (inst2.output == Const.STACK_1 || inst2.output == Const.STACK_2)
                            inst2.output = null;
                    }
                    instructions.remove(i);
                    i--;
                    continue;
                }
                for (int j = inst.params.length - 1; j >= 0; j--)
                    if (inst.params[j] == Const.STACK_1 || inst.params[j] == Const.STACK_2) {
                        Object v = null;
                        boolean conflict = false;
                        for (final Stack<Slot> stack : stacks) {
                            final Inst inst2 = stack.peek().inst;
                            if (inst2.output != Const.STACK_1 && inst2.output != Const.STACK_2) {
                                if (!(inst2.output instanceof IRTmp))
                                    throw new RuntimeException("Not TMP");
                                if (v == null && !conflict)
                                    v = inst2.output;
                                else if (v != null && !v.equals(inst2.output)) {
                                    conflict = true;
                                    v = null;
                                }
                            }
                        }
                        if (conflict) {
                            v = new IRTmp(tmp++);
                            for (final Stack<Slot> stack : stacks) {
                                final Inst inst2 = stack.pop().inst;
                                if (inst2.output == Const.STACK_1 || inst2.output == Const.STACK_2)
                                    inst2.output = v;
                                else {
                                    instructions.add(MiniUtil.indexOf(inst2, instructions), new Inst(Opcode.STORE, v, new Object[]{inst2.output}));
                                    i++;
                                }
                            }
                        } else if (v == null) {
                            v = new IRTmp(tmp++);
                            for (final Stack<Slot> stack : stacks)
                                stack.pop().inst.output = v;
                        } else
                            for (final Stack<Slot> stack : stacks) {
                                final Inst inst2 = stack.pop().inst;
                                if (inst2.output == Const.STACK_1 || inst2.output == Const.STACK_2)
                                    inst2.output = v;
                            }
                        inst.params[j] = v;
                    }
                if (inst.opcode == Opcode.CATCH)
                    stackLater.computeIfAbsent(((IRAnchor) inst.params[1]).id, ignored -> new ArrayList<>())
                            .add(new Stack<>());
                else {
                    boolean f = false;
                    for (int j = 0; j < inst.params.length; j++)
                        if (inst.params[j] instanceof IRAnchor) {
                            if (f)
                                throw new RuntimeException("2 anchors are not allowed");
                            f = true;
                            final IRAnchor a = (IRAnchor) inst.params[j];
                            final Stack<Slot> snapshot = anchors.get(a.id);
                            if (snapshot != null) {
                                if (snapshot.size() != stacks.get(0).size())
                                    throw new RuntimeException("Different sizes");
                                for (int i2 = 0; i2 < stacks.get(0).size(); i2++) {
                                    final Inst inst3 = snapshot.get(i2).inst;
                                    for (final Stack<Slot> stack : stacks) {
                                        final Inst inst2 = stack.get(i2).inst;
                                        if (inst2.output == null) {
                                            inst2.output = inst3.output;
                                            continue;
                                        }
                                        if (inst2.output.equals(inst3.output))
                                            continue;
                                        instructions.add(MiniUtil.indexOf(inst2, instructions), new Inst(Opcode.STORE, inst3.output, new Object[]{inst2.output}));
                                        i++;
                                    }
                                }
                            } else {
                                final ArrayList<Stack<Slot>> l = stackLater.computeIfAbsent(((IRAnchor) inst.params[j]).id, ignored -> new ArrayList<>());
                                for (final Stack<Slot> stack : stacks) {
                                    final Stack<Slot> s = new Stack<>();
                                    s.addAll(stack);
                                    l.add(s);
                                }
                            }
                        }
                }
                if (inst.output == Const.STACK_1 || inst.output == Const.STACK_2) {
                    if (stacks.isEmpty())
                        throw new RuntimeException("No stacks");
                    final Slot slot = new Slot(inst);
                    for (final Stack<Slot> stack : stacks)
                        stack.push(slot);
                }
                if (Opcode.STOPPERS.contains(inst.opcode) || inst.opcode == Opcode.GOTO)
                    stacks.clear();
            }
        } catch (final RuntimeException ex) {
            System.err.println("Index: " + i);
            System.err.println(stacks);
            throw ex;
        }
    }
}