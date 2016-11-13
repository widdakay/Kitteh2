/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.preprocess;
import compiler.parse.Transform;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author leijurv
 */
public class Preprocessor {
    static final LineBasedTransform CHAR_STRIPPER = new CharStripperFactory()
            .addChar(' ', StripLocation.BOTH)//remove spaces from both ends of each line
            .addChar(';', StripLocation.END)//remove semicolons from the end, they are optional lol (yes you can have something like "x=5;;;;;;  ; ; ;; " and it'll be valid
            .addChar('	', StripLocation.BOTH)//you can use tabs or spaces
            .addChar('\r', StripLocation.BOTH)//idk how returns work
            .addChar((char) 11, StripLocation.BOTH)//literally https://en.wikipedia.org/wiki/Tab_key#Tab_characters
            .addChar(' ', StripLocation.BOTH)//alt+space
            .build();
    static final Transform<ArrayList<String>> REMOVE_BLANK = new BlankLineRemover();
    @SuppressWarnings("unchecked")//you can't actually do "new Transform<>[]{" so I can't fix this warning
    static final Transform<ArrayList<String>>[] PREPROCESSOR_ACTIONS = new Transform[]{
        CHAR_STRIPPER,
        REMOVE_BLANK
    };
    public static ArrayList<Object> preprocess(String rawProgram) {
        rawProgram = new StripComments().transform(rawProgram);
        ArrayList<String> program = new ArrayList<>(Arrays.asList(rawProgram.split("\n")));
        for (Transform<ArrayList<String>> action : PREPROCESSOR_ACTIONS) {
            System.out.println(program);
            action.apply(program);
        }
        ArrayList<Object> temp = new ArrayList<>(program.size());
        for (String line : program) {
            temp.add(line);
        }
        return temp;
    }
}
