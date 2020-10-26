package iti.hepia.ch.malandroid

interface CameraSubscriber {
    fun onImageReceived(imgPath: String)
}