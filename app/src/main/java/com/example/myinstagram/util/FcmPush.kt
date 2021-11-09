package com.example.myinstagram.util

import com.example.myinstagram.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.okhttp.*
import java.io.IOException

class FcmPush {
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAABZlTv10:APA91bEgtGceIXzU2Bhkk9mLgFWke70VFI0bEQ8HDV3HBhkfJvL4QcF5yNqC10Xz_gEkvw7IJh3rO181Dc8eOPc64KcCLivdzUSji3kumjf8kgd7y0DKsFHJqogU0O6WyEBTKJUgWh3G"
    var project_id = "24047239005"

    var gson : Gson
    var okHttpClient: OkHttpClient

    companion object{
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(destinationUid: String, title: String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task.result.get("pushToken").toString()
                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key="+serverKey)
//                    .addHeader("project_id", project_id)
                    .post(body)
                    .build()

                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(request: Request?, e: IOException?) {
                    }

                    override fun onResponse(response: Response?) {
                        println(response?.body()?.string())
                    }
                })
            }
        }
    }
}