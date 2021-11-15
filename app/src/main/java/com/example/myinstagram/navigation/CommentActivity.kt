package com.example.myinstagram.navigation

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColor
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myinstagram.MainActivity
import com.example.myinstagram.R
import com.example.myinstagram.databinding.ActivityCommentBinding
import com.example.myinstagram.databinding.ItemCommentBinding
import com.example.myinstagram.model.AlarmDTO
import com.example.myinstagram.model.ContentDTO
import com.example.myinstagram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CommentActivity :AppCompatActivity() {
    val activityCommentBinding by lazy { ActivityCommentBinding.inflate(layoutInflater) }
    lateinit var destinationUid: String
    lateinit var contentUid: String
    lateinit var uid: String


    lateinit var auth : FirebaseAuth
    lateinit var storage : FirebaseStorage
    lateinit var firestore : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityCommentBinding.root)



        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        getSystemService(Context.INPUT_METHOD_SERVICE)

        contentUid = intent.getStringExtra("contentUid").toString()
        destinationUid = intent.getStringExtra("destinationUid").toString()
        uid = intent.getStringExtra("uid").toString()

        activityCommentBinding.commentRecyclerview.adapter = CommentRecyclerviewAdapter()
        activityCommentBinding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        activityCommentBinding.commentBtnSend.setOnClickListener {
            var comment = ContentDTO.Comment()
            var firebaseAuth = FirebaseAuth.getInstance()
            comment.userId = firebaseAuth.currentUser?.email
            comment.uid = firebaseAuth.currentUser?.uid
            comment.comment = activityCommentBinding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, activityCommentBinding.commentEditMessage.text.toString())
            activityCommentBinding.commentEditMessage.setText("")
        }

        if(uid == auth.currentUser!!.uid){
            activityCommentBinding.detailviewitemDelBtn.visibility = View.VISIBLE
            activityCommentBinding.detailviewitemDelBtn.setOnClickListener {
                contentDelete()
            }
        }else {
            activityCommentBinding.detailviewitemDelBtn.visibility = View.INVISIBLE
        }


        getImage(contentUid)
    }

    fun contentDelete() {



        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)

        builder.setTitle("콘텐츠 삭제")
        builder.setMessage("삭제하시겠습니까?")
        builder.setIcon(R.mipmap.ic_launcher)

        var listener = DialogInterface.OnClickListener { dialog, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> {
                    var imageFileName : String
                    firestore.collection("images").document(contentUid!!)
                        .addSnapshotListener { value, error ->
                            imageFileName = value?.data?.get("imageFileName").toString()

                            var storageRef = storage?.reference?.child("images/" + imageFileName)
                            storageRef.delete().addOnSuccessListener {  }
                        }

                    firestore.collection("images").document(contentUid!!)
                        .delete().addOnSuccessListener {  }

                    Toast.makeText(this, "게시물을 삭제하였습니다.", Toast.LENGTH_LONG);

                    var intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    return@OnClickListener
                }
            }
        }

        builder.setPositiveButton("예", listener)
        builder.setNegativeButton("아니요", listener)

        builder.show()
    }

    fun getImage(contentUid: String){
        FirebaseFirestore.getInstance()
            .collection("images")
            .document(contentUid!!)
            .get().addOnSuccessListener { doc ->
                if(doc == null){
                    return@addOnSuccessListener
                }
                var imageUrl = doc.data?.get("imageUrl")
                Glide.with(this).load(imageUrl).into(activityCommentBinding.commentImageview)
            }
    }

    fun commentAlarm(destinationUid: String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_follow) + " of \n" + message
        FcmPush.instance.sendMessage(destinationUid, "Howlstargram", msg)
    }

    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { value, error ->
                    comments.clear()
                    if(value == null) return@addSnapshotListener
                    for(snapshot in value.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHOlder(binding)
        }

        private inner class CustomViewHOlder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as CustomViewHOlder).itemView

            holder.binding.commentviewitemTextviewComment.text = comments[position].comment
            holder.binding.commentviewitemTextviewProfile.text = comments[position].userId

            FirebaseFirestore.getInstance().collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url = task.result["image"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(holder.binding.commentviewitemImageviewProfile)
                    }
                }



        }

        override fun getItemCount(): Int {
            return comments.size
        }
    }
}