package com.kirakishou.photoexchange.mvp.view

import io.reactivex.Single
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
interface TakePhotoActivityView : BaseView{
    fun takePhoto(file: File): Single<Boolean>
}