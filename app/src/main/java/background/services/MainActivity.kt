package background.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import background.services.MainActivity.Companion.EXERCISE_ACTION_STOPPED
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var _tvStatus : TextView
    private lateinit var _btnStart : Button

    private lateinit var _btnStartExercise : Button
    private lateinit var _tvExerciseStatus : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        _btnStart = findViewById<Button>(R.id.btnStart)
        _tvStatus = findViewById<TextView>(R.id.statusTextView)

        _btnStart.setOnClickListener {
            if (hasAllPermissions()){
                startTrackerService()
            }
            else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSION
                )
            }
        }

        handleIntent(intent)


        _tvExerciseStatus = findViewById(R.id.tvExerciseStatus)
        _btnStartExercise = findViewById(R.id.btnStartExercise)

        _btnStartExercise.setOnClickListener {
            if (hasAllPermissions()){
                val serviceIntent = Intent(this, TrackerService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
                _tvExerciseStatus.text = "Latihan sedang berjalan..."
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSION
                )
            }
        }

        processExerciseIntent(intent)
    }

    companion object {
        const val EXERCISE_ACTION_STOPPED = "background.services.EXERCISE_ACTION_STOPPED"
        const val REQUEST_CODE_PERMISSION = 101
        const val ACTION_SERVICE_STOPPED_JUMP = "background.service.ACTION_SERVICE_STOPPED_JUMP"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        processExerciseIntent(intent)
    }

    private fun processExerciseIntent(intent: Intent?) {
        if (intent?.action == EXERCISE_ACTION_STOPPED) {
            val jumpCount = intent.getIntExtra("KEY_TOTAL_JUMPS", 0)
            Log.d("TrackerService", "Latihan Selesai dikirim ke MainActivity dengan $jumpCount lompatan")

            _tvExerciseStatus.text = "Latihan Hari Ini Selesai!\nTotal Lompatan: $jumpCount kali"

            createCalendarEvent(jumpCount)
        }
    }

    private fun createCalendarEvent(jumps: Int) {
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }

        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            putExtra(CalendarContract.Events.TITLE, "Latihan Fisik Selesai")
            putExtra(CalendarContract.Events.DESCRIPTION, "Berhasil menyelesaikan $jumps lompatan hari ini.")
        }

        startActivity(calendarIntent)
    }


    private fun hasAllPermissions(): Boolean {
        return permissionToRequest.all {
            ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService(){
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        _tvStatus.text = "Background service sedang berjalan..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            var allPermissionGranted = true
            for (result in grantResults){
                if (result != PackageManager.PERMISSION_GRANTED){
                    allPermissionGranted = false
                    break
                }
            }

            if (allPermissionGranted){
                val serviceIntent = Intent(this, TrackerService::class.java)
                ContextCompat.startForegroundService(this,serviceIntent)
                _tvExerciseStatus.text = "Latihan sedang berjalan..."
                finish()
            } else{
                _tvExerciseStatus.text = "Izin ditolak, latihan tidak dapat dijalankan."
            }
        }
    }

    private fun handleIntent(intent: Intent?){
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP){
            _tvStatus.text = "Background services sudah berhenti karena Anda melompat"
        }
    }

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        setIntent(intent)
//        handleIntent(intent)
//    }
}

private val permissionToRequest = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
    arrayOf(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.READ_CALENDAR
    )
} else{
    arrayOf(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.READ_CALENDAR
    )
}

