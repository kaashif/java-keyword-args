package org.example;

import org.example.MyBuiltClassBuilder;

public class Main {
    public static void main(String[] args) {
        // Compile Error!
        //MyClass c1 = new MyClass(MyClass.Builder.create().setFirst("1").setSecond("2"));

        // Compile Error!
        //MyClass c2 = new MyClass(MyClass.Builder.create().setFirst("1").setThird("3"));

        // Works!, all params supplied.
        //MyClass c3 = new MyClass(MyClass.Builder.create().first("1").second("2").third("3"));

        MyBuiltClass c = new MyBuiltClass();
    }
}