package com.example.fragments.ar

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.aroom.R
import com.example.fragments.shopping.ProductDetailsFragmentArgs
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ArMultiFragment : Fragment(R.layout.fragment_vr) {

    private val args by navArgs<ProductDetailsFragmentArgs>()

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private val storage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference.child("models")

    private var modelsToLoad = emptyList<String>();

    //    private val modelsToLoad = listOf(
//        "1706099350306_chairmodel.glb",
//        "lilly.glb",
//        "monstera.glb"
//    );
    private var currentModelIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modelFilenames = arguments?.getStringArrayList("modelFilenames")
        if (!modelFilenames.isNullOrEmpty()) {
            Log.d(TAG, "Model filenames: $modelFilenames")
            modelsToLoad = modelFilenames
        } else {
            Log.e(TAG, "No model filenames found in arguments")
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
            }
            setOnTapArPlaneListener(::onTapPlane)
        }
    }


    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        Log.d(TAG, "Model filenames to load: $modelsToLoad")
        if (currentModelIndex <= modelsToLoad.size) {
            val modelUri = modelsToLoad[currentModelIndex]

            lifecycleScope.launch {
                try {
                    val url = modelUri
                    Log.d(TAG, "Model URL: $url")
                    val model = loadModel(url)
                    addToScene(hitResult, model)
                    currentModelIndex++
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading model: ${e.message}")
                }
            }
        } else {
            Toast.makeText(context, "All models loaded", Toast.LENGTH_SHORT).show()
        }
    }


    private suspend fun loadModel(modelUri: String): Renderable {
        return ModelRenderable.builder()
            .setSource(context, Uri.parse(modelUri))
            .setIsFilamentGltf(true)
            .build()
            .await()
    }



    private val MODEL_SIZE = Vector3(0.7f, 0.7f, 0.7f)
    private val PADDING = 500.2f// Adjust as needed

    private fun addToScene(hitResult: HitResult, model: Renderable) {
        val anchorNode = AnchorNode(hitResult.createAnchor())
        scene.addChild(anchorNode)

        

        anchorNode.addChild(TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
        })
    }

    

    object {
        private const val TAG = "ArMultiFragment"
    }
}
