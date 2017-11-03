package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.ErrorCode

/**
 * Created by kirakishou on 11/3/2017.
 */
class StatusResponse(
        @Expose
        @SerializedName("server_error_code")
        var errorCode: ErrorCode
)