package com.example.myinstagram.navigation

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myinstagram.databinding.ActivityAddPhotoBinding
import com.example.myinstagram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.*


class AddPhotoActivity : AppCompatActivity(){
    private val binding by lazy { ActivityAddPhotoBinding.inflate(layoutInflater)}
    var PICK_IMAGE_FROM_ALBUM = 0
    lateinit var storage : FirebaseStorage
    lateinit var photoUri : Uri
    lateinit var galleryLauncher : ActivityResultLauncher<String>
    lateinit var auth : FirebaseAuth
    lateinit var firestore : FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initiate storage
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            uri -> binding.addphotoImage.setImageURI(uri)
            photoUri = uri
            contentUpload()
            Toast.makeText(this, "Hello", Toast.LENGTH_LONG)
        }
        binding.addphotoBtnUpload.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    fun contentUpload(){
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //Promise method google 권장방식
        storageRef?.putFile(photoUri)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            contentDTO.imageUrl = uri.toString()
            contentDTO.uid = auth?.currentUser?.uid
            contentDTO.userId = auth?.currentUser?.email
            contentDTO.explain = binding.addphotoEditExplain.text.toString()
            contentDTO.timestamp = System.currentTimeMillis()
            firestore?.collection("images")?.document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)
            finish()
        }



        //Callback method
        /*
        storageRef.putFile(photoUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()
                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.explain = binding.addphotoEditExplain.text.toString()
                contentDTO.timestamp = System.currentTimeMillis()
                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        */
    }
}