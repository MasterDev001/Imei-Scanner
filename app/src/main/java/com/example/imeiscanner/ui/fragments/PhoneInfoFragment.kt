package com.example.imeiscanner.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.imeiscanner.R
import com.example.imeiscanner.databinding.FragmentEditPhoneDataBinding
import com.example.imeiscanner.models.PhoneDataModel
import com.example.imeiscanner.ui.fragments.base.BaseFragment
import com.example.imeiscanner.utilits.replaceFragment


class PhoneInfoFragment : BaseFragment(R.layout.fragment_edit_phone_data) {
    private lateinit var items: PhoneDataModel

    private lateinit var binding: FragmentEditPhoneDataBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditPhoneDataBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        installData()
        changeToEdit()

    }

    private fun changeToEdit() {
        binding.btnEdit.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("items", items)
            parentFragmentManager.setFragmentResult("data_from_editPhone_fragment", bundle)
            replaceFragment(EditFragment())
        }
    }

    private fun installData() {
        parentFragmentManager.setFragmentResultListener(
            "data_from_main_fragment",
            this
        ) { requestKey, result ->
            items = result.getSerializable("position") as PhoneDataModel
            binding.tvPhoneName.text = items.phone_name
            binding.tvPhoneSerialNumber.text = items.phone_serial_number
            binding.tvPhoneImei1.text = items.phone_imei1
            binding.tvPhoneImei2.text = items.phone_imei2
            binding.tvPhoneAddedDate.text = items.phone_added_date
            binding.tvPhoneMemory.text = items.phone_memory
            binding.tvPhoneBatteryState.text = items.phone_battery_info
            binding.tvPhonePrice.text = items.phone_price
            Log.d(
                "editphonefragment",
                "installData: ${items.phone_name} ${items.phone_imei1} ${items.phone_imei2}"
            )
        }

    }


}