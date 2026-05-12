package com.chandni.kavos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SafeZonesActivity : AppCompatActivity() {

    private lateinit var adapter: SafeZoneAdapter
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_zones)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { goHome() }

        adapter = SafeZoneAdapter(
            zones = emptyList(),
            onNavigate = { zone -> openMapsTo(zone) },
            onDelete = { zone -> confirmDelete(zone) }
        )
        val rv = findViewById<RecyclerView>(R.id.rvZones)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddZone).setOnClickListener {
            showAddDialog()
        }

        // Hero gradient (always purple → pink for the nearest card)
        val nearestGrad = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(getColor(R.color.grad_purple_a), getColor(R.color.grad_pink_a))
        )
        findViewById<View>(R.id.nearestGradient).background = nearestGrad

        findViewById<View>(R.id.cardNearest).setOnClickListener {
            adapter.let {
                val first = currentZonesSnapshot().firstOrNull()
                if (first != null) openMapsTo(first.first) else showAddDialog()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
    }

    override fun onResume() {
        super.onResume()
        refreshLocationAndZones()
    }

    private var snapshotZones: List<Pair<SafeZone, Float?>> = emptyList()
    private fun currentZonesSnapshot() = snapshotZones

    private fun refreshLocationAndZones() {
        currentLocation = lastKnownLocation()
        Thread {
            val zones = AppDatabase.getDatabase(this).safeZoneDao().getAllZonesForUser(UserScope.uid())
            val withDist = zones.map { z ->
                val d = currentLocation?.let {
                    val out = FloatArray(1)
                    Location.distanceBetween(it.latitude, it.longitude, z.latitude, z.longitude, out)
                    out[0]
                }
                z to d
            }.sortedBy { it.second ?: Float.MAX_VALUE }

            runOnUiThread {
                snapshotZones = withDist
                adapter.submit(withDist)
                renderNearest(withDist.firstOrNull())
                findViewById<View>(R.id.llEmptyZones).visibility =
                    if (withDist.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun renderNearest(nearest: Pair<SafeZone, Float?>?) {
        val ivIcon = findViewById<ImageView>(R.id.ivNearestIcon)
        val tvName = findViewById<TextView>(R.id.tvNearestName)
        val tvDist = findViewById<TextView>(R.id.tvNearestDistance)
        val arrow = findViewById<TextView>(R.id.tvNearestArrow)

        if (nearest == null) {
            ivIcon.setImageResource(R.drawable.ic_zone_other)
            tvName.text = "Add your first safe zone"
            tvDist.text = "Tap + to begin"
            arrow.text = "+"
            return
        }

        val (zone, dist) = nearest
        val (iconRes, _) = SafeZoneTypes.iconAndColor(zone.type)
        ivIcon.setImageResource(iconRes)
        tvName.text = zone.name
        tvDist.text = when {
            dist == null -> zone.address
            dist < 1000 -> "${dist.toInt()} m away · tap to navigate"
            else -> String.format("%.1f km away · tap to navigate", dist / 1000f)
        }
        arrow.text = "→"
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_safe_zone, null)
        val spType = dialogView.findViewById<Spinner>(R.id.spZoneType)
        val etName = dialogView.findViewById<EditText>(R.id.etZoneName)
        val etAddress = dialogView.findViewById<EditText>(R.id.etZoneAddress)
        val btnLoc = dialogView.findViewById<Button>(R.id.btnUseCurrentLocation)
        val tvCoords = dialogView.findViewById<TextView>(R.id.tvCoords)

        spType.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, SafeZoneTypes.ALL
        )

        var pickedLat: Double? = null
        var pickedLon: Double? = null

        btnLoc.setOnClickListener {
            val loc = lastKnownLocation()
            if (loc != null) {
                pickedLat = loc.latitude
                pickedLon = loc.longitude
                tvCoords.text = String.format("📍 %.5f, %.5f", loc.latitude, loc.longitude)
            } else {
                Toast.makeText(this, "Couldn't get location. Check GPS permissions.", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Add Safe Zone")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val addr = etAddress.text.toString().trim()
                val type = spType.selectedItem?.toString() ?: "Other"

                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (addr.isEmpty() && pickedLat == null) {
                    Toast.makeText(this, "Add an address or use current location", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val lat = pickedLat ?: 0.0
                val lon = pickedLon ?: 0.0
                saveZone(SafeZone(name = name, address = addr, type = type, latitude = lat, longitude = lon, userId = UserScope.uid()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveZone(zone: SafeZone) {
        Thread {
            AppDatabase.getDatabase(this).safeZoneDao().insertZone(zone)
            runOnUiThread {
                Toast.makeText(this, "${zone.name} added", Toast.LENGTH_SHORT).show()
                refreshLocationAndZones()
            }
        }.start()
    }

    private fun confirmDelete(zone: SafeZone) {
        AlertDialog.Builder(this)
            .setTitle("Remove ${zone.name}?")
            .setMessage("This won't affect your guardians or SOS history.")
            .setPositiveButton("Remove") { _, _ ->
                Thread {
                    AppDatabase.getDatabase(this).safeZoneDao().deleteZone(zone)
                    runOnUiThread { refreshLocationAndZones() }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openMapsTo(zone: SafeZone) {
        val uri = if (zone.latitude != 0.0 || zone.longitude != 0.0) {
            Uri.parse("google.navigation:q=${zone.latitude},${zone.longitude}")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(zone.address)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to any maps app
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun lastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) { null }
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}
