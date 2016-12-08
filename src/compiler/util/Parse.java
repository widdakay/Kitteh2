/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.util;
import compiler.Context;
import compiler.Keyword;
import compiler.Operator;
import compiler.Struct;
import compiler.token.Token;
import compiler.token.TokenType;
import compiler.type.Type;
import compiler.type.TypePointer;
import compiler.type.TypeStruct;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author leijurv
 */
public class Parse {
    public static Type typeFromTokens(List<Token> tokens, Context context) {
        return typeFromTokens(tokens, context, null);
    }
    public static Type typeFromTokens(List<Token> tokens, Context context, String selfRef) {
        if (tokens.isEmpty()) {
            return null;
        }
        String accessing = null;
        if (tokens.size() > 1 && tokens.get(1) == TokenType.ACCESS) {
            accessing = (String) tokens.get(0).data();
            tokens = tokens.subList(2, tokens.size());
        }
        Token first = tokens.get(0);
        Type tp;
        switch (first.tokenType()) {
            case KEYWORD:
                Keyword keyword = (Keyword) first;
                if (!keyword.isType()) {
                    return null;
                }
                tp = keyword.type;
                break;
            case VARIABLE:
                String name = (String) first.data();
                if (name.equals(selfRef)) {
                    tp = new TypeStruct(null);
                } else {
                    if (accessing != null) {
                        name = accessing + "::" + name;
                    }
                    Struct struct = context.getStruct(name);
                    if (struct == null) {
                        return null;
                    }
                    tp = new TypeStruct(struct);
                }
                break;
            default:
                return null;
        }
        for (int i = 1; i < tokens.size(); i++) {
            if (tokens.get(i) != Operator.MULTIPLY) {
                return null;
            }
            tp = new <Type>TypePointer<Type>(tp); //if there are N *s, it's a N - nested pointer, so for every *, wrap the type in another TypePointer
        }
        return tp;
    }
    public static List<List<Token>> splitList(List<Token> list, TokenType splitOn) {
        List<List<Token>> result = new ArrayList<>();
        List<Token> temp = new ArrayList<>();
        for (Token t : list) {
            if (t.tokenType() == splitOn) {
                result.add(temp);
                temp = new ArrayList<>();
            } else {
                temp.add(t);
            }
        }
        if (!list.isEmpty()) {
            result.add(temp);
        }
        return result;
    }
    public static Type typeFromObjs(ArrayList<Object> o, Context context) {
        ArrayList<Token> tmp = new ArrayList<>(o.size());
        for (Object obj : o) {
            if (!(obj instanceof Token)) {
                return null;
            }
            tmp.add((Token) obj);
        }
        return Parse.typeFromTokens(tmp, context);
    }
}