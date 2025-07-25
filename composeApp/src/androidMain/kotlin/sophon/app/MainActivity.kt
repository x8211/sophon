package sophon.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import sophon.server.ServiceManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).also {
            it.setBackgroundColor(Color.RED)
        })

        val rsp = ServiceManager.AMS?.javaClass?.methods?.forEach { method ->
            Log.i("whw", "onCreate: ${method.name}")
        }
    }
}