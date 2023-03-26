package org.example;

import org.example.MyBuiltClassBuilder;

public class Main {
    public static void main(String[] args) {
        // Compile Error!
        //MyBuiltClass c1 = MyBuiltClassBuilder.build(MyBuiltClassBuilder.create().setFirst("1").setSecond("2"));

        // Compile Error!
        //MyBuiltClass c1 = MyBuiltClassBuilder.build(MyBuiltClassBuilder.create().setFirst("1").setThird("3"));

        // Works!, all params supplied.
        MyBuiltClass c1 = MyBuiltClassBuilder.build(MyBuiltClassBuilder.create().setFirst("1").setSecond("2").setThird("3"));
    }
}