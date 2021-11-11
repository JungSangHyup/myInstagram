package com.example.myinstagram.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myinstagram.LoginActivity
import com.example.myinstagram.R
import com.example.myinstagram.databinding.FragmentDetailBinding
import com.example.myinstagram.databinding.ItemDetailBinding
import com.example.myinstagram.model.AlarmDTO
import com.example.myinstagram.model.ContentDTO
import com.example.myinstagram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HashDetailFragment : Fragment() {
    lateinit var firestore: FirebaseFirestore
    var uid : String? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = FragmentDetailBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid


        if(uid == null){
            val intent = Intent(getActivity(), LoginActivity::class.java)
            startActivity(intent)
        }

        var linearLayoutManager : LinearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true

        view.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragmentRecyclerview.layoutManager = linearLayoutManager

        return view.root
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : MutableList<ContentDTO> = mutableListOf()
        var contentUidList : MutableList<String> = mutableListOf()
        lateinit var itemContext : Context



        init {
            var hashtag = arguments?.getString("hashtag").toString()
            firestore?.collection("images")?.whereArrayContains("hashtags", hashtag).addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            itemContext = parent.context

            return CustomViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as CustomViewHolder).itemView

            //UserId
            holder.binding.detailviewitemProfileTextview.text = contentDTOs!![position].userId

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(holder.binding.detailviewitemImageviewContent)
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(holder.binding.detailviewitemProfileImage)

            holder.binding.detailviewitemExplainTextview.text = contentDTOs!![position].explain

            holder.binding.detailviewitemFavoritecounterTextview.text = "Likes " + contentDTOs!![position].favoriteCount

            var hashtagList = contentDTOs!![position].hashtags

            holder.binding.detailviewitemHashtagBox.removeAllViews()

            hashtagList.forEach {
                var tv = TextView(itemContext)
                tv.text = it
                tv.gravity = Gravity.CENTER
                tv.textSize = 16f
                tv.background = resources.getDrawable(R.drawable.hashtag)
                tv.setOnClickListener {v->
                    val bundle = Bundle()
                    var fragment = HashDetailFragment()
                    bundle.putString("hashtag", it)
                    fragment.arguments = bundle
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.main_content, fragment)
                        ?.commit()
                }
                holder.binding.detailviewitemHashtagBox.addView(tv)
            }


            holder.binding.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(position)
            }

            if(contentDTOs!![position].favorites.containsKey(uid)){
                holder.binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            }else {
                holder.binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            holder.binding.detailviewitemProfileImage.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }
            holder.binding.detailviewitemCommentImageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        inner class CustomViewHolder(val binding: ItemDetailBinding) : RecyclerView.ViewHolder(binding.root)

        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)
                }else{
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO?.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }
        }

        fun favoriteAlarm(destinationUid: String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "Howlstargram", message)
        }

    }
}