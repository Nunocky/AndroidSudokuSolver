package org.nunocky.sudokusolver.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import org.nunocky.sudokusolver.adapter.AboutListAdapter
import org.nunocky.sudokusolver.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        val adapter = AboutListAdapter()
        binding.listView.adapter = adapter

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                1 -> {
                    startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
                }
                2 -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/Nunocky/AndroidSudokuSolver/")
                    }
                    startActivity(intent)
                }
                else -> {
                }
            }
        }

        return binding.root
    }
}