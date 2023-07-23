package com.videocall.vedeocalltwo

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.videocall.vedeocalltwo.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val appId = "baf5d692b1ac42bf8d947641dfe2a8ae"
    private val channelName = "Speeky"
    private val token = "007eJxTYPCXE0j+H254xylW4PxWu12c589uan339vnfsJeXXP6dURJSYEhKTDNNMbM0SjJMTDYxSkqzSLE0MTczMUxJSzVKtEhM3fJyT0pDICNDf0IvCyMDBIL4bAzBBamp2ZUMDABFRyN8" // Replace this with your Agora token
    private val uid = 0 // Set the UID for the local user

    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val PERMISSION_ID = 12
    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, REQUESTED_PERMISSION[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.message!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }

        setupVideoSDKEngine()

        binding.joinButton.setOnClickListener {
            joinCall()
        }
        binding.leaveButton.setOnClickListener {
            leaveCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun joinCall() {
        if (checkSelfPermission()) {
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, option)
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun leaveCall() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = GONE
            isJoined = false
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler =
        object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                showMessage("Remote User joined: $uid")
                runOnUiThread { setupRemoteVideo(uid) }
            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                isJoined = true
                showMessage("Joined Channel: $channel")
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                showMessage("User offline")
                runOnUiThread { removeRemoteVideo(uid) }
            }
        }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        localSurfaceView!!.setZOrderMediaOverlay(true)
        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                0
            )
        )
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        remoteSurfaceView!!.id = uid
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
    }

    private fun removeRemoteVideo(uid: Int) {
        val view = binding.remoteUser.findViewById<SurfaceView>(uid)
        if (view != null) {
            binding.remoteUser.removeView(view)
        }
    }
}
