package com.example.kitrinostheos

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.appcompat.widget.Toolbar
import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Drawer Layout
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        // Navigation View (drawer menu)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Find NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_section1,
                R.id.nav_section2,
                R.id.nav_section3,
                R.id.nav_section4,
                R.id.nav_section5,
                R.id.nav_section6,
                R.id.nav_section7,
                R.id.nav_section8,
                R.id.nav_section9,
                R.id.nav_section10,
                R.id.nav_section11,
                R.id.nav_section12,
                R.id.nav_section13,
                R.id.nav_section14,
                R.id.nav_section15,
                R.id.nav_section16,
                R.id.nav_section17,
                R.id.nav_section18,
                R.id.nav_section19,
                R.id.nav_section20
            ),
            drawerLayout
        )

        // Set up ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up NavigationUI for Drawer clicks
        NavigationUI.setupWithNavController(navView, navController)

        // Load Section1 when tapping the logo in the header
        val headerView: View = navView.getHeaderView(0)
        val logoImageView: ImageView = headerView.findViewById(R.id.imageView)
        logoImageView.setOnClickListener {
            navController.navigate(R.id.nav_section1)
            drawerLayout.closeDrawers() // Κλείνει το drawer μετά το click
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.primaryNavigationFragment

        if (currentFragment is WebViewReloadable && currentFragment.getWebView().canGoBack()) {
            currentFragment.getWebView().goBack()
        } else if (navController.navigateUp()) {
            // Do nothing (navigation handled by navigateUp)
        } else if (currentFragment !is Section1Fragment) { // Check if not on Section1Fragment
            navController.navigate(R.id.nav_section1) // Navigate to Section1Fragment
        }
    }
}