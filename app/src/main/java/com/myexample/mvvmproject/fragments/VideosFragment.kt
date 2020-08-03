package com.myexample.mvvmproject.fragments

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.app.OnboardingSupportFragment
import com.myexample.mvvmproject.HomeScreenActivity
import com.myexample.mvvmproject.R

class VideosFragment : OnboardingSupportFragment() {
    lateinit var imageView: ImageView
    var intArray = arrayOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_background)
    var stringarrayData = listOf<String>("Launcher", "Launcher")
    override fun getPageTitle(pageIndex: Int): CharSequence {
        return stringarrayData.get(pageIndex)
    }

    override fun getPageDescription(pageIndex: Int): CharSequence {
        return stringarrayData.get(pageIndex)
    }

    override fun onCreateForegroundView(inflater: LayoutInflater?, container: ViewGroup?): View? {
        return ImageView(requireContext()).apply {
            imageView = this
        }
    }

    override fun onCreateBackgroundView(inflater: LayoutInflater?, container: ViewGroup?): View? {
        return ImageView(requireContext()).apply {
            imageView = this
        }
    }

    override fun getPageCount(): Int {
        return intArray.size
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewData = super.onCreateView(inflater, container, savedInstanceState)
        logoResourceId = R.mipmap.bg

        /*val handler = Handler()
        val runnable = Runnable { onFinishFragment() }
        handler.postDelayed(runnable,5000)*/
        return viewData
    }

    override fun onCreateContentView(inflater: LayoutInflater?, container: ViewGroup?): View? {
        return ImageView(requireContext())
            .apply {
                scaleType = ImageView.ScaleType.FIT_XY
                setImageResource(R.mipmap.ic_launcher)
                imageView = this
            }
    }

    override fun onFinishFragment() {
        super.onFinishFragment()
        val intent = Intent(requireActivity(), HomeScreenActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private val TAG = VideosFragment::class.java.simpleName
    }

    override fun onCreateEnterAnimation(): Animator =
        ObjectAnimator.ofFloat(imageView, View.SCALE_X, 0.2f, 1.0f)
            .setDuration(5000)
}