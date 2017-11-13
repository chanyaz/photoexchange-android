package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.other.ServerErrorCode

/**
 * Created by kirakishou on 11/12/2017.
 */
class PhotoAnswerResponse(

        @Expose
        @SerializedName("photo_answer_list")
        val photoAnswerList: List<PhotoAnswerJsonObject>,

        @Expose
        @SerializedName("all_found")
        val allFound: Boolean,

        serverErrorCode: ServerErrorCode

) : StatusResponse(serverErrorCode.value) {

}