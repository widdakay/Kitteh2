/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.tac.optimize;
import compiler.tac.TACCast;
import compiler.tac.TACConst;
import compiler.tac.TACFunctionCall;
import compiler.tac.TACJump;
import compiler.tac.TACPointerDeref;
import compiler.tac.TACPointerRef;
import compiler.tac.TACStandard;
import compiler.tac.TACStatement;
import compiler.tac.TempVarUsage;
import compiler.type.TypeStruct;
import java.util.List;

/**
 * Remove temp vars that have no point Like t1=5,a=t1+t2 should become a=5+t2
 *
 * @author leijurv
 */
public class UselessTempVars extends TACOptimization {
    public static boolean isTempVariable(String s) {
        if (!s.startsWith(TempVarUsage.TEMP_VARIABLE_PREFIX)) {//all temp vars start with t. variables starting with a t are not supported in kitteh
            return false;
        }
        try {
            Integer.parseInt(s.substring(TempVarUsage.TEMP_VARIABLE_PREFIX.length()));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    @Override
    public void run(List<TACStatement> block, int blockBegin) {
        for (int ind = 0; ind < block.size() - 1; ind++) {
            if (!(block.get(ind) instanceof TACConst)) {
                continue;
            }
            TACConst curr = (TACConst) block.get(ind);
            String valSet = curr.destName;
            if (!isTempVariable(valSet)) {//this should ignore struct field writes because they don't follow the pattern of "tmp" then an integer
                continue;
            }
            for (int usageLocation = ind + 1; usageLocation < block.size(); usageLocation++) {
                TACStatement next = block.get(usageLocation);
                if (curr.dest.getType() instanceof TypeStruct) {
                    if (usageLocation > ind + 1) {//todo figure out why this inner IF is really necesary to make the tests succeed
                        break;
                    }
                }
                if (next instanceof TACStandard) {
                    TACStandard n = (TACStandard) next;
                    if (n.secondName.equals(valSet)) {
                        n.secondName = curr.sourceName;
                        n.second = curr.source;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                    if (n.firstName.equals(valSet)) {
                        n.firstName = curr.sourceName;
                        n.first = curr.source;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                }
                if (next instanceof TACConst) {
                    TACConst c = (TACConst) next;
                    if (c.sourceName.equals(valSet)) {
                        c.sourceName = curr.sourceName;
                        c.source = curr.source;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                }
                if (next instanceof TACFunctionCall) {
                    TACFunctionCall c = (TACFunctionCall) next;
                    boolean shouldBreak = false;
                    for (int i = 0; i < c.paramNames.size(); i++) {
                        if (c.paramNames.get(i).equals(valSet)) {
                            c.paramNames.set(i, curr.sourceName);
                            c.params.set(i, curr.source);
                            block.remove(ind);
                            ind = Math.max(-1, ind - 2);
                            shouldBreak = true;
                            break;
                        }
                    }
                    if (shouldBreak) {
                        break;
                    }
                }
                if (next instanceof TACPointerRef) {
                    TACPointerRef t = (TACPointerRef) next;
                    if (t.sourceName.equals(valSet)) {
                        t.source = curr.source;
                        t.sourceName = curr.sourceName;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                }
                if (next instanceof TACPointerDeref) {
                    TACPointerDeref t = (TACPointerDeref) next;
                    if (t.sourceName.equals(valSet)) {
                        t.source = curr.source;
                        t.sourceName = curr.sourceName;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                }
                if (next instanceof TACCast) {
                    TACCast t = (TACCast) next;
                    if (t.inputName.equals(valSet) && curr.source != null) {
                        t.input = curr.source;
                        t.inputName = curr.sourceName;
                        block.remove(ind);
                        ind = Math.max(-1, ind - 2);
                        break;
                    }
                }
                if (next.requiredVariables().contains(valSet)) {
                    //this temp variable is used in a context that does not allow for optimized insertion
                    //since temp variables are only used once, we can't insert it into an expression after its usage
                    break;
                }
                if (next.modifiedVariables().contains(valSet)) {
                    //something is wrong
                    //something like:
                    //tmp0=1
                    // ... (temp0 not used)
                    //tmp0=2
                    //so somehow tmp0 was set but then unused
                    //again, this might be caused by a different optimization leaving dangling temp variables
                    throw new RuntimeException("Another optimization did something weird");
                }
                if (next instanceof TACJump) {
                    //if it gets to here, it means something is wrong
                    //it means that a temp variable is set, then goes unused up to a jump
                    //and since no temp variables are used again after a jump
                    //it means that somehow this temp variable was generated but went unused entirely
                    //it could indicate a bug in a different optimization btw
                    throw new RuntimeException("Another optimization did something weird");
                }
            }
        }
    }
}
