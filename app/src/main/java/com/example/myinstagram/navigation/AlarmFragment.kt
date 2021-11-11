package com.example.myinstagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myinstagram.R
import com.example.myinstagram.databinding.FragmentAlarmBinding
import com.example.myinstagram.databinding.ItemCommentBinding
import com.example.myinstagram.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlarmFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = FragmentAlarmBinding.inflate(inflater, container, false)
        view.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter()
        view.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return view.root
    }
    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid).addSnapshotListener { value, error ->
                alarmDTOList.clear()
                if(value == null) return@addSnapshotListener
                for(snapshot in value.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }



        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as CustomViewHolder).itemView

            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    view?.context?.let { Glide.with(it).load(url).apply(RequestOptions().circleCrop()).into(holder.binding.commentviewitemImageviewProfile) }
                }
            }

            when(alarmDTOList[position].kind){
                0 -> {
                    val str_0 = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    holder.binding.commentviewitemTextviewProfile.text = str_0
                }
                1 -> {
                    val str_1 = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + " of \n" + alarmDTOList[position].message
                    holder.binding.commentviewitemTextviewProfile.text = str_1
                }
                2 -> {
                    val str_2 = alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    holder.binding.commentviewitemTextviewProfile.text = str_2
                }
            }
            holder.binding.commentviewitemTextviewComment.visibility = View.INVISIBLE
        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

    }
}