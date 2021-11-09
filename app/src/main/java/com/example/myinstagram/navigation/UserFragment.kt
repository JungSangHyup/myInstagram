package com.example.myinstagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myinstagram.LoginActivity
import com.example.myinstagram.R
import com.example.myinstagram.databinding.ActivityMainBinding
import com.example.myinstagram.databinding.FragmentUserBinding
import com.example.myinstagram.navigation.model.AlarmDTO
import com.example.myinstagram.navigation.model.ContentDTO
import com.example.myinstagram.navigation.model.FollowDTO
import com.example.myinstagram.util.FcmPush
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class UserFragment : Fragment() {
    lateinit var fragmentView : FragmentUserBinding
    lateinit var firestore: FirebaseFirestore
    lateinit var uid : String
    lateinit var auth : FirebaseAuth
    lateinit var currentUserUid : String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = FragmentUserBinding.inflate(inflater, container, false)

        var photoPickterLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){ imageUri ->
            if(imageUri == null) return@registerForActivityResult
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!)
                .continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                    return@continueWithTask storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    var map = HashMap<String, Any>()
                    map["image"] = uri.toString()
                    FirebaseFirestore.getInstance().collection("profileImages").document(uid!!).set(map)
            }
        }

        fragmentView = view
        uid = arguments?.getString("destinationUid").toString()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth.currentUser!!.uid

        if(uid == currentUserUid){
            //Mypage
            fragmentView?.accountBtnFollowSignout.text = getString(R.string.signout)
            fragmentView?.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else {
            //OtherUserpage
            fragmentView?.accountBtnFollowSignout.text = getString(R.string.follow)

            var mainactivity = ActivityMainBinding.inflate(layoutInflater)

            mainactivity.toolbarUsername?.text = arguments?.getString("userId")
            mainactivity?.toolbarBtnBack?.setOnClickListener {
                mainactivity.bottomNavigation.selectedItemId = R.id.action_home
            }
            mainactivity?.toolbarTitleImage?.visibility = View.GONE
            mainactivity?.toolbarUsername?.visibility = View.VISIBLE
            mainactivity?.toolbarBtnBack?.visibility = View.VISIBLE
            fragmentView?.accountBtnFollowSignout?.setOnClickListener {
                requestFollow()
            }
        }


        firestore = FirebaseFirestore.getInstance()
        view.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        view.accountRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        view.accountIvProfile.setOnClickListener {
            var image = photoPickterLauncher.launch("image/*")
        }

        getProfileImage()
        getFollowerAndFollowing()
        return view.root
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "Howlstargram", message)
    }


    fun getFollowerAndFollowing(){
        firestore.collection("users")?.document(uid!!).addSnapshotListener { documentSnapshot, firebaseStoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView.accountTvFollowingCount.text = followDTO.followingCount.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView.accountTvFollowerCount.text = followDTO.followerCount.toString()
                if(followDTO.followers.containsKey(currentUserUid)){
                    fragmentView.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                    fragmentView.accountBtnFollowSignout.background.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else {
                    if(uid != currentUserUid){
                        fragmentView.accountBtnFollowSignout.text = getString(R.string.follow)
                        fragmentView.accountBtnFollowSignout.background.colorFilter = null
                    }
                }

            }
        }
    }

    fun requestFollow(){
        //Save data to my account
        var tsDocFollowing = firestore.collection("users").document(currentUserUid)
        firestore.runTransaction {
            transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true
                followerAlarm(uid!!)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                //It remove following thir person when a thir person follow me
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)
            }else{
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        var tsDocFollower = firestore.collection("users").document(uid!!)

        firestore.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollower, followDTO!!)

                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)){
                //It remove following thir person when a thir person follow me
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid)
            }else{
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true

            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }


        //Save data to thir person
    }

    fun getProfileImage(){
        firestore?.collection("profileImages").document(uid).addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot.data!!["image"]
                activity?.let { Glide.with(it).load(url).apply(RequestOptions().circleCrop()).into(fragmentView.accountIvProfile) }
            }

        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : MutableList<ContentDTO> = mutableListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView.accountTvPostCount.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageview

            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}