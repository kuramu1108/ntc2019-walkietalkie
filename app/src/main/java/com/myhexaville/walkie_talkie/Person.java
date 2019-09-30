package com.myhexaville.walkie_talkie;

import android.content.Context;
import android.widget.EditText;

public class Person {

    private String name;

    public void setName(EditText editText)
    {
        this.name = editText.getText().toString();
    }

    public String getName()
    {
        return this.name;
    }


}
