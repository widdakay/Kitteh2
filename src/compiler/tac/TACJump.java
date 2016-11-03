/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.tac;
import compiler.X86Emitter;

/**
 *
 * @author leijurv
 */
public class TACJump extends TACStatement {
    int jumpTo;
    public TACJump(int jumpTo) {
        this.jumpTo = jumpTo;
    }
    @Override
    protected void onContextKnown() {
        if (getClass() != TACJump.class) {
            throw new IllegalStateException("Subclasses must override onContextKnown");
        }
    }
    @Override
    public String toString0() {
        return "jmp " + jumpTo;
    }
    @Override
    public void printx86(X86Emitter emit) {
        emit.addStatement("jmp " + emit.lineToLabel(jumpTo));
    }
    public int jumpTo() {
        return jumpTo;
    }
    public void bump(boolean direction) {
        jumpTo += direction ? 1 : -1;
    }
}
