package com.lumberjack

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Lumberjack.getInstance().init(this)

        Lumberjack.getInstance().logException("MainActivity","MainActivity on create method", Exception())


    }
}

data class Abc(val a:String)
