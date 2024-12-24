package com.example.videoplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cars.reels.R
import com.cars.reels.VideoModel
import java.util.Locale

class VideoAdapter(
    private val videoPaths: List<VideoModel>,
    private val viewPager: ViewPager2,
    private val context: Context
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isAutopilotEnabled = false
    private var isListening = false
    private var currentPosition = 0
    private var currentVideoHolder: VideoViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoModel = videoPaths[position]
        holder.videoTitle.text = videoModel.videoTitle
        holder.videoDesc.text = videoModel.videoDesc
        holder.videoView.setVideoPath(videoModel.videoUrl)

        if (position == currentPosition) {
            currentVideoHolder = holder
        }

        holder.videoView.setOnPreparedListener { it.start() }

        holder.videoView.setOnCompletionListener {
            if (isAutopilotEnabled) {
                val nextPosition = viewPager.currentItem + 1
                if (nextPosition < videoPaths.size) {
                    currentPosition = nextPosition
                    viewPager.setCurrentItem(nextPosition, true)
                }
            } else {
                it.start()
            }
        }

        holder.enableAutoPilot.isChecked = isAutopilotEnabled
        holder.enableAutoPilot.setOnCheckedChangeListener { _, isChecked ->
            isAutopilotEnabled = isChecked
            if (isChecked) startListening() else stopListening()
        }

        holder.like.setOnClickListener {
            currentVideoHolder?.likeTxt?.text = "1"
            showToast("Video Liked")
            highlightIcon(holder.like)
        }

        holder.dislike.setOnClickListener {
            currentVideoHolder?.likeTxt?.text = "0"
            showToast("Video Disliked")
            highlightIcon(holder.dislike)
        }

        holder.comment.setOnClickListener {
            showToast("Comment Posted")
            highlightIcon(holder.comment)
        }

        holder.share.setOnClickListener {
            showToast("Shared")
            highlightIcon(holder.share)
        }

        holder.rotate.setOnClickListener {
            showToast("Rotated")
            highlightIcon(holder.rotate)
        }
    }

    override fun getItemCount(): Int = videoPaths.size

    private fun startListening() {
        if (isListening) return

        isListening = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (isListening) restartListening()
                }
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        handleCommand(it.lowercase(Locale.getDefault()))
                    }
                    if (isListening) restartListening()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        speechRecognizer?.startListening(createSpeechRecognizerIntent())
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null
    }

    private fun restartListening() {
        speechRecognizer?.startListening(createSpeechRecognizerIntent())
    }

    private fun createSpeechRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command (e.g., like, dislike, or next)")
        }
    }

    private fun handleCommand(command: String) {
        when (command) {
            "like" -> handleLike(currentVideoHolder?.like, currentVideoHolder?.dislike)
            "dislike" -> handleLikeDislike(currentVideoHolder?.dislike, currentVideoHolder?.like)
            "next" -> moveToNextVideo()
        }
    }

    private fun moveToNextVideo() {
        if (isAutopilotEnabled) {
            val nextPosition = viewPager.currentItem + 1
            if (nextPosition < videoPaths.size) {
                currentPosition = nextPosition
                viewPager.setCurrentItem(nextPosition, true)
                showToast("Switched to next video")
            } else {
                showToast("No more videos")
            }
        }
    }

    private fun handleLike(selectedIcon: ImageView?, otherIcon: ImageView?) {
        currentVideoHolder?.likeTxt?.text = "1"
        selectedIcon?.let { highlightIcon(it) }
        otherIcon?.clearColorFilter()
    }
    private fun handleLikeDislike(selectedIcon: ImageView?, otherIcon: ImageView?) {
        currentVideoHolder?.likeTxt?.text = "0"
        selectedIcon?.let { highlightIcon(it) }
        otherIcon?.clearColorFilter()
    }

    private fun highlightIcon(icon: ImageView) {
        val orangeTint = 0xFFFFA500.toInt()
        icon.setColorFilter(orangeTint, android.graphics.PorterDuff.Mode.SRC_ATOP)
        icon.postDelayed({ icon.clearColorFilter() }, 300)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: VideoView = itemView.findViewById(R.id.videoView)
        val videoTitle: TextView = itemView.findViewById(R.id.vidTitle)
        val videoDesc: TextView = itemView.findViewById(R.id.vidDescription)
        val like: ImageView = itemView.findViewById(R.id.like)
        val likeTxt: TextView = itemView.findViewById(R.id.likesText)
        val dislike: ImageView = itemView.findViewById(R.id.dislike)
        val comment: ImageView = itemView.findViewById(R.id.comment)
        val share: ImageView = itemView.findViewById(R.id.share)
        val rotate: ImageView = itemView.findViewById(R.id.rotate)
        val enableAutoPilot: SwitchCompat = itemView.findViewById(R.id.switchButton)
    }
}
