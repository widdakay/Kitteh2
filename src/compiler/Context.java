/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler;
import compiler.command.CommandDefineFunction;
import compiler.command.FunctionsContext;
import compiler.expression.ExpressionConst;
import compiler.tac.TempVarUsage;
import compiler.type.Type;
import compiler.util.MutInt;
import java.awt.dnd.InvalidDnDOperationException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Sorta a symbol table that also deals with scoping and temp variables that
 * overlap on the stack
 *
 * @author leijurv
 */
public class Context {
    public static boolean printFull = true;

    public class VarInfo {
        private final String name;
        private final Type type;
        private ExpressionConst knownValue;
        private final int stackLocation;
        private final boolean secret;
        public VarInfo(String name, Type type, int stackLocation) {
            this(name, type, stackLocation, false);
        }
        public VarInfo(String name, Type type, int stackLocation, boolean secret) {
            this.name = name;
            this.type = type;
            this.stackLocation = stackLocation;
            this.secret = secret;
        }
        @Override
        public String toString() {
            //return ("{name: " + name + ", type: " + type + ", location: " + stackLocation + ", val: " + knownValue + "}");
            if (printFull) {
                return ("{name: " + name + ", type: " + type + ", location: " + stackLocation + "}");
            } else {
                return name;
            }
            //return ("{type: " + type + ", location: " + stackLocation + "}");
        }
        public Type getType() {
            return type;
        }
        public int getStackLocation() {
            return stackLocation;
        }
        public String getName() {
            return name;
        }
        public String x86() {
            if (secret) {
                throw new RuntimeException();
            }
            return (stackLocation) + ("(%rbp)");
        }
        public Context getContext() {
            if (secret) {
                throw new RuntimeException();
            }
            return Context.this;
        }
    }
    private final HashMap<String, VarInfo>[] values;
    private final HashMap<String, Struct> structs;
    private int stackSize;
    private Integer additionalSizeTemp = null;
    private TempVarUsage currentTempVarUsage = null;
    private CommandDefineFunction currentFunction = null;
    public FunctionsContext gc;
    public final MutInt varIndex;
    @SuppressWarnings("unchecked")//you can't actually do "new HashMap<>[]{" so I can't fix this warning
    public Context() {
        this.values = new HashMap[]{new HashMap<>()};
        this.stackSize = 0;
        this.structs = new HashMap<>();
        this.varIndex = null;
    }
    public void defineStruct(Struct struct) {
        if (structs.containsKey(struct.name)) {
            throw new InvalidDnDOperationException();
        }
        if (!isTopLevel()) {
            throw new OverlappingFileLockException();
        }
        structs.put(struct.name, struct);
    }
    public Struct getStruct(String name) {
        return structs.get(name);
    }
    public void setCurrFunc(CommandDefineFunction cdf) {
        this.currentFunction = cdf;
    }
    public Type getCurrentFunctionReturnType() {
        return currentFunction.getHeader().getReturnType();
    }
    public TempVarUsage getTempVarUsage() {
        if (currentTempVarUsage == null) {
            throw new IllegalStateException("Unable to add int and boolean on line 7");//lol
        }
        return currentTempVarUsage;
    }
    public boolean isTopLevel() {
        if (values.length == 1) {
            if (getTotalStackSize() != 0) {
                throw new SecurityException();
            }
            if (gc != null) {
                throw new SecurityException();
            }
            return true;
        }
        if (gc == null) {
            throw new SecurityException();
        }
        return false;
    }
    public void setTempVarUsage(TempVarUsage curr) {
        if (curr == null) {
            Stream s = Stream.of(new String[]{});
            s.count();
            s.count();//this causes an exception
        }
        this.currentTempVarUsage = curr;
    }
    public int getTotalStackSize() {
        return stackSize + (additionalSizeTemp == null ? 0 : additionalSizeTemp);
    }
    public int getNonTempStackSize() {
        return stackSize;
    }
    public void updateMinAdditionalSizeTemp(int tempSize) {
        if (additionalSizeTemp == null) {
            additionalSizeTemp = tempSize;
        } else {
            additionalSizeTemp = Math.min(additionalSizeTemp, tempSize);
        }
    }
    private Context(HashMap<String, VarInfo>[] values, int stackSize, FunctionsContext gc, HashMap<String, Struct> structs, CommandDefineFunction currentFunction, MutInt sub) {
        this.values = values;
        this.stackSize = stackSize;
        this.structs = structs;
        this.gc = gc;
        this.currentFunction = currentFunction;
        this.varIndex = sub;
    }
    @SuppressWarnings("unchecked")//you can't actually do "new HashMap<>[" so I can't fix this warning
    public Context subContext() {
        HashMap<String, VarInfo>[] temp = new HashMap[values.length + 1];
        System.arraycopy(values, 0, temp, 0, values.length);
        temp[values.length] = new HashMap<>();
        Context subContext = new Context(temp, stackSize, gc, structs, currentFunction, varIndex == null ? new MutInt() : varIndex);
        return subContext;
    }
    private void defineLocal(String name, VarInfo value) {
        values[values.length - 1].put(name, value);
    }
    public ExpressionConst knownValue(String name) {
        VarInfo info = get(name);
        //System.out.println("Known for " + name + ": " + info);
        if (info == null) {
            return null;
        }
        return info.knownValue;
    }
    public boolean varDefined(String name) {
        for (HashMap<String, VarInfo> value : values) {
            if (value.containsKey(name)) {
                return true;
            }
        }
        return false;
    }
    public void registerArgumentInput(String name, Type type, int loc) {
        if (loc < 16) {
            throw new PatternSyntaxException(name, type + "", loc);
        }
        if (varDefined(name)) {
            throw new IllegalStateException(name + " is already defined -_-");
        }
        defineLocal(name, new VarInfo(name, type, loc));
    }
    public void setType(String name, Type type) {
        if (varDefined(name)) {
            throw new IllegalStateException(name + " is already defined -_-");
        }
        stackSize -= type.getSizeBytes();
        defineLocal(name, new VarInfo(name, type, stackSize));//Otherwise define it as local
    }
    public void setKnownValue(String name, ExpressionConst val) {
        get(name).knownValue = val;
    }
    public void clearKnownValue(String name) {
        if (get(name) != null) {
            setKnownValue(name, null);
        }
    }
    public VarInfo getRequired(String name) {
        VarInfo info = get(name);
        if (info == null) {
            throw new IllegalStateException("WEWLAD\nEWLADW\nWLADWE\nLADWEW\nADWEWL\nDWEWLA\n" + name);
        }
        return info;
    }
    public VarInfo get(String name) {
        for (int i = values.length - 1; i >= 0; i--) {
            VarInfo possibleValue = values[i].get(name);
            if (possibleValue != null) {
                return possibleValue;
            }
        }
        if (currentTempVarUsage != null) {
            VarInfo pos = currentTempVarUsage.getInfo(name);
            if (pos != null) {
                return pos;
            }
        }
        //System.out.println("WARNING: Unable to find requested variable named '" + name + "'. Returning null. Context is " + toString());
        return null;
    }
    @Override
    public String toString() {
        return Arrays.asList(values).toString() + " " + structs;
    }
}
