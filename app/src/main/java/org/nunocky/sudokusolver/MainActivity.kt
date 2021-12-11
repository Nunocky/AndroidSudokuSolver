package org.nunocky.sudokusolver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import org.nunocky.sudokusolver.databinding.ActivityMainBinding


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
    }

    private fun setupToolbar() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        val toolbar = binding.toolbar

        // (1) OptionsMenuを出すために必要
        setSupportActionBar(toolbar)
        // (2) タイトル設定、Up navigateボタンのために必要
        toolbar.setupWithNavController(navController)
        // (2)はこちらの書き方でもOK
        // NavigationUI.setupWithNavController(toolbar, navController)
    }
}