package org.nunocky.sudokusolver.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.nunocky.sudokusolver.databinding.FragmentCameraBinding
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraFragment : Fragment() {
    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).toTypedArray()
    }

    private lateinit var binding: FragmentCameraBinding
    private val viewModel: CameraViewModel by viewModels()
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        //binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.button2.setOnClickListener {
            takePhoto()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                if (it.second?.isNotBlank() == true) {
                    setFragmentResult("camera", bundleOf("sudoku" to it.second))
                    findNavController().popBackStack()
                }
            }?.onFailure {
                Toast.makeText(
                    this@CameraFragment.context,
                    "couldn't find sudoku board",
                    Toast.LENGTH_SHORT
                ).show()
//                findNavController().popBackStack()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Timber.e("Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // save captured image to cache directory
        val outputFile = File(requireContext().cacheDir, "temp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            outputFile
        )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.process(outputFile.absolutePath)
//                    var srcBitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
//                    srcBitmap = srcBitmap.rotate(90)
//                    viewModel.process(srcBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    findNavController().popBackStack()
                }
            })
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePhoto()
            } else {
                Toast.makeText(requireActivity(), "denied", Toast.LENGTH_SHORT).show()
            }
        }
}

//fun Uri.getBitmapOrNull(contentResolver: ContentResolver): Bitmap? {
//    return kotlin.runCatching {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val source = ImageDecoder.createSource(contentResolver, this)
//            ImageDecoder.decodeBitmap(source)
//        } else {
//            MediaStore.Images.Media.getBitmap(contentResolver, this)
//        }
//    }.getOrNull()
//}