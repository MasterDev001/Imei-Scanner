package com.example.imeiscanner.ui.fragments.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.imeiscanner.R
import com.example.imeiscanner.databinding.FragmentChangeUserNameBinding
import com.example.imeiscanner.ui.fragments.base.BaseChangeFragment

class ChangeUserNameFragment : BaseChangeFragment(R.layout.fragment_change_user_name) {

    private lateinit var binding: FragmentChangeUserNameBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       binding= FragmentChangeUserNameBinding.inflate(inflater,container,false)
        return binding.root
    }

}