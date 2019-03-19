package com.xbeats.swipebacksample

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.aitangba.swipeback.SwipeBackActivity
import java.util.Random


class SwipeActivity : SwipeBackActivity() {

    private var page: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)
        val containerLayout = findViewById<RelativeLayout>(R.id.container)
        //随机色
        val random = Random()
        val red = random.nextInt(255)
        val green = random.nextInt(255)
        val blue = random.nextInt(255)
        containerLayout.setBackgroundColor(Color.argb(255, red, green, blue))
        page = MainActivity.page
        findViewById<TextView>(R.id.text).apply {
            text = "当前页$page"
            setOnClickListener {
                Toast.makeText(applicationContext, "点击了当前页$page", Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ finish() }, 2000)
            }
            setOnLongClickListener {
                Toast.makeText(applicationContext, "触发了长按事件", Toast.LENGTH_SHORT).show()
                false
            }
        }
        findViewById<View>(R.id.next_btn).setOnClickListener {
            MainActivity.page = MainActivity.page + 1
            startActivity(Intent(this@SwipeActivity, SwipeActivity::class.java))
        }
    }

    override fun onBackPressed() {
        MainActivity.page = MainActivity.page - 1
        super.onBackPressed()
    }
}
