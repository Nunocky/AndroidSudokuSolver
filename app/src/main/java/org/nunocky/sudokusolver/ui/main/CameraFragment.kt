package org.nunocky.sudokusolver.ui.main

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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
        binding.viewModel = viewModel
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
                    it.first?.let { bmp ->
                        saveBitmap("success", bmp)
                    }

                    setFragmentResult("camera", bundleOf("sudoku" to it.second))
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        context, "couldn't find sudoku board", Toast.LENGTH_SHORT
                    ).show()

                    it.first?.let { bmp ->
                        saveBitmap("fail", bmp)
                    }

                    startCamera()
                }
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

    private var cameraProvider: ProcessCameraProvider? = null

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

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
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
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
                    cameraProvider?.unbindAll()
                    viewModel.process(outputFile.absolutePath)
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

    private fun saveBitmap(prefix: String, bmp: Bitmap) {
        val d = Date() // 現在時刻
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        val date = sdf.format(d)
        val filename = "Sudoku-$prefix-$date.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = requireContext().contentResolver

        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item = resolver.insert(collection, contentValues)

            try {
                val outputStream = resolver.openOutputStream(item!!)
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } catch (e: IOException) {
                Timber.d(e)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(item!!, contentValues, null, null);
        } else {
            TODO("Q未満での保存処理")
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